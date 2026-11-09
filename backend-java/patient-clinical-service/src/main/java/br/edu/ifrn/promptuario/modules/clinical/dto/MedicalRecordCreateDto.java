package br.edu.ifrn.promptuario.modules.clinical.dto;

import br.edu.ifrn.promptuario.modules.clinical.model.MedicalRecord;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record MedicalRecordCreateDto(@NotBlank String appointmentId, @NotBlank String patientId,
                                     @NotBlank String chiefComplaint, String anamnesis, String physicalExam,
                                     String diagnosis, List<String> diagnosisCodes, String treatmentPlan,
                                     String observations, Map<String, Object> richNotes) {
    public MedicalRecord toEntity() {
        MedicalRecord e = new MedicalRecord(); e.setPatientId(patientId); e.setChiefComplaint(chiefComplaint);
        e.setAnamnesis(anamnesis); e.setPhysicalExam(physicalExam); e.setDiagnosis(diagnosis);
        e.setDiagnosisCodes(diagnosisCodes == null ? new ArrayList<>() : diagnosisCodes);
        e.setTreatmentPlan(treatmentPlan); e.setObservations(observations); e.setRichNotes(richNotes); return e;
    }
}
