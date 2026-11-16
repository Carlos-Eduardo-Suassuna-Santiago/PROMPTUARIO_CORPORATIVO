package br.edu.ifrn.promptuario.reporting.controller;

import br.edu.ifrn.promptuario.reporting.dto.ScheduleCreateDTO;
import br.edu.ifrn.promptuario.reporting.dto.ScheduleResponseDTO;
import br.edu.ifrn.promptuario.reporting.model.ReportSchedule;
import br.edu.ifrn.promptuario.reporting.repository.ReportScheduleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/schedules")
@Tag(name = "Report Schedules")
public class ScheduleController {

    private final ReportScheduleRepository reportScheduleRepository;

    public ScheduleController(ReportScheduleRepository reportScheduleRepository) {
        this.reportScheduleRepository = reportScheduleRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista agendamentos recorrentes de relatórios")
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> list() {
        return reportScheduleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria novo agendamento recorrente com expressão Cron")
    @Transactional
    public ResponseEntity<ScheduleResponseDTO> create(
            @Valid @RequestBody ScheduleCreateDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        ReportSchedule schedule = new ReportSchedule(
                request.name(),
                request.reportType(),
                request.outputFormat(),
                request.cronExpression(),
                userId,
                request.parameters(),
                request.recipients());
        ReportSchedule saved = reportScheduleRepository.save(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ScheduleResponseDTO toResponse(ReportSchedule schedule) {
        return new ScheduleResponseDTO(
                schedule.getId(),
                schedule.getName(),
                schedule.getReportType(),
                schedule.getOutputFormat(),
                schedule.getCronExpression(),
                schedule.isActive(),
                schedule.getLastRunAt(),
                schedule.getNextRunAt());
    }
}