package br.edu.ifrn.promptuario.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMQConfig {

    public static final String DOMAIN_EVENTS_EXCHANGE = "domain.events";
    public static final String IAM_EVENTS_EXCHANGE = "iam.events";
    public static final String IAM_EVENTS_QUEUE = "patient_clinical.iam_events";
    public static final String LEGACY_IAM_EXCHANGE = "promptuario.iam";

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

    @Bean
    public Queue iamEventsQueue() {
        return new Queue(IAM_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding userCreatedBinding(Queue iamEventsQueue,
                                     @Qualifier("iamEventsExchange") TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(iamEventsExchange).with("user.created");
    }

    @Bean
    public Binding userDeactivatedBinding(Queue iamEventsQueue,
                                          @Qualifier("iamEventsExchange") TopicExchange iamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(iamEventsExchange).with("user.deactivated");
    }

    @Bean
    public Binding legacyUserCreatedBinding(Queue iamEventsQueue,
                                            @Qualifier("legacyIamEventsExchange") TopicExchange legacyIamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(legacyIamEventsExchange).with("iam.user.created");
    }

    @Bean
    public Binding legacyUserDeactivatedBinding(Queue iamEventsQueue,
                                                @Qualifier("legacyIamEventsExchange") TopicExchange legacyIamEventsExchange) {
        return BindingBuilder.bind(iamEventsQueue).to(legacyIamEventsExchange).with("iam.user.deactivated");
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
