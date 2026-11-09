package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.MedicalRecordHistory;
import java.time.Instant;
import java.util.Map;
public record MedicalRecordHistoryResponseDto(String id, String changedBy, String changeType,
                                              Map<String, Object> snapshot, Instant createdAt) {
    public static MedicalRecordHistoryResponseDto fromEntity(MedicalRecordHistory e) {
        return new MedicalRecordHistoryResponseDto(e.getId(), e.getChangedBy(), e.getChangeType(), e.getSnapshot(), e.getCreatedAt());
    }
}
