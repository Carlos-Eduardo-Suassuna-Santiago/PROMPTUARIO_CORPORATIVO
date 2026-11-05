package br.edu.ifrn.promptuario.modules.clinical.dto;

import br.edu.ifrn.promptuario.modules.clinical.model.Appointment;
import br.edu.ifrn.promptuario.modules.clinical.model.AppointmentStatus;
import br.edu.ifrn.promptuario.modules.clinical.model.AppointmentType;
import java.time.Instant;

public record AppointmentResponseDto(String id, String patientId, String patientName, String doctorId,
                                     Instant scheduledAt, AppointmentType appointmentType, String specialty,
                                     AppointmentStatus status, String cancellationReason, String notes,
                                     Instant createdAt) {
    public static AppointmentResponseDto fromEntity(Appointment e) {
        return new AppointmentResponseDto(e.getId(), e.getPatient().getId(), e.getPatient().getFullName(), e.getDoctorId(),
                e.getScheduledAt(), e.getAppointmentType(), e.getSpecialty(), e.getStatus(), e.getCancellationReason(),
                e.getNotes(), e.getCreatedAt());
    }
}
