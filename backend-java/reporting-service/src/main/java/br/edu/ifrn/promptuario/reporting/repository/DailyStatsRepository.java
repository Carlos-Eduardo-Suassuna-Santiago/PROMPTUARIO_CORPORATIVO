package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DailyStatsRepository extends JpaRepository<DailyStats, String> {

    Optional<DailyStats> findByStatDateAndStatTypeAndEntityId(
            String statDate, String statType, String entityId);

    Optional<DailyStats> findByStatDateAndStatTypeAndEntityIdIsNull(String statDate, String statType);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO daily_stats (id, stat_date, stat_type, entity_id, value, metadata, updated_at)
            VALUES (:id, :statDate, :statType, :entityId, 1, CAST('{}' AS jsonb), CURRENT_TIMESTAMP)
            ON CONFLICT (stat_date, stat_type, entity_id_key)
            DO UPDATE SET value = daily_stats.value + 1, updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int incrementMetricAtomically(@Param("id") String id,
                                  @Param("statDate") String statDate,
                                  @Param("statType") String statType,
                                  @Param("entityId") String entityId);

    List<DailyStats> findByStatTypeAndStatDateBetweenOrderByStatDateDesc(
            String statType, String fromDate, String toDate);

    List<DailyStats> findTop100001ByStatTypeAndStatDateBetweenOrderByStatDateDesc(
            String statType, String fromDate, String toDate);

    List<DailyStats> findByStatTypeAndStatDateBetweenAndEntityIdOrderByStatDateDesc(
            String statType, String fromDate, String toDate, String entityId);

    List<DailyStats> findByStatDateBetweenOrderByStatDateDescStatTypeAscEntityIdAsc(
            String fromDate, String toDate);

    List<DailyStats> findTop100001ByStatDateBetweenOrderByStatDateDescStatTypeAscEntityIdAsc(
            String fromDate, String toDate);

    @Query("""
            SELECT d.statDate AS statDate,
                   d.statType AS statType,
                   SUM(d.value) AS totalValue
            FROM DailyStats d
            WHERE d.statDate BETWEEN :from AND :to
            GROUP BY d.statDate, d.statType
            ORDER BY d.statDate DESC
            """)
    List<DailyMetricAggregate> aggregateMetricsByPeriod(
            @Param("from") String from,
            @Param("to") String to);

    @Query("""
            SELECT d.entityId AS doctorId,
                   SUM(d.value) AS totalConsultations
            FROM DailyStats d
            WHERE d.statType = :statType
              AND d.statDate BETWEEN :from AND :to
              AND d.entityId IS NOT NULL
              AND (:doctorId IS NULL OR d.entityId = :doctorId)
            GROUP BY d.entityId
            ORDER BY SUM(d.value) DESC
            """)
    List<DoctorProductivityAggregate> aggregateDoctorProductivity(
            @Param("statType") String statType,
            @Param("from") String from,
            @Param("to") String to,
            @Param("doctorId") String doctorId);

    interface DailyMetricAggregate {
        String getStatDate();

        String getStatType();

        Long getTotalValue();
    }

    interface DoctorProductivityAggregate {
        String getDoctorId();

        Long getTotalConsultations();
    }
}