package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.model.ReportSchedule;
import br.edu.ifrn.promptuario.reporting.repository.ReportScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReportSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ReportSchedulerService.class);

    private final ReportScheduleRepository reportScheduleRepository;
    private final ReportJobService reportJobService;

    public ReportSchedulerService(ReportScheduleRepository reportScheduleRepository,
                                 ReportJobService reportJobService) {
        this.reportScheduleRepository = reportScheduleRepository;
        this.reportJobService = reportJobService;
    }

    @Scheduled(fixedRate = 60_000)
    public void dispatchDueSchedules() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
        Instant nowInstant = now.toInstant();
        Instant minuteStart = currentMinute.toInstant();

        List<ReportSchedule> schedules = reportScheduleRepository.findByActiveTrue();
        for (ReportSchedule schedule : schedules) {
            dispatchIfDue(schedule, now, currentMinute, nowInstant, minuteStart);
        }
    }

    private void dispatchIfDue(ReportSchedule schedule, ZonedDateTime now, ZonedDateTime currentMinute,
                               Instant nowInstant, Instant minuteStart) {
        CronExpression cron;
        try {
            cron = parseCronExpression(schedule.getCronExpression());
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid report schedule cron expression: scheduleId={}, expression={}, reason={}",
                schedule.getId(), schedule.getCronExpression(), exception.getMessage());
            return;
        }

        ZonedDateTime dueExecution = schedule.getNextRunAt() == null
                ? cron.next(currentMinute.minusNanos(1))
                : schedule.getNextRunAt().atZone(now.getZone());
        if (dueExecution == null) {
            reportScheduleRepository.updateNextRunAt(schedule.getId(), null, nowInstant);
            return;
        }
        if (dueExecution.isAfter(now)) {
            reportScheduleRepository.updateNextRunAt(schedule.getId(), dueExecution.toInstant(), nowInstant);
            return;
        }

        ZonedDateTime nextExecution = cron.next(now);
        Instant nextRunAt = nextExecution == null ? null : nextExecution.toInstant();

        int claimed = reportScheduleRepository.claimScheduleForMinute(
                schedule.getId(), nowInstant, minuteStart, nextRunAt);
        if (claimed == 0) {
            return;
        }

        try {
            reportJobService.createScheduledExportJob(schedule);
            log.info("Dispatched scheduled report: scheduleId={}, scheduleName={}, nextRunAt={}",
                    schedule.getId(), schedule.getName(), nextRunAt);
        } catch (RuntimeException exception) {
            log.error("Could not create job for report schedule: scheduleId={}, scheduleName={}",
                    schedule.getId(), schedule.getName(), exception);
        }
    }

    private CronExpression parseCronExpression(String expression) {
        String normalized = expression == null ? "" : expression.trim();
        if (!normalized.startsWith("@")) {
            String[] fields = normalized.split("\\s+");
            if (fields.length == 5) {
                normalized = "0 " + normalized;
            } else if (fields.length != 6) {
                throw new IllegalArgumentException("Cron must contain five or six fields");
            }
        }
        return CronExpression.parse(normalized);
    }
}