package br.edu.ifrn.promptuario.modules.clinical.controller;

import br.edu.ifrn.promptuario.modules.clinical.dto.*;
import br.edu.ifrn.promptuario.modules.clinical.model.*;
import br.edu.ifrn.promptuario.modules.clinical.repository.AppointmentRepository;
import br.edu.ifrn.promptuario.modules.patient.service.PatientService;
import br.edu.ifrn.promptuario.shared.exception.BusinessException;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/api/v1/appointments") @Tag(name="Appointments")
@Transactional
public class AppointmentController {
    private final AppointmentRepository repository; private final PatientService patients;
    public AppointmentController(AppointmentRepository repository, PatientService patients){this.repository=repository;this.patients=patients;}
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") @Operation(summary="List appointments")
    public AppointmentListResponseDto list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String patientId,@RequestParam(required=false) String doctorId,@RequestParam(required=false) AppointmentStatus status,@RequestParam(required=false) Instant fromDate,@RequestParam(required=false) Instant toDate,@RequestParam(required=false) String patientName){
        var result=repository.searchAppointments(patientId,doctorId,status,fromDate,toDate,patientName,PageRequest.of(Math.max(0,page),Math.max(1,size)));
        return new AppointmentListResponseDto(result.getContent().stream().map(AppointmentResponseDto::fromEntity).toList(),result.getTotalElements(),result.getNumber(),result.getSize());
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT')") public AppointmentResponseDto create(@Valid @RequestBody AppointmentCreateDto dto,Authentication auth){
        if(repository.existsByDoctorIdAndScheduledAtAndStatusNot(dto.doctorId(),dto.scheduledAt(),AppointmentStatus.CANCELLED)) throw new BusinessException("Horário indisponível");
        Appointment a=dto.toEntity(); a.setPatient(patients.getPatientById(dto.patientId())); a.setCreatedBy(auth.getName());
        return AppointmentResponseDto.fromEntity(repository.save(a));
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public AppointmentResponseDto get(@PathVariable String id){return AppointmentResponseDto.fromEntity(find(id));}
    @PatchMapping("/{id}/cancel") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public AppointmentResponseDto cancel(@PathVariable String id,@Valid @RequestBody AppointmentCancelDto dto,Authentication auth){Appointment a=find(id); if(a.getScheduledAt().isBefore(Instant.now())) throw new BusinessException("Consulta já iniciada não pode ser cancelada"); a.setStatus(AppointmentStatus.CANCELLED);a.setCancellationReason(dto.reason());a.setCancelledBy(auth.getName());a.setCancelledAt(Instant.now());return AppointmentResponseDto.fromEntity(repository.save(a));}
    @PatchMapping("/{id}/complete") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')") public AppointmentResponseDto complete(@PathVariable String id){Appointment a=find(id); a.setStatus(AppointmentStatus.COMPLETED); return AppointmentResponseDto.fromEntity(repository.save(a));}
    private Appointment find(String id){return repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Consulta não encontrada"));}
}
