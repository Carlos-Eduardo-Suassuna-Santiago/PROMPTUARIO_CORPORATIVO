package br.edu.ifrn.promptuario.modules.patient.service;

import br.edu.ifrn.promptuario.modules.patient.dto.AllergyCreateDto;
import br.edu.ifrn.promptuario.modules.patient.event.AllergyAddedEvent;
import br.edu.ifrn.promptuario.modules.patient.model.Allergy;
import br.edu.ifrn.promptuario.modules.patient.repository.AllergyRepository;
import br.edu.ifrn.promptuario.shared.audit.AuditService;
import br.edu.ifrn.promptuario.shared.exception.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AllergyService {
    private final AllergyRepository repository;
    private final PatientService patients;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public AllergyService(AllergyRepository repository, PatientService patients, AuditService audit, ApplicationEventPublisher events) {
        this.repository = repository; this.patients = patients; this.audit = audit; this.events = events;
    }

    @Transactional(rollbackFor = Exception.class)
    public Allergy addAllergy(String patientId, AllergyCreateDto dto) {
        Allergy allergy = dto.toEntity(); allergy.setPatient(patients.getPatientById(patientId));
        Allergy saved = repository.save(allergy);
        audit.record("allergies", "INSERT", saved.getId(), null, null, Map.of("patientId", patientId, "substance", saved.getSubstance()));
        events.publishEvent(new AllergyAddedEvent(patientId, saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Allergy> listAllergies(String patientId) { patients.getPatientById(patientId); return repository.findByPatient_Id(patientId); }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAllergy(String patientId, String allergyId) {
        Allergy allergy = repository.findById(allergyId).orElseThrow(() -> new ResourceNotFoundException("Alergia não encontrada"));
        if (!patientId.equals(allergy.getPatient().getId())) throw new ResourceNotFoundException("Alergia não encontrada");
        repository.delete(allergy); audit.record("allergies", "DELETE", allergyId, null, Map.of("substance", allergy.getSubstance()), null);
    }
}
