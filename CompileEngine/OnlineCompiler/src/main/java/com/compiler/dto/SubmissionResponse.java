package com.compiler.dto;

public class SubmissionResponse {

    private Long submissionId;
    private String status;

    // ✅ REQUIRED no-args constructor
    public SubmissionResponse() {
    }

    // optional all-args constructor
    public SubmissionResponse(Long submissionId, String status) {
        this.submissionId = submissionId;
        this.status = status;
    }

    // getters and setters
    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
