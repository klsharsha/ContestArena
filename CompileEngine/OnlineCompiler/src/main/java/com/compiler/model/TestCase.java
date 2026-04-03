package com.compiler.model;

import jakarta.persistence.*;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id")
    private String problemId;

    @Lob
    @Column(columnDefinition = "TEXT") // Explicitly defining as TEXT
    private String input;

    @Lob
    @Column(columnDefinition = "TEXT") // Explicitly defining as TEXT
    private String expectedOutput;

    private boolean isSample;

    // GETTERS & SETTERS (Keep your existing ones)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public boolean isSample() { return isSample; }
    public void setSample(boolean sample) { isSample = sample; }
}