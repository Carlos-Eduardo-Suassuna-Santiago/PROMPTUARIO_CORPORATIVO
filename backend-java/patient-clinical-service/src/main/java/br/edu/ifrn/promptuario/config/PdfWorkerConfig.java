package br.edu.ifrn.promptuario.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class PdfWorkerConfig {

    public static final String PDF_QUEUE = "prescription.pdf.generation";
    public static final String PDF_DLQ = "prescription.pdf.generation.dlq";
    public static final String PDF_ROUTING_KEY = "prescription.pdf.generate";

    @Bean
    public Queue prescriptionPdfQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "");
        arguments.put("x-dead-letter-routing-key", PDF_DLQ);
        return new Queue(PDF_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Queue prescriptionPdfDlq() {
        return new Queue(PDF_DLQ, true);
    }

    @Bean
    public Binding prescriptionPdfBinding(@Qualifier("prescriptionPdfQueue") Queue prescriptionPdfQueue,
                                          @Qualifier("domainEventsExchange") TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(prescriptionPdfQueue)
                .to(domainEventsExchange)
                .with(PDF_ROUTING_KEY);
    }

    @Bean(name = "pdfTaskExecutor")
    public Executor pdfTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pdf-worker-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "pdfRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory pdfRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(6);
        factory.setTaskExecutor(pdfTaskExecutor());
        factory.setDefaultRequeueRejected(false);
        factory.setErrorHandler(new ConditionalRejectingErrorHandler());
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .build());
        return factory;
    }
}
