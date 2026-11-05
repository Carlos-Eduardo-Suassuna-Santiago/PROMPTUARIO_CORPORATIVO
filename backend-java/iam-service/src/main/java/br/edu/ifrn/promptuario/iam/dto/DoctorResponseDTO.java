package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DoctorResponseDTO(
        String id,
        @JsonProperty("full_name") String fullName,
        String email,
        String crm,
        String specialty
) {
}
