package com.compiler.rabbitmq;

import com.compiler.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExecutionProducer {

    private final RabbitTemplate rabbitTemplate;

    public ExecutionProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(Long submissionId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXECUTION_QUEUE,
                submissionId
        );
    }
}
