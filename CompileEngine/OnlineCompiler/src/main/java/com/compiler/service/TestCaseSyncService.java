package com.compiler.service;

import com.compiler.dto.ContestProblemResponse;
import com.compiler.model.TestCase;
import com.compiler.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseSyncService {

    private final TestCaseRepository testCaseRepository;
    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${contest.service.base-url:http://localhost:8082}")
    private String contestServiceBaseUrl;

    @Transactional
    public List<TestCase> getOrSyncTestCases(String problemId) {
        List<TestCase> local = testCaseRepository.findByProblemId(problemId);
        if (!local.isEmpty()) {
            return local;
        }

        List<TestCase> fetched = fetchFromContestService(problemId);
        if (fetched.isEmpty()) {
            return Collections.emptyList();
        }

        testCaseRepository.deleteByProblemId(problemId);
        List<TestCase> saved = testCaseRepository.saveAll(fetched);
        log.info("Synced {} test cases for problemId={}", saved.size(), problemId);
        return saved;
    }

    private List<TestCase> fetchFromContestService(String problemId) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        String url = contestServiceBaseUrl + "/contests/problems/" + problemId;

        try {
            ResponseEntity<ContestProblemResponse> response = restTemplate.getForEntity(url, ContestProblemResponse.class);
            ContestProblemResponse body = response.getBody();
            if (body == null || body.getTestCases() == null || body.getTestCases().isEmpty()) {
                log.warn("No test cases received from contest-service for problemId={}", problemId);
                return Collections.emptyList();
            }

            List<TestCase> mapped = new ArrayList<>();
            for (ContestProblemResponse.TestCaseDto tc : body.getTestCases()) {
                TestCase entity = new TestCase();
                entity.setProblemId(problemId);
                entity.setInput(tc.getInput());
                entity.setExpectedOutput(tc.getExpectedOutput());
                entity.setSample(Boolean.TRUE.equals(tc.getIsSample()));
                mapped.add(entity);
            }
            return mapped;
        } catch (RestClientException ex) {
            log.error("Failed to fetch test cases from contest-service for problemId={}: {}", problemId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
