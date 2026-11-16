package br.edu.ifrn.promptuario.reporting.controller;

import br.edu.ifrn.promptuario.reporting.dto.DailyStatItemDTO;
import br.edu.ifrn.promptuario.reporting.dto.DashboardSummaryDTO;
import br.edu.ifrn.promptuario.reporting.dto.DoctorProductivityDTO;
import br.edu.ifrn.promptuario.reporting.service.DashboardStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Dashboard Stats")
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    public DashboardStatsController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/consultations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(summary = "Estatísticas diárias de consultas")
    public List<DailyStatItemDTO> consultations(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        return dashboardStatsService.getDailyConsultations(fromDate, toDate);
    }

    @GetMapping("/patients")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estatísticas diárias de novos pacientes")
    public List<DailyStatItemDTO> patients(
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        return dashboardStatsService.getNewPatientsStats(fromDate, toDate);
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Produtividade médica")
    public List<DoctorProductivityDTO> doctors(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(required = false) String doctorId) {
        return dashboardStatsService.getDoctorProductivityStats(fromDate, toDate, doctorId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumo analítico rápido para cards do dashboard")
    public DashboardSummaryDTO summary() {
        return dashboardStatsService.getSummaryMetrics();
    }
}