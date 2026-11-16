package br.edu.ifrn.promptuario.reporting.service;

import br.edu.ifrn.promptuario.reporting.dto.DailyStatItemDTO;
import br.edu.ifrn.promptuario.reporting.dto.DashboardSummaryDTO;
import br.edu.ifrn.promptuario.reporting.dto.DoctorProductivityDTO;
import br.edu.ifrn.promptuario.reporting.repository.DailyStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardStatsService {

    private static final String CONSULTATIONS = "CONSULTATIONS";
    private static final String NEW_PATIENTS = "NEW_PATIENTS";
    private static final String CANCELLATIONS = "CANCELLATIONS";
    private static final String PRESCRIPTIONS = "PRESCRIPTIONS";
    private static final String DOCTOR_CONSULTATIONS = "DOCTOR_CONSULTATIONS";

    private final DailyStatsRepository dailyStatsRepository;

    public DashboardStatsService(DailyStatsRepository dailyStatsRepository) {
        this.dailyStatsRepository = dailyStatsRepository;
    }

    public List<DailyStatItemDTO> getDailyConsultations(String fromDate, String toDate) {
        return dailyItems(CONSULTATIONS, fromDate, toDate);
    }

    public List<DailyStatItemDTO> getNewPatientsStats(String fromDate, String toDate) {
        return dailyItems(NEW_PATIENTS, fromDate, toDate);
    }

    public List<DoctorProductivityDTO> getDoctorProductivityStats(
            String fromDate, String toDate, String doctorId) {
        DateRange range = validateRange(fromDate, toDate);
        long days = ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        return dailyStatsRepository.aggregateDoctorProductivity(
                        DOCTOR_CONSULTATIONS, range.from().toString(), range.to().toString(), doctorId)
                .stream()
                .map(aggregate -> new DoctorProductivityDTO(
                        aggregate.getDoctorId(),
                        null,
                        valueOrZero(aggregate.getTotalConsultations()),
                        (double) valueOrZero(aggregate.getTotalConsultations()) / days))
                .toList();
    }

    public DashboardSummaryDTO getSummaryMetrics() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<String, Long> totals = aggregateTotals(today.toString(), today.toString());
        Map<String, Object> metricsWindow = new LinkedHashMap<>();
        metricsWindow.put("from_date", today.toString());
        metricsWindow.put("to_date", today.toString());
        metricsWindow.put("doctor_consultations", totals.getOrDefault(DOCTOR_CONSULTATIONS, 0L));
        return new DashboardSummaryDTO(
                totals.getOrDefault(CONSULTATIONS, 0L),
                totals.getOrDefault(NEW_PATIENTS, 0L),
                totals.getOrDefault(CANCELLATIONS, 0L),
                totals.getOrDefault(PRESCRIPTIONS, 0L),
                metricsWindow);
    }

    private List<DailyStatItemDTO> dailyItems(String statType, String fromDate, String toDate) {
        DateRange range = validateRange(fromDate, toDate);
        List<DailyStatsRepository.DailyMetricAggregate> grouped = dailyStatsRepository.aggregateMetricsByPeriod(
                range.from().toString(), range.to().toString());
        return grouped.stream()
                .filter(row -> statType.equals(row.getStatType()))
                .map(row -> new DailyStatItemDTO(row.getStatDate(), valueOrZero(row.getTotalValue()), Map.of()))
                .toList();
    }

    private Map<String, Long> aggregateTotals(String fromDate, String toDate) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (DailyStatsRepository.DailyMetricAggregate aggregate
                : dailyStatsRepository.aggregateMetricsByPeriod(fromDate, toDate)) {
            totals.merge(aggregate.getStatType(), valueOrZero(aggregate.getTotalValue()), Long::sum);
        }
        return totals;
    }

    private DateRange validateRange(String fromDate, String toDate) {
        try {
            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("fromDate must be on or before toDate");
            }
            return new DateRange(from, to);
        } catch (NullPointerException | java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Date range must use YYYY-MM-DD format", exception);
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}