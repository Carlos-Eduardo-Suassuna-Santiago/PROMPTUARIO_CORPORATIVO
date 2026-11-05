package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.ExamRequest;
import br.edu.ifrn.promptuario.modules.clinical.model.ExamUrgency;
import java.time.Instant;
public record ExamRequestResponseDto(String id, String recordId, String patientId, String examType, ExamUrgency urgency,
                                     String instructions, String result, Instant resultDate, Instant createdAt) {
    public static ExamRequestResponseDto fromEntity(ExamRequest e) {
        return new ExamRequestResponseDto(e.getId(), e.getRecord().getId(), e.getPatientId(), e.getExamType(), e.getUrgency(),
                e.getInstructions(), e.getResult(), e.getResultDate(), e.getCreatedAt());
    }
}
