package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AllergyDto(@NotBlank @Size(min = 2, max = 255) String substance,
                         @NotNull AllergySeverity severity, String reactionType, String notes) {
}
