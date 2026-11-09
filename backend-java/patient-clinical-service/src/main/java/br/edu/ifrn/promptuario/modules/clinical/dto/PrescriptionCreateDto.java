package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.MedicalRecord;
import br.edu.ifrn.promptuario.modules.clinical.model.Prescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record PrescriptionCreateDto(@NotEmpty List<@Valid MedicationItemDto> medications, String instructions,
                                    @Min(1) @Max(365) Integer validDays) {
    public Prescription toEntity(MedicalRecord record, String patientId, String doctorId) {
        Prescription e = new Prescription(); e.setRecord(record); e.setPatientId(patientId); e.setDoctorId(doctorId);
        e.setMedications(medications.stream().map(m -> java.util.Map.<String, Object>of("name", m.name(), "dosage", m.dosage(),
                "frequency", m.frequency(), "durationDays", m.durationDays(), "instructions", m.instructions())).toList());
        e.setInstructions(instructions); e.setValidDays(validDays == null ? 30 : validDays); return e;
    }
}
