package br.edu.ifrn.promptuario.modules.clinical.dto;
import br.edu.ifrn.promptuario.modules.clinical.model.MedicalCertificate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record MedicalCertificateCreateDto(@NotBlank String reason, @NotNull @Min(1) Integer daysOff,
                                          @NotNull LocalDate startDate, String notes) {
    public MedicalCertificate toEntity(String patientId, String doctorId) {
        MedicalCertificate e = new MedicalCertificate(); e.setPatientId(patientId); e.setDoctorId(doctorId);
        e.setReason(reason); e.setDaysOff(daysOff); e.setStartDate(startDate); e.setNotes(notes); return e;
    }
}
