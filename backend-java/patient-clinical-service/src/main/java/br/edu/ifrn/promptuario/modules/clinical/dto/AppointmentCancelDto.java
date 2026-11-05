package br.edu.ifrn.promptuario.modules.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppointmentCancelDto(@NotBlank @Size(min = 5, max = 255) String reason) {
}
