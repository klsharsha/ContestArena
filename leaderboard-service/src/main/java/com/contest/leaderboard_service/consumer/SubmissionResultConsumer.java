package com.contest.leaderboard_service.consumer;

import com.contest.leaderboard_service.dto.SubmissionResultEvent;
import com.contest.leaderboard_service.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionResultConsumer {

    private final LeaderboardService leaderboardService;

    @KafkaListener(
            topics = "submission-result",
            groupId = "leaderboard-service-v5"
    )
    public void consume(SubmissionResultEvent event) {
        log.info("📥 Kafka Message Received | ID: {} | Status: {} | Verdict: {}", 
                event.getSubmissionId(), event.getStatus(), event.getVerdict());

        try {
            // Always process event so WA attempts are tracked and included in penalty on AC.
            leaderboardService.processResult(event);
            log.info("🏆 Leaderboard Logic Triggered for user: {}", event.getUsername());
        } catch (Exception e) {
            log.error("❌ Error processing Kafka message: {}", e.getMessage(), e);
        }
    }
}