package br.edu.ifrn.promptuario.modules.clinical.dto;

import br.edu.ifrn.promptuario.modules.clinical.model.Appointment;
import br.edu.ifrn.promptuario.modules.clinical.model.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AppointmentCreateDto(@NotBlank String patientId, @NotBlank String doctorId, String slotId,
                                   @NotNull Instant scheduledAt, AppointmentType appointmentType,
                                   @Size(max = 100) String specialty, String notes) {
    public Appointment toEntity() {
        Appointment e = new Appointment(); e.setDoctorId(doctorId); e.setScheduledAt(scheduledAt);
        e.setAppointmentType(appointmentType == null ? AppointmentType.CONSULTATION : appointmentType);
        e.setSpecialty(specialty); e.setNotes(notes); return e;
    }
}
