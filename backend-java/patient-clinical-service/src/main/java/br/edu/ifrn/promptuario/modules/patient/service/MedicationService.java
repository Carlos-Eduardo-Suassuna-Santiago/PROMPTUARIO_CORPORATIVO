package br.edu.ifrn.promptuario.modules.patient.service;

import br.edu.ifrn.promptuario.modules.patient.dto.MedicationCreateDto;
import br.edu.ifrn.promptuario.modules.patient.dto.MedicationUpdateDto;
import br.edu.ifrn.promptuario.modules.patient.model.ContinuousMedication;
import br.edu.ifrn.promptuario.modules.patient.model.MedicationChangeType;
import br.edu.ifrn.promptuario.modules.patient.model.MedicationHistory;
import br.edu.ifrn.promptuario.modules.patient.repository.ContinuousMedicationRepository;
import br.edu.ifrn.promptuario.modules.patient.repository.MedicationHistoryRepository;
import br.edu.ifrn.promptuario.shared.audit.AuditService;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MedicationService {
    private final ContinuousMedicationRepository repository;
    private final MedicationHistoryRepository historyRepository;
    private final PatientService patients;
    private final AuditService audit;
    public MedicationService(ContinuousMedicationRepository repository, MedicationHistoryRepository historyRepository, PatientService patients, AuditService audit) {
        this.repository = repository; this.historyRepository = historyRepository; this.patients = patients; this.audit = audit;
    }
    @Transactional(rollbackFor = Exception.class)
    public ContinuousMedication createMedication(String patientId, MedicationCreateDto dto, String userId) {
        ContinuousMedication medication = dto.toEntity(); medication.setPatient(patients.getPatientById(patientId));
        ContinuousMedication saved = repository.save(medication); saveHistory(saved, MedicationChangeType.CREATED, userId, snapshot(saved));
        audit.record("continuous_medications", "INSERT", saved.getId(), userId, null, snapshot(saved)); return saved;
    }
    @Transactional(rollbackFor = Exception.class)
    public ContinuousMedication addMedication(String patientId, MedicationCreateDto dto, String userId) {
        return createMedication(patientId, dto, userId);
    }
    @Transactional(readOnly = true)
    public List<ContinuousMedication> listMedications(String patientId, boolean activeOnly) { patients.getPatientById(patientId); return activeOnly ? repository.findByPatient_IdAndActiveTrue(patientId) : repository.findByPatient_Id(patientId); }
    @Transactional(rollbackFor = Exception.class)
    public ContinuousMedication updateMedication(String patientId, String medicationId, MedicationUpdateDto dto, String userId) {
        ContinuousMedication medication = find(patientId, medicationId); Map<String,Object> old = snapshot(medication);
        if (dto.dosage() != null) medication.setDosage(dto.dosage()); if (dto.frequency() != null) medication.setFrequency(dto.frequency());
        if (dto.route() != null) medication.setRoute(dto.route()); if (dto.prescribingDoctor() != null) medication.setPrescribingDoctor(dto.prescribingDoctor());
        if (dto.endedAt() != null) medication.setEndedAt(dto.endedAt()); if (dto.endReason() != null) medication.setEndReason(dto.endReason()); if (dto.notes() != null) medication.setNotes(dto.notes());
        medication.setVersion(medication.getVersion() + 1); ContinuousMedication saved = repository.save(medication);
        saveHistory(saved, MedicationChangeType.UPDATED, userId, old); audit.record("continuous_medications", "UPDATE", medicationId, userId, old, snapshot(saved)); return saved;
    }
    @Transactional(rollbackFor = Exception.class)
    public ContinuousMedication deactivateMedication(String patientId, String medicationId, String reason, String userId) {
        ContinuousMedication medication = find(patientId, medicationId); Map<String,Object> old = snapshot(medication);
        medication.setActive(false); medication.setEndedAt(java.time.LocalDate.now()); medication.setEndReason(reason); medication.setVersion(medication.getVersion()+1);
        ContinuousMedication saved = repository.save(medication); saveHistory(saved, MedicationChangeType.DEACTIVATED, userId, old); audit.record("continuous_medications", "UPDATE", medicationId, userId, old, snapshot(saved)); return saved;
    }
    private ContinuousMedication find(String patientId, String medicationId) { return repository.findById(medicationId).filter(m -> patientId.equals(m.getPatient().getId())).orElseThrow(() -> new ResourceNotFoundException("Medicação não encontrada")); }
    private void saveHistory(ContinuousMedication m, MedicationChangeType type, String userId, Map<String,Object> snapshot) { MedicationHistory h = new MedicationHistory(); h.setPatient(m.getPatient()); h.setMedication(m); h.setName(m.getName()); h.setDosage(m.getDosage()); h.setFrequency(m.getFrequency()); h.setPrescribingDoctor(m.getPrescribingDoctor()); h.setStartedAt(m.getStartedAt()); h.setEndedAt(m.getEndedAt()); h.setEndReason(m.getEndReason()); h.setNotes(m.getNotes()); h.setActive(m.isActive()); h.setVersion(m.getVersion()); h.setChangeType(type); h.setChangedBy(userId); h.setSnapshot(snapshot); historyRepository.save(h); }
    private Map<String,Object> snapshot(ContinuousMedication m) { Map<String,Object> s = new LinkedHashMap<>(); s.put("id",m.getId()); s.put("name",m.getName()); s.put("dosage",m.getDosage()); s.put("frequency",m.getFrequency()); s.put("route",m.getRoute()); s.put("prescribingDoctor",m.getPrescribingDoctor()); s.put("startedAt",m.getStartedAt()); s.put("endedAt",m.getEndedAt()); s.put("endReason",m.getEndReason()); s.put("active",m.isActive()); s.put("version",m.getVersion()); s.put("notes",m.getNotes()); return s; }
}
