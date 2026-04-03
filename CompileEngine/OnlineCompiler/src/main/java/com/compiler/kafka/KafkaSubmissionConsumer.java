package com.compiler.kafka;

import com.compiler.service.SubmissionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaSubmissionConsumer {

    private final SubmissionService submissionService;

    public KafkaSubmissionConsumer(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @KafkaListener(topics = "${kafka.topic.submission}", groupId = "execution-group")
    public void consume(Object message) {
        System.out.println("Received from Kafka: " + message);

        // For now just log
        // Later: call evaluation service
    }
}