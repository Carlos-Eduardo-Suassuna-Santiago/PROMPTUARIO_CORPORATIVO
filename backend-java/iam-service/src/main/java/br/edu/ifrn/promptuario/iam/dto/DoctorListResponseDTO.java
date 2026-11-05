package br.edu.ifrn.promptuario.iam.dto;

import java.util.List;

public record DoctorListResponseDTO(List<DoctorResponseDTO> items, int total) {
    public DoctorListResponseDTO {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
