package br.edu.ifrn.promptuario.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void compatibilityExchangesAndQueuesAreDurable() {
        assertThat(config.domainEventsExchange().isDurable()).isTrue();
        assertThat(config.legacyClinicalEventsExchange().isDurable()).isTrue();
        assertThat(config.clinicalEventsQueue().isDurable()).isTrue();
        assertThat(config.medicalRecordCreatedBinding(
                config.clinicalEventsQueue(), config.domainEventsExchange()).getRoutingKey())
                .isEqualTo("medical_record.created");
        assertThat(config.legacyMedicalRecordCreatedBinding(
                config.clinicalEventsQueue(), config.legacyClinicalEventsExchange()).getRoutingKey())
                .isEqualTo("clinical.medical_record.created");
    }

    @Test
    void retriesTransientAmqpFailuresUpToThreeAttempts() {
        RetryTemplate retryTemplate = config.eventPublishRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        String result = retryTemplate.execute(context -> {
            if (attempts.incrementAndGet() < 3) throw new AmqpTimeoutException("temporary timeout");
            return "published";
        });

        assertThat(result).isEqualTo("published");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void doesNotRetryProgrammingErrors() {
        RetryTemplate retryTemplate = config.eventPublishRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> retryTemplate.execute(context -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("invalid event");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(attempts).hasValue(1);
    }
}
