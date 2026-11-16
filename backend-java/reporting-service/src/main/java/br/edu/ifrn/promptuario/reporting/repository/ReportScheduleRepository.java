package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, String> {

    List<ReportSchedule> findByActiveTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ReportSchedule schedule "
            + "SET schedule.lastRunAt = :runAt, schedule.nextRunAt = :nextRunAt, schedule.updatedAt = :runAt "
            + "WHERE schedule.id = :scheduleId AND schedule.active = true "
            + "AND (schedule.lastRunAt IS NULL OR schedule.lastRunAt < :minuteStart)")
    int claimScheduleForMinute(@Param("scheduleId") String scheduleId,
                               @Param("runAt") Instant runAt,
                               @Param("minuteStart") Instant minuteStart,
                               @Param("nextRunAt") Instant nextRunAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ReportSchedule schedule "
            + "SET schedule.nextRunAt = :nextRunAt, schedule.updatedAt = :updatedAt "
            + "WHERE schedule.id = :scheduleId AND schedule.active = true "
            + "AND (schedule.nextRunAt IS NULL OR schedule.nextRunAt <> :nextRunAt)")
    int updateNextRunAt(@Param("scheduleId") String scheduleId,
                        @Param("nextRunAt") Instant nextRunAt,
                        @Param("updatedAt") Instant updatedAt);
}