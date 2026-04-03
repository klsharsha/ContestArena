package com.compiler.kafka;

import com.compiler.dto.ResultEvent;
import com.compiler.model.Submission;
import com.compiler.repository.SubmissionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaResultConsumer {

    private final SubmissionRepository submissionRepository;

    public KafkaResultConsumer(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

   @KafkaListener(topics = "${kafka.topic.result:submission-result}", groupId = "${spring.kafka.consumer.group-id}")
public void handleResult(ResultEvent event) {
    // Parse the String ID back to a Long
    Long subId = Long.parseLong(event.getSubmissionId());
    
    submissionRepository.findById(subId).ifPresent(submission -> {
        submission.setStatus(event.getStatus());
        submission.setOutput(event.getOutput());
        submissionRepository.save(submission);
    });
}
}