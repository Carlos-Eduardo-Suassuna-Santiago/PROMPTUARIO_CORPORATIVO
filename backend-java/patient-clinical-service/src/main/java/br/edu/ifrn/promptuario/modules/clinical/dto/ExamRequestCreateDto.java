package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.ExamRequest;
import br.edu.ifrn.promptuario.modules.clinical.model.ExamUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ExamRequestCreateDto(@NotBlank @Size(min = 3, max = 255) String examType,
                                   ExamUrgency urgency, String instructions) {
    public ExamRequest toEntity(String patientId, String doctorId) {
        ExamRequest e = new ExamRequest(); e.setPatientId(patientId); e.setDoctorId(doctorId); e.setExamType(examType);
        e.setUrgency(urgency == null ? ExamUrgency.ROUTINE : urgency); e.setInstructions(instructions); return e;
    }
}
