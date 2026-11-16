package br.edu.ifrn.promptuario.reporting.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportJobQueueConfig {

    @Bean
    public Queue reportingJobQueue(@Value("${app.messaging.job-queue:reporting.job.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }
}