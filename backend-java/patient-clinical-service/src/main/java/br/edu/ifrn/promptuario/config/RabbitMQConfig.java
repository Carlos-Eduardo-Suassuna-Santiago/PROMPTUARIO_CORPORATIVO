package br.edu.ifrn.promptuario.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.AmqpResourceNotAvailableException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.Executor;
import java.util.List;

@Configuration
@EnableAsync
public class RabbitMQConfig {

    public static final String DOMAIN_EVENTS_EXCHANGE = "domain.events";
    public static final String IAM_EVENTS_EXCHANGE = "iam.events";
    public static final String IAM_EVENTS_QUEUE = "patient_clinical.iam_events";
    public static final String LEGACY_IAM_EXCHANGE = "promptuario.iam";
    public static final String LEGACY_CLINICAL_EXCHANGE = "promptuario.clinical";
    public static final String CLINICAL_EVENTS_QUEUE = "patient_clinical.clinical_events";

    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange iamEventsExchange() {
        return new TopicExchange(IAM_EVENTS_EXCHANGE, true, false);
    }

    /** Compatibility exchange used by the existing Python IAM publisher. */
    @Bean
    public TopicExchange legacyIamEventsExchange() {
        return new TopicExchange(LEGACY_IAM_EXCHANGE, true, false);
    }

    /** Temporary compatibility exchange consumed by the Python AI service. */
    @Bean
    public TopicExchange legacyClinicalEventsExchange() {
        return new TopicExchange(LEGACY_CLINICAL_EXCHANGE, true, false);
    }

    @Bean
    public Queue iamEventsQueue() {
        return new Queue(IAM_EVENTS_QUEUE, true);
    }

    @Bean
    public Queue clinicalEventsQueue() {
        return new Queue(CLINICAL_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding medicalRecordCreatedBinding(@Qualifier("clinicalEventsQueue") Queue clinicalEventsQueue,
                                               @Qualifier("domainEventsExchange") TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(clinicalEventsQueue).to(domainEventsExchange).with("medical_record.created");
    }

    @Bean
    public Binding prescriptionGeneratedBinding(@Qualifier("clinicalEventsQueue") Queue clinicalEventsQueue,
                                                @Qualifier("domainEventsExchange") TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(clinicalEventsQueue).to(domainEventsExchange).with("prescription.generated");
    }

    @Bean
    public Binding legacyMedicalRecordCreatedBinding(@Qualifier("clinicalEventsQueue") Queue clinicalEventsQueue,
                                                     @Qualifier("legacyClinicalEventsExchange") TopicExchange legacyClinicalEventsExchange) {
        return BindingBuilder.bind(clinicalEventsQueue).to(legacyClinicalEventsExchange)
                .with("clinical.medical_record.created");
    }

    @Bean
    public Binding legacyPrescriptionGeneratedBinding(@Qualifier("clinicalEventsQueue") Queue clinicalEventsQueue,
                                                      @Qualifier("legacyClinicalEventsExchange") TopicExchange legacyClinicalEventsExchange) {
        return BindingBuilder.bind(clinicalEventsQueue).to(legacyClinicalEventsExchange)
                .with("clinical.prescription.generated");
    }

    @Bean
    public Binding userCreatedBinding(@Qualifier("iamEventsQueue") Queue iamEventsQueue,
                                     @Qualifier("iamEventsExchange") TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(iamEventsExchange).with("user.created");
    }

    @Bean
    public Binding userDeactivatedBinding(@Qualifier("iamEventsQueue") Queue iamEventsQueue,
                                          @Qualifier("iamEventsExchange") TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(iamEventsExchange).with("user.deactivated");
    }

    @Bean
    public Binding legacyUserCreatedBinding(@Qualifier("iamEventsQueue") Queue iamEventsQueue,
                                            @Qualifier("legacyIamEventsExchange") TopicExchange legacyIamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(legacyIamEventsExchange).with("iam.user.created");
    }

    @Bean
    public Binding legacyUserDeactivatedBinding(@Qualifier("iamEventsQueue") Queue iamEventsQueue,
                                                @Qualifier("legacyIamEventsExchange") TopicExchange legacyIamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(legacyIamEventsExchange).with("iam.user.deactivated");
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        // Reuse Spring Boot's Java-time configuration so Instant is emitted as ISO-8601.
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public RetryTemplate eventPublishRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(250, 2.0, 2000)
                .retryOn(List.of(
                        AmqpConnectException.class,
                        AmqpIOException.class,
                        AmqpResourceNotAvailableException.class,
                        AmqpTimeoutException.class))
                .build();
    }

    @Bean(name = "eventPublishingExecutor")
    public Executor eventPublishingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("event-publisher-");
        executor.initialize();
        return executor;
    }
}
