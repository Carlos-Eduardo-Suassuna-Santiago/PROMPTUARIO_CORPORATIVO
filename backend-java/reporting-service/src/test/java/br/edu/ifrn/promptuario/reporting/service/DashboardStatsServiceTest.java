package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.repository.DailyStatsRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardStatsServiceTest {

    @Test
    void returnsAggregatedSummaryForCurrentUtcDate() {
        DailyStatsRepository repository = mock(DailyStatsRepository.class);
        when(repository.aggregateMetricsByPeriod(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(
                aggregate("CONSULTATIONS", 8L),
                aggregate("NEW_PATIENTS", 3L),
                aggregate("CANCELLATIONS", 2L),
                aggregate("PRESCRIPTIONS", 6L),
                aggregate("DOCTOR_CONSULTATIONS", 5L)));

        var summary = new DashboardStatsService(repository).getSummaryMetrics();

        assertEquals(8, summary.totalConsultationsToday());
        assertEquals(3, summary.totalNewPatientsToday());
        assertEquals(2, summary.totalCancellationsToday());
        assertEquals(6, summary.totalPrescriptionsToday());
        assertEquals(5L, summary.metricsWindow().get("doctor_consultations"));
    }

    private DailyStatsRepository.DailyMetricAggregate aggregate(String type, long value) {
        return new DailyStatsRepository.DailyMetricAggregate() {
            @Override
            public String getStatDate() {
                return "2026-11-15";
            }

            @Override
            public String getStatType() {
                return type;
            }

            @Override
            public Long getTotalValue() {
                return value;
            }
        };
    }
}
