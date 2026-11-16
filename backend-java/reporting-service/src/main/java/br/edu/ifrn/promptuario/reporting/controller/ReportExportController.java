package br.edu.ifrn.promptuario.reporting.controller;

import br.edu.ifrn.promptuario.reporting.dto.ReportExportRequestDTO;
import br.edu.ifrn.promptuario.reporting.dto.ReportJobResponseDTO;
import br.edu.ifrn.promptuario.reporting.service.ReportJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/reports/export")
@Tag(name = "Report Export")
public class ReportExportController {

    private final ReportJobService reportJobService;

    public ReportExportController(ReportJobService reportJobService) {
        this.reportJobService = reportJobService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(summary = "Solicita exportação assíncrona de relatório")
    public ResponseEntity<ReportJobResponseDTO> requestExport(
            @Valid @RequestBody ReportExportRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : "anonymous";
        ReportJobResponseDTO response = reportJobService.createExportJob(request, userId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulta status do job de exportação")
    public ResponseEntity<ReportJobResponseDTO> getJob(@PathVariable String jobId) {
        return ResponseEntity.ok(reportJobService.getJob(jobId));
    }

    @GetMapping("/{jobId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtém URL pré-assinada para download do relatório")
    public ResponseEntity<Void> download(@PathVariable String jobId) {
        String url = reportJobService.getDownloadUrl(jobId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}