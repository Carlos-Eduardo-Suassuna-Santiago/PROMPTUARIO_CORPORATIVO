package br.edu.ifrn.promptuario.modules.patient.service;

import br.edu.ifrn.promptuario.modules.patient.dto.PatientCreateDto;
import br.edu.ifrn.promptuario.modules.patient.dto.PatientListResponseDto;
import br.edu.ifrn.promptuario.modules.patient.dto.PatientResponseDto;
import br.edu.ifrn.promptuario.modules.patient.dto.PatientUpdateDto;
import br.edu.ifrn.promptuario.modules.patient.event.PatientCreatedEvent;
import br.edu.ifrn.promptuario.modules.patient.event.PatientUpdatedEvent;
import br.edu.ifrn.promptuario.modules.patient.model.Patient;
import br.edu.ifrn.promptuario.modules.patient.repository.PatientRepository;
import br.edu.ifrn.promptuario.shared.audit.AuditService;
import br.edu.ifrn.promptuario.shared.exception.BusinessException;
import br.edu.ifrn.promptuario.shared.exception.ConflictException;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PatientService {
    private final PatientRepository repository;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public PatientService(PatientRepository repository, AuditService auditService, ApplicationEventPublisher events) {
        this.repository = repository;
        this.auditService = auditService;
        this.events = events;
    }

    @Transactional(rollbackFor = Exception.class)
    public PatientResponseDto createPatient(PatientCreateDto dto, String authenticatedUserId) {
        String userId = dto.userId() == null || dto.userId().isBlank() ? authenticatedUserId : dto.userId();
        if (userId == null || userId.isBlank()) throw new BusinessException("Usuário autenticado é obrigatório");
        if (repository.findByUserId(userId).isPresent()) throw new ConflictException("Paciente já cadastrado para este usuário");
        if (dto.cpf() != null && !dto.cpf().isBlank() && repository.findByCpf(dto.cpf()).isPresent()) {
            throw new ConflictException("CPF já cadastrado");
        }
        Patient patient = dto.toEntity();
        patient.setUserId(userId);
        Patient saved = repository.save(patient);
        auditService.record("patients", "INSERT", saved.getId(), authenticatedUserId, null,
                Map.of("userId", userId, "fullName", saved.getFullName()));
        events.publishEvent(new PatientCreatedEvent(saved.getId(), userId));
        return PatientResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Patient getPatientById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
    }

    @Transactional(readOnly = true)
    public Patient getPatientByUserId(String userId) {
        return repository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
    }

    @Transactional(readOnly = true)
    public PatientListResponseDto listPatients(int page, int size, String search) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        var result = search == null || search.isBlank()
                ? repository.findAll(pageable)
                : repository.findByFullNameContainingIgnoreCaseOrCpfContaining(search, search, pageable);
        return new PatientListResponseDto(result.getContent().stream().map(PatientResponseDto::fromEntity).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public PatientResponseDto updatePatient(String id, PatientUpdateDto dto, String userId) {
        Patient patient = getPatientById(id);
        if (dto.cpf() != null && !dto.cpf().equals(patient.getCpf()) && repository.findByCpf(dto.cpf()).isPresent()) {
            throw new ConflictException("CPF já cadastrado");
        }
        if (dto.fullName() != null) patient.setFullName(dto.fullName());
        if (dto.cpf() != null) patient.setCpf(dto.cpf());
        if (dto.dateOfBirth() != null) patient.setDateOfBirth(dto.dateOfBirth());
        if (dto.gender() != null) patient.setGender(dto.gender());
        if (dto.bloodType() != null) patient.setBloodType(dto.bloodType());
        if (dto.phone() != null) patient.setPhone(dto.phone());
        if (dto.email() != null) patient.setEmail(dto.email());
        if (dto.street() != null) patient.setStreet(dto.street());
        if (dto.city() != null) patient.setCity(dto.city());
        if (dto.state() != null) patient.setState(dto.state());
        if (dto.zipCode() != null) patient.setZipCode(dto.zipCode());
        if (dto.emergencyName() != null) patient.setEmergencyName(dto.emergencyName());
        if (dto.emergencyPhone() != null) patient.setEmergencyPhone(dto.emergencyPhone());
        if (dto.emergencyRelation() != null) patient.setEmergencyRelation(dto.emergencyRelation());
        if (dto.notes() != null) patient.setNotes(dto.notes());
        Patient saved = repository.save(patient);
        auditService.record("patients", "UPDATE", id, userId, null, Map.of("fullName", saved.getFullName()));
        events.publishEvent(new PatientUpdatedEvent(id, userId));
        return PatientResponseDto.fromEntity(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void anonymizePatient(String id, String adminUserId) {
        Patient patient = getPatientById(id);
        patient.setCpf(null); patient.setEmail(null); patient.setPhone(null);
        patient.setStreet(null); patient.setCity(null); patient.setState(null); patient.setZipCode(null);
        patient.setEmergencyName(null); patient.setEmergencyPhone(null); patient.setEmergencyRelation(null);
        patient.setAnonymized(true); patient.setActive(false);
        repository.save(patient);
        auditService.record("patients", "UPDATE", id, adminUserId, null, Map.of("anonymized", true, "active", false));
    }
}
