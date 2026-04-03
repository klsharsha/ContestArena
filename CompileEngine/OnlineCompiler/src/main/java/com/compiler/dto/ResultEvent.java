package com.compiler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResultEvent {
    private String submissionId;
    private String userId;
    private String username;
    private String contestId;
    private String problemId;
    private String verdict;      // For Dharma ("AC" or "WA")
    private String status;       // For Frontend ("ACCEPTED", "RUNTIME_ERROR")
    private String output;       // For Frontend (Stack traces, test case diffs)
    private String submittedAt;
    public ResultEvent(String submissionId, String userId, String username, String contestId, String problemId,
            String verdict, String status, String output, String submittedAt) {
        this.submissionId = submissionId;
        this.userId = userId;
        this.username = username;
        this.contestId = contestId;
        this.problemId = problemId;
        this.verdict = verdict;
        this.status = status;
        this.output = output;
        this.submittedAt = submittedAt;
    }

    
}