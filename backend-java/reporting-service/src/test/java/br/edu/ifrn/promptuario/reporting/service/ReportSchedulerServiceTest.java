package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.model.ReportSchedule;
import br.edu.ifrn.promptuario.reporting.model.enums.OutputFormat;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportType;
import br.edu.ifrn.promptuario.reporting.repository.ReportScheduleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSchedulerServiceTest {

    @Test
    void dispatchesDueFiveFieldCronSchedule() {
        ReportScheduleRepository schedules = mock(ReportScheduleRepository.class);
        ReportJobService jobs = mock(ReportJobService.class);
        ReportSchedule schedule = new ReportSchedule("Every minute", ReportType.CONSULTATIONS,
                OutputFormat.CSV, "* * * * *", "admin", null, null);
        schedule.setId("schedule-1");
        when(schedules.findByActiveTrue()).thenReturn(List.of(schedule));
        when(schedules.claimScheduleForMinute(eq("schedule-1"), any(), any(), any())).thenReturn(1);

        new ReportSchedulerService(schedules, jobs).dispatchDueSchedules();

        verify(jobs).createScheduledExportJob(schedule);
        verify(schedules, never()).updateNextRunAt(eq("schedule-1"), any(), any());
    }

    @Test
    void ignoresInvalidCronWithoutCreatingJob() {
        ReportScheduleRepository schedules = mock(ReportScheduleRepository.class);
        ReportJobService jobs = mock(ReportJobService.class);
        ReportSchedule schedule = new ReportSchedule("Bad cron", ReportType.CONSULTATIONS,
                OutputFormat.CSV, "invalid", "admin", null, null);
        schedule.setId("schedule-invalid");
        when(schedules.findByActiveTrue()).thenReturn(List.of(schedule));

        new ReportSchedulerService(schedules, jobs).dispatchDueSchedules();

        verify(jobs, never()).createScheduledExportJob(any());
    }
}
