package br.edu.ifrn.promptuario.modules.clinical.dto;
import java.util.List;
import java.util.Map;
public record MedicalRecordUpdateDto(String chiefComplaint, String anamnesis, String physicalExam, String diagnosis,
                                     List<String> diagnosisCodes, String treatmentPlan, String observations,
                                     Map<String, Object> richNotes) {
}
