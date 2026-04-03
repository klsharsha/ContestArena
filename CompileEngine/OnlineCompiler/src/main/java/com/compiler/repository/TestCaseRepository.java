package com.compiler.repository;

import com.compiler.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProblemId(String problemId);

    @Transactional
    void deleteByProblemId(String problemId);
}