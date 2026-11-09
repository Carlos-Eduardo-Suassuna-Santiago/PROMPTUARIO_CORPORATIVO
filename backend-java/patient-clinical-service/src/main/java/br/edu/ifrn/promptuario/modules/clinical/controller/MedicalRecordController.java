package br.edu.ifrn.promptuario.modules.clinical.controller;

import br.edu.ifrn.promptuario.modules.clinical.dto.*;
import br.edu.ifrn.promptuario.modules.clinical.model.*;
import br.edu.ifrn.promptuario.modules.clinical.repository.*;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/api/v1/records") @Tag(name="Medical records") @Transactional
public class MedicalRecordController {
    private final MedicalRecordRepository records; private final AppointmentRepository appointments; private final PrescriptionRepository prescriptions; private final ExamRequestRepository exams; private final MedicalCertificateRepository certificates;
    public MedicalRecordController(MedicalRecordRepository records,AppointmentRepository appointments,PrescriptionRepository prescriptions,ExamRequestRepository exams,MedicalCertificateRepository certificates){this.records=records;this.appointments=appointments;this.prescriptions=prescriptions;this.exams=exams;this.certificates=certificates;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')") @Transactional @Operation(summary="Create SOAP medical record")
    public MedicalRecordResponseDto create(@Valid @RequestBody MedicalRecordCreateDto dto,Authentication auth){MedicalRecord r=dto.toEntity(); r.setAppointment(appointments.findById(dto.appointmentId()).orElseThrow(()->new ResourceNotFoundException("Consulta não encontrada")));r.setDoctorId(auth.getName());return MedicalRecordResponseDto.fromEntity(records.save(r));}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public MedicalRecordResponseDto get(@PathVariable String id){return MedicalRecordResponseDto.fromEntity(find(id));}
    @PatchMapping("/{id}") @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')") @Transactional public MedicalRecordResponseDto update(@PathVariable String id,@RequestBody MedicalRecordUpdateDto dto){MedicalRecord r=find(id);if(dto.chiefComplaint()!=null)r.setChiefComplaint(dto.chiefComplaint());if(dto.anamnesis()!=null)r.setAnamnesis(dto.anamnesis());if(dto.physicalExam()!=null)r.setPhysicalExam(dto.physicalExam());if(dto.diagnosis()!=null)r.setDiagnosis(dto.diagnosis());if(dto.diagnosisCodes()!=null)r.setDiagnosisCodes(dto.diagnosisCodes());if(dto.treatmentPlan()!=null)r.setTreatmentPlan(dto.treatmentPlan());if(dto.observations()!=null)r.setObservations(dto.observations());if(dto.richNotes()!=null)r.setRichNotes(dto.richNotes());return MedicalRecordResponseDto.fromEntity(records.save(r));}
    @GetMapping("/patient/{patientId}") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public List<MedicalRecordResponseDto> history(@PathVariable String patientId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return records.findByPatientIdOrderByCreatedAtDesc(patientId,org.springframework.data.domain.PageRequest.of(Math.max(page,0),Math.max(size,1))).getContent().stream().map(MedicalRecordResponseDto::fromEntity).toList();}
    @PostMapping("/{recordId}/prescriptions") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')") @Transactional public PrescriptionResponseDto prescription(@PathVariable String recordId,@Valid @RequestBody PrescriptionCreateDto dto,Authentication auth){MedicalRecord r=find(recordId);return PrescriptionResponseDto.fromEntity(prescriptions.save(dto.toEntity(r,r.getPatientId(),auth.getName())));}
    @GetMapping("/{recordId}/prescriptions") @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ATTENDANT','PATIENT')") public List<PrescriptionResponseDto> prescriptions(@PathVariable String recordId){find(recordId);return prescriptions.findByRecord_IdOrderByCreatedAtDesc(recordId).stream().map(PrescriptionResponseDto::fromEntity).toList();}
    @PostMapping("/{recordId}/exams") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')") @Transactional public ExamRequestResponseDto exam(@PathVariable String recordId,@Valid @RequestBody ExamRequestCreateDto dto,Authentication auth){MedicalRecord r=find(recordId);ExamRequest e=dto.toEntity(r.getPatientId(),auth.getName());e.setRecord(r);return ExamRequestResponseDto.fromEntity(exams.save(e));}
    @PutMapping("/{recordId}/exams/{examId}/result") @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')") @Transactional public ExamRequestResponseDto result(@PathVariable String recordId,@PathVariable String examId,@Valid @RequestBody ExamResultUpdateDto dto){find(recordId);ExamRequest e=exams.findById(examId).filter(x->recordId.equals(x.getRecord().getId())).orElseThrow(()->new ResourceNotFoundException("Exame não encontrado"));e.setResult(dto.result());e.setResultDate(dto.resultDate()==null?Instant.now():dto.resultDate());return ExamRequestResponseDto.fromEntity(exams.save(e));}
    @PostMapping("/{recordId}/certificates") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')") @Transactional public MedicalCertificateResponseDto certificate(@PathVariable String recordId,@Valid @RequestBody MedicalCertificateCreateDto dto,Authentication auth){MedicalRecord r=find(recordId);MedicalCertificate c=dto.toEntity(r.getPatientId(),auth.getName());c.setRecord(r);return MedicalCertificateResponseDto.fromEntity(certificates.save(c));}
    private MedicalRecord find(String id){return records.findById(id).orElseThrow(()->new ResourceNotFoundException("Prontuário não encontrado"));}
}
