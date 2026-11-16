package br.edu.ifrn.promptuario.reporting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitMQConfig {

    public static final String EVENTS_QUEUE = "reporting.events.queue";
    public static final String DOMAIN_EVENTS_EXCHANGE = "domain.events";
    private static final List<String> ROUTING_KEYS = List.of(
            "patient.created",
            "appointment.created",
            "appointment.cancelled",
            "medical_record.created",
            "prescription.generated");

    @Bean
    public Queue reportingEventsQueue(
            @Value("${app.messaging.events-queue:reporting.events.queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public TopicExchange reportingDomainEventsExchange(
            @Value("${app.messaging.domain-exchange:domain.events}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Declarables reportingEventBindings(
            @Qualifier("reportingEventsQueue") Queue queue,
            @Qualifier("reportingDomainEventsExchange") TopicExchange exchange) {
        List<Binding> bindings = ROUTING_KEYS.stream()
                .map(key -> BindingBuilder.bind(queue).to(exchange).with(key))
                .toList();
        return new Declarables(bindings);
    }

    @Bean
    public Jackson2JsonMessageConverter reportingMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}