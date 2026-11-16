package br.edu.ifrn.promptuario.reporting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "daily_stats")
public class DailyStats {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    @Size(max = 10)
    @Column(name = "stat_date", length = 10, nullable = false)
    private String statDate;

    @NotBlank
    @Size(max = 50)
    @Column(name = "stat_type", length = 50, nullable = false)
    private String statType;

    @Size(max = 36)
    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Min(0)
    @Column(nullable = false)
    private int value;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyStats() {
    }

    public DailyStats(String statDate, String statType, String entityId, int value,
                      Map<String, Object> metadata) {
        this.statDate = statDate;
        this.statType = statType;
        this.entityId = entityId;
        this.value = value;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}