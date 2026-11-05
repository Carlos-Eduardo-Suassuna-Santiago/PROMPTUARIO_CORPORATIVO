package br.edu.ifrn.promptuario.modules.clinical.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
public record MedicationItemDto(@NotBlank String name, @NotBlank String dosage, @NotBlank String frequency,
                                @Min(1) Integer durationDays, String instructions) {
}
