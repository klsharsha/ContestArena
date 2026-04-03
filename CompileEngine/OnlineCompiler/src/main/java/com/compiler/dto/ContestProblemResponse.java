package com.compiler.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContestProblemResponse {
    private String id;
    private List<TestCaseDto> testCases;

    @Data
    public static class TestCaseDto {
        private String input;
        private String expectedOutput;
        private Boolean isSample;
    }
}
