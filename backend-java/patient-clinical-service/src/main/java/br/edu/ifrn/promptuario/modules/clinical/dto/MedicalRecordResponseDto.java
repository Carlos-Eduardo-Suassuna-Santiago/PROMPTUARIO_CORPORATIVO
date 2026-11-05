package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.MedicalRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
public record MedicalRecordResponseDto(String id, String appointmentId, String patientId, String patientName,
                                       String doctorId, String chiefComplaint, String anamnesis, String physicalExam,
                                       String diagnosis, List<String> diagnosisCodes, String treatmentPlan,
                                       String observations, Map<String, Object> richNotes, String signatureHash,
                                       String signedBy, Instant signedAt, String aiAnalysisId,
                                       List<PrescriptionResponseDto> prescriptions, List<ExamRequestResponseDto> examRequests,
                                       List<MedicalCertificateResponseDto> certificates,
                                       List<MedicalRecordHistoryResponseDto> history, Instant createdAt, Instant updatedAt) {
    public static MedicalRecordResponseDto fromEntity(MedicalRecord e) {
        return new MedicalRecordResponseDto(e.getId(), e.getAppointment().getId(), e.getPatientId(), null, e.getDoctorId(),
                e.getChiefComplaint(), e.getAnamnesis(), e.getPhysicalExam(), e.getDiagnosis(), e.getDiagnosisCodes(),
                e.getTreatmentPlan(), e.getObservations(), e.getRichNotes(), e.getSignatureHash(), e.getSignedBy(), e.getSignedAt(),
                e.getAiAnalysisId(), e.getPrescriptions().stream().map(PrescriptionResponseDto::fromEntity).toList(),
                e.getExamRequests().stream().map(ExamRequestResponseDto::fromEntity).toList(),
                e.getCertificates().stream().map(MedicalCertificateResponseDto::fromEntity).toList(),
                e.getHistory().stream().map(MedicalRecordHistoryResponseDto::fromEntity).toList(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
