-- Durable event inbox: the inbox write and metric increment are committed in
-- the same transaction, so RabbitMQ redelivery cannot count an event twice.
CREATE TABLE processed_domain_events (
    event_id       VARCHAR(200) PRIMARY KEY,
    event_type     VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Normalize NULL entity IDs for a single unique key that supports atomic
-- INSERT .. ON CONFLICT upserts for both global and doctor-specific metrics.
-- Merge possible legacy duplicate aggregates before adding the uniqueness rule.
CREATE TEMPORARY TABLE daily_stats_merge AS
SELECT stat_date,
             stat_type,
             entity_id,
             MIN(id) AS keeper_id,
             SUM(value)::INT AS merged_value
FROM daily_stats
GROUP BY stat_date, stat_type, entity_id
HAVING COUNT(*) > 1;

UPDATE daily_stats stats
SET value = merged.merged_value,
        updated_at = CURRENT_TIMESTAMP
FROM daily_stats_merge merged
WHERE stats.id = merged.keeper_id;

DELETE FROM daily_stats stats
USING daily_stats_merge merged
WHERE stats.stat_date = merged.stat_date
    AND stats.stat_type = merged.stat_type
    AND stats.entity_id IS NOT DISTINCT FROM merged.entity_id
    AND stats.id <> merged.keeper_id;

DROP TABLE daily_stats_merge;

ALTER TABLE daily_stats
    ADD COLUMN entity_id_key VARCHAR(36)
    GENERATED ALWAYS AS (COALESCE(entity_id, '')) STORED;

CREATE UNIQUE INDEX ux_daily_stats_date_type_entity
    ON daily_stats (stat_date, stat_type, entity_id_key);

CREATE INDEX ix_processed_domain_events_processed_at
    ON processed_domain_events (processed_at);