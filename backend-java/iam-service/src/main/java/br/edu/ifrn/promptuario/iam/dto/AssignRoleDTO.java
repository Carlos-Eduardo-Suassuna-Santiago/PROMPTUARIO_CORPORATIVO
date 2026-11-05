package br.edu.ifrn.promptuario.iam.dto;

import br.edu.ifrn.promptuario.iam.model.Role;
import jakarta.validation.constraints.NotNull;

public record AssignRoleDTO(
        @NotNull(message = "Papel é obrigatório")
        Role role
) {
}
