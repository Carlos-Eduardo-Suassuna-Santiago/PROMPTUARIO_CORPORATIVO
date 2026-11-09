package br.edu.ifrn.promptuario.modules.patient.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MedicationUpdateDto(@Size(max = 100) String dosage, @Size(max = 100) String frequency,
                                  String route, String prescribingDoctor, LocalDate endedAt,
                                  @Size(max = 255) String endReason, String notes) {
}
