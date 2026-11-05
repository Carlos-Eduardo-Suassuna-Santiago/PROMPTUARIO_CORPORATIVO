package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.Prescription;
import java.time.Instant;
import java.util.List;
import java.util.Map;
public record PrescriptionResponseDto(String id, String recordId, String patientId, String doctorId,
                                      List<Map<String, Object>> medications, String instructions, int validDays,
                                      String pdfS3Key, Instant pdfGeneratedAt, String signatureHash, String signedBy,
                                      Instant signedAt, Instant createdAt) {
    public static PrescriptionResponseDto fromEntity(Prescription e) {
        return new PrescriptionResponseDto(e.getId(), e.getRecord().getId(), e.getPatientId(), e.getDoctorId(), e.getMedications(),
                e.getInstructions(), e.getValidDays(), e.getPdfS3Key(), e.getPdfGeneratedAt(), e.getSignatureHash(), e.getSignedBy(),
                e.getSignedAt(), e.getCreatedAt());
    }
}
