package com.contest.leaderboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultEvent {
    private String submissionId;
    private String userId;
    private String username;
    private String contestId;
    private String problemId;
    private String verdict;   // e.g., "AC", "WA"
    private String status;    // e.g., "ACCEPTED", "REJECTED"
    private String submittedAt;
    private int score;
}