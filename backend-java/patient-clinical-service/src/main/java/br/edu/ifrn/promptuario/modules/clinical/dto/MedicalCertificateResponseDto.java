package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.MedicalCertificate;
import java.time.Instant;
import java.time.LocalDate;
public record MedicalCertificateResponseDto(String id, String recordId, String patientId, String doctorId, String reason,
                                            int daysOff, LocalDate startDate, String notes, String pdfS3Key,
                                            Instant pdfGeneratedAt, String signatureHash, String signedBy,
                                            Instant signedAt, Instant createdAt) {
    public static MedicalCertificateResponseDto fromEntity(MedicalCertificate e) {
        return new MedicalCertificateResponseDto(e.getId(), e.getRecord().getId(), e.getPatientId(), e.getDoctorId(), e.getReason(),
                e.getDaysOff(), e.getStartDate(), e.getNotes(), e.getPdfS3Key(), e.getPdfGeneratedAt(), e.getSignatureHash(),
                e.getSignedBy(), e.getSignedAt(), e.getCreatedAt());
    }
}
