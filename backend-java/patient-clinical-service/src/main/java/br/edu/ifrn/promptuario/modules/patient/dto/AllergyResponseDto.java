package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.Allergy;
import br.edu.ifrn.promptuario.modules.patient.model.AllergySeverity;

import java.time.Instant;

public record AllergyResponseDto(String id, String patientId, String substance, AllergySeverity severity,
                                 String reactionType, String notes, Instant createdAt) {
    public static AllergyResponseDto fromEntity(Allergy e) {
        return new AllergyResponseDto(e.getId(), e.getPatient().getId(), e.getSubstance(), e.getSeverity(),
                e.getReactionType(), e.getNotes(), e.getCreatedAt());
    }
}
