package br.edu.ifrn.promptuario.modules.patient.service;

import br.edu.ifrn.promptuario.modules.patient.dto.VaccineCreateDto;
import br.edu.ifrn.promptuario.modules.patient.dto.VaccineDto;
import br.edu.ifrn.promptuario.modules.patient.model.Vaccine;
import br.edu.ifrn.promptuario.modules.patient.repository.VaccineRepository;
import br.edu.ifrn.promptuario.shared.audit.AuditService;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class VaccineService {
    private final VaccineRepository repository;
    private final PatientService patients;
    private final AuditService audit;
    public VaccineService(VaccineRepository repository, PatientService patients, AuditService audit) { this.repository = repository; this.patients = patients; this.audit = audit; }

    @Transactional(rollbackFor = Exception.class)
    public Vaccine addVaccine(String patientId, VaccineCreateDto dto, String userId) {
        Vaccine vaccine = dto.toEntity(); vaccine.setPatient(patients.getPatientById(patientId));
        Vaccine saved = repository.save(vaccine);
        audit.record("vaccines", "INSERT", saved.getId(), userId, null, Map.of("patientId", patientId, "name", saved.getName()));
        return saved;
    }
    @Transactional(rollbackFor = Exception.class)
    public Vaccine updateVaccine(String patientId, String vaccineId, VaccineDto dto, String userId) {
        Vaccine vaccine = repository.findById(vaccineId).filter(v -> patientId.equals(v.getPatient().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Vacina não encontrada"));
        if (dto.name() != null) vaccine.setName(dto.name());
        if (dto.dose() != null) vaccine.setDose(dto.dose());
        if (dto.appliedAt() != null) vaccine.setAppliedAt(dto.appliedAt());
        if (dto.nextDoseAt() != null) vaccine.setNextDoseAt(dto.nextDoseAt());
        if (dto.notes() != null) vaccine.setNotes(dto.notes());
        Vaccine saved = repository.save(vaccine);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("dose", saved.getDose());
        values.put("nextDoseAt", saved.getNextDoseAt());
        audit.record("vaccines", "UPDATE", vaccineId, userId, null, values);
        return saved;
    }
    @Transactional(readOnly = true)
    public List<Vaccine> listVaccines(String patientId) { patients.getPatientById(patientId); return repository.findByPatient_Id(patientId); }
    @Transactional(rollbackFor = Exception.class)
    public void deleteVaccine(String patientId, String vaccineId, String userId) {
        Vaccine vaccine = repository.findById(vaccineId).orElseThrow(() -> new ResourceNotFoundException("Vacina não encontrada"));
        if (!patientId.equals(vaccine.getPatient().getId())) throw new ResourceNotFoundException("Vacina não encontrada");
        repository.delete(vaccine); audit.record("vaccines", "DELETE", vaccineId, userId, Map.of("name", vaccine.getName()), null);
    }
}
