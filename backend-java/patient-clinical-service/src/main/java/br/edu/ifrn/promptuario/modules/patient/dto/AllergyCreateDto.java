package br.edu.ifrn.promptuario.modules.patient.dto;

import br.edu.ifrn.promptuario.modules.patient.model.Allergy;
import br.edu.ifrn.promptuario.modules.patient.model.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;

public record AllergyCreateDto(
        @NotBlank @Size(min = 2, max = 255) String substance,
        @NotNull AllergySeverity severity,
        @JsonAlias({"reaction_type", "reactionType"}) String reactionType,
        String notes) {

    public Allergy toEntity() {
        Allergy allergy = new Allergy();
        allergy.setSubstance(substance);
        allergy.setSeverity(severity);
        allergy.setReactionType(reactionType);
        allergy.setNotes(notes);
        return allergy;
    }
}
