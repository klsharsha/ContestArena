package com.compiler.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    private String username;

    @Column(name = "contest_id")
    private String contestId;

    @Column(nullable = false)
    private String language;

    @Column(name = "problem_id")
    private String problemId;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false) // Use TEXT for Postgres
    private String code;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String input;

    @Lob
    @Column(columnDefinition = "TEXT") // Use TEXT for Postgres
    private String output;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Submission() {}

    public Submission(String language, String code, String input, String output, String status, 
                      LocalDateTime createdAt, String problemId, String userId, 
                      String username, String contestId) {
        this.language = language;
        this.code = code;
        this.input = input;
        this.output = output;
        this.status = status;
        this.createdAt = createdAt;
        this.problemId = problemId;
        this.userId = userId;
        this.username = username;
        this.contestId = contestId;
    }

    // GETTERS & SETTERS (Keep your existing ones)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getContestId() { return contestId; }
    public void setContestId(String contestId) { this.contestId = contestId; }
    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}