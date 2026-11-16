package br.edu.ifrn.promptuario.reporting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "webhook_configs")
public class WebhookConfig {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Size(max = 500)
    @Column(length = 500, nullable = false)
    private String url;

    @NotBlank
    @Size(max = 128)
    @Column(length = 128, nullable = false)
    private String secret;

    @Size(max = 255)
    @Column(length = 255)
    private String description = "";

    @Column(nullable = false)
    private boolean active = true;

    @Min(0)
    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Min(0)
    @Column(name = "retry_interval_seconds", nullable = false)
    private int retryIntervalSeconds = 60;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> events = new ArrayList<>();

    @Column(name = "last_delivery_at")
    private Instant lastDeliveryAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Min(0)
    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @NotBlank
    @Size(max = 36)
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected WebhookConfig() {
    }

    public WebhookConfig(String url, String secret, String createdBy, String description, List<String> events) {
        this.url = url;
        this.secret = secret;
        this.createdBy = createdBy;
        this.description = description == null ? "" : description;
        this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}