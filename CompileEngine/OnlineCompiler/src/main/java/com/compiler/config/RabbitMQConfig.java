package com.compiler.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXECUTION_QUEUE = "code.execution.queue";

    @Bean
    public Queue executionQueue() {
        return new Queue(EXECUTION_QUEUE, true);
    }
}
