package br.edu.ifrn.promptuario.iam.dto;

import java.util.List;

public record UserListResponseDTO(List<UserResponseDTO> items, long total, int page, int size) {
    public UserListResponseDTO {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
