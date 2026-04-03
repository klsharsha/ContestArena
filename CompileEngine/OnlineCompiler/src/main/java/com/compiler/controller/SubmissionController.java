package com.compiler.controller;

import com.compiler.dto.SubmissionRequest;
import com.compiler.dto.SubmissionResponse;
import com.compiler.model.Submission;
import com.compiler.repository.SubmissionRepository;
import com.compiler.rabbitmq.ExecutionProducer;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for managing code submissions.
 * Handles the initial API request, persists data to MySQL, 
 * and triggers the asynchronous execution flow via RabbitMQ.
 */
@RestController
@RequestMapping("/execute")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}) // Allow local React and Next frontends
public class SubmissionController {

    private final SubmissionRepository repository;
    private final ExecutionProducer producer;

    public SubmissionController(SubmissionRepository repository,
                                ExecutionProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    /**
     * Receives a code submission request.
     * Maps DTO fields to the Entity, saves to DB, and sends ID to Task Queue.
     */
    @PostMapping("/submit")
    public SubmissionResponse submit(@RequestBody SubmissionRequest request) {
        // Initialize a new Submission Entity
        Submission sub = new Submission();
        
        // 1. Map Core Execution Fields
        sub.setLanguage(request.getLanguage());
        sub.setCode(request.getCode());
        sub.setProblemId(request.getProblemId());
        sub.setInput(request.getInput());

        // 2. Map Leaderboard & Auth Integration Fields
        // These ensure the Kafka broadcast has user context
        sub.setUserId(request.getUserId());
        sub.setUsername(request.getUsername());
        sub.setContestId(request.getContestId());

        // 3. Set System Metadata
        sub.setStatus("PENDING");
        sub.setCreatedAt(LocalDateTime.now());

        // 4. Persist to MySQL Database
        Submission saved = repository.save(sub);

        // 5. Trigger Asynchronous Worker (RabbitMQ)
        // We only send the ID to keep the message payload lightweight
        producer.send(saved.getId());

        // 6. Immediate Response to Client (Polling starts now)
        return new SubmissionResponse(saved.getId(), "PENDING");
    }

    /**
     * Polling Endpoint: Returns the current status and output of a submission.
     */
    @GetMapping("/submissions/{id}")
    public Submission getSubmission(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found with ID: " + id));
    }
}