package com.contest.leaderboard_service.service;

import com.contest.leaderboard_service.dto.ContestStartedEvent;
import com.contest.leaderboard_service.dto.LeaderboardUpdateDto;
import com.contest.leaderboard_service.dto.SubmissionResultEvent;
import com.contest.leaderboard_service.entity.*;
import com.contest.leaderboard_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardEntryRepository entryRepo;
    private final ProblemAttemptRepository attemptRepo;
    private final ContestMetaRepository contestMetaRepo;
    private final ContestProblemRepository contestProblemRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final LeaderboardRedisService redisService;

    @Transactional
    public void handleContestStarted(ContestStartedEvent event) {
        ContestMeta meta = new ContestMeta();
        meta.setContestId(event.getContestId());
        meta.setStartTime(event.getStartTime().toInstant(ZoneOffset.UTC));
        meta.setEndTime(event.getEndTime().toInstant(ZoneOffset.UTC));
        meta.setTitle(event.getTitle());
        contestMetaRepo.save(meta);

        for (ContestStartedEvent.ProblemInfo p : event.getProblems()) {
            ContestProblem cp = new ContestProblem();
            cp.setContestId(event.getContestId());
            cp.setProblemId(p.getProblemId());
            cp.setProblemOrder(p.getOrder());
            cp.setProblemLabel(p.getLabel());
            cp.setTitle(p.getTitle());
            cp.setScore(p.getScore());
            contestProblemRepo.save(cp);
        }
        log.info("✅ Contest metadata initialized: {}", event.getContestId());
    }

    @Transactional
    public void processResult(SubmissionResultEvent event) {
        log.info("Processing result: verdict={} user={} problem={}", 
                 event.getVerdict(), event.getUsername(), event.getProblemId());

        // Check for both short code (AC) and full status (ACCEPTED)
        if ("ACCEPTED".equalsIgnoreCase(event.getStatus()) || 
            "ACCEPTED".equalsIgnoreCase(event.getVerdict()) || 
            "AC".equalsIgnoreCase(event.getVerdict())) {
            handleAccepted(event);
        } else {
            handleWrongAttempt(event);
        }
    }

    private void handleAccepted(SubmissionResultEvent event) {
        ProblemAttempt attempt = getOrCreateAttempt(event);
        if (attempt.isSolved()) {
            log.info("Problem {} already solved by user {}, skipping.", event.getProblemId(), event.getUserId());
            return;
        }

        // 1. SAFE TIMESTAMP PARSING
        LocalDateTime submittedAt;
        try {
            String ts = event.getSubmittedAt().replace(" ", "T");
            submittedAt = LocalDateTime.parse(ts);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp, defaulting to now: {}", e.getMessage());
            submittedAt = LocalDateTime.now();
        }
        Instant solvedAtInstant = submittedAt.toInstant(ZoneOffset.UTC);

        // 2. NULL-SAFE CONTEST CHECK (Now includes mandatory endTime)
        ContestMeta contest = contestMetaRepo.findById(event.getContestId())
                .orElseGet(() -> {
                    log.warn("⚠️ ContestMeta missing for {}. Creating temporary record.", event.getContestId());
                    ContestMeta temp = new ContestMeta();
                    temp.setContestId(event.getContestId());
                    temp.setStartTime(solvedAtInstant.minus(1, ChronoUnit.HOURS)); 
                    temp.setEndTime(solvedAtInstant.plus(3, ChronoUnit.HOURS)); // Fixed: Now non-null
                    temp.setTitle("Auto-Generated Contest");
                    return contestMetaRepo.save(temp);
                });

        // 3. CALCULATION
        long minutesElapsed = ChronoUnit.MINUTES.between(contest.getStartTime(), solvedAtInstant);
        int penalty = (int) Math.max(0, minutesElapsed) + (attempt.getWrongAttempts() * 10);
        
        int baseScore = contestProblemRepo.findByContestIdAndProblemId(event.getContestId(), event.getProblemId())
                .map(ContestProblem::getScore).orElse(100);

        int earnedScore = computeScore(baseScore, attempt.getWrongAttempts());
        
        // Update Attempt Entity
        attempt.setSolved(true);
        attempt.setSolvedAt(solvedAtInstant); 
        attempt.setPenaltyForProblem(penalty);
        attempt.setScoreEarned(earnedScore);
        attemptRepo.save(attempt);

        // Update Leaderboard Entry Entity
        LeaderboardEntry entry = getOrCreateEntry(event);
        entry.setSolvedCount(entry.getSolvedCount() + 1);
        entry.setTotalPenalty(entry.getTotalPenalty() + penalty);
        entry.setTotalScore(entry.getTotalScore() + earnedScore);
        entry.setLastAcAt(solvedAtInstant);
        entry.setUpdatedAt(Instant.now());
        entryRepo.save(entry);

        // 4. SYNC WITH REDIS & BROADCAST
        redisService.updateRank(event.getContestId(), event.getUserId(), entry.getTotalScore(), entry.getTotalPenalty());
        int newRank = redisService.getUserRank(event.getContestId(), event.getUserId());

        broadcast(event, entry, newRank, penalty, earnedScore);
        log.info("🏆 SUCCESS: Leaderboard updated for user: {}", event.getUsername());
    }

    private int computeScore(int baseScore, int wrongAttempts) {
        int floor = (int)(baseScore * 0.3);
        int score = baseScore - (50 * wrongAttempts);
        return Math.max(score, floor);
    }

    private void handleWrongAttempt(SubmissionResultEvent event) {
        ProblemAttempt attempt = getOrCreateAttempt(event);
        if (!attempt.isSolved()) {
            attempt.setWrongAttempts(attempt.getWrongAttempts() + 1);
            attemptRepo.save(attempt);
        }
        LeaderboardEntry entry = getOrCreateEntry(event);
        entry.setUpdatedAt(Instant.now());
        entryRepo.save(entry);

        redisService.updateRank(
                event.getContestId(),
                event.getUserId(),
                entry.getTotalScore(),
                entry.getTotalPenalty()
        );
        int currentRank = redisService.getUserRank(event.getContestId(), event.getUserId());
        broadcast(event, entry, currentRank, 0, 0);
    }

    private void broadcast(SubmissionResultEvent event, LeaderboardEntry entry, int newRank, int penalty, int score) {
        LeaderboardUpdateDto dto = new LeaderboardUpdateDto();
        dto.setContestId(event.getContestId());
        dto.setUserId(event.getUserId());
        dto.setUsername(event.getUsername());
        dto.setProblemId(event.getProblemId());
        dto.setVerdict(event.getVerdict());
        dto.setSolvedCount(entry.getSolvedCount());
        dto.setTotalPenalty(entry.getTotalPenalty());
        dto.setTotalScore(entry.getTotalScore());
        dto.setNewRank(newRank);
        dto.setPenaltyAdded(penalty);
        dto.setScoreEarned(score);

        messagingTemplate.convertAndSend("/topic/leaderboard/" + event.getContestId(), dto);
    }

    private ProblemAttempt getOrCreateAttempt(SubmissionResultEvent e) {
        return attemptRepo.findByContestIdAndUserIdAndProblemId(e.getContestId(), e.getUserId(), e.getProblemId())
                .orElseGet(() -> {
                    ProblemAttempt a = new ProblemAttempt();
                    a.setContestId(e.getContestId());
                    a.setUserId(e.getUserId());
                    a.setProblemId(e.getProblemId());
                    return a;
                });
    }

    private LeaderboardEntry getOrCreateEntry(SubmissionResultEvent e) {
        return entryRepo.findByContestIdAndUserId(e.getContestId(), e.getUserId())
                .orElseGet(() -> {
                    LeaderboardEntry entry = new LeaderboardEntry();
                    entry.setContestId(e.getContestId());
                    entry.setUserId(e.getUserId());
                    entry.setUsername(e.getUsername());
                    return entry;
                });
    }
}