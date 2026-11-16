package br.edu.ifrn.promptuario.reporting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "webhook_delivery_logs")
public class WebhookDeliveryLog {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webhook_config_id", nullable = false)
    private WebhookConfig webhookConfig;

    @NotBlank
    @Size(max = 36)
    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @NotBlank
    @Size(max = 500)
    @Column(length = 500, nullable = false)
    private String url;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(nullable = false)
    private boolean success;

    @Min(1)
    @Column(nullable = false)
    private int attempt = 1;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "delivered_at", nullable = false, updatable = false)
    private Instant deliveredAt;

    protected WebhookDeliveryLog() {
    }

    public WebhookDeliveryLog(WebhookConfig webhookConfig, String jobId, String eventType, String url) {
        this.webhookConfig = webhookConfig;
        this.jobId = jobId;
        this.eventType = eventType;
        this.url = url;
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}