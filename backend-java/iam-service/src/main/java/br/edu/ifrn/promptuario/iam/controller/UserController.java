package br.edu.ifrn.promptuario.iam.controller;

import br.edu.ifrn.promptuario.iam.dto.*;
import br.edu.ifrn.promptuario.iam.exception.ForbiddenException;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista usuários")
    public UserListResponseDTO listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isActive
    ) { return userService.listUsers(page, size, role, isActive); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    @Operation(summary = "Cria um usuário")
    public UserResponseDTO createUser(Authentication authentication, @Valid @RequestBody UserCreateDTO dto) {
        if (hasRole(authentication, "ATTENDANT") && dto.role() != Role.PATIENT) {
            throw new ForbiddenException("Atendente só pode criar usuários pacientes");
        }
        return userService.createUser(dto);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtém o usuário autenticado")
    public UserResponseDTO me(Authentication authentication) { return userService.getUserById(authentication.getName()); }

    @GetMapping("/doctors")
    @Operation(summary = "Lista médicos ativos")
    public DoctorListResponseDTO listDoctors() { return userService.listDoctors(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtém um usuário")
    public UserResponseDTO getUser(@PathVariable String id) { return userService.getUserById(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o cadastro do próprio usuário ou por administrador")
    public UserResponseDTO updateUser(Authentication authentication, @PathVariable String id, @Valid @RequestBody UserUpdateDTO dto) {
        if (!id.equals(authentication.getName()) && !hasRole(authentication, "ADMIN")) {
            throw new ForbiddenException("Acesso negado");
        }
        return userService.updateUser(id, dto);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atribui um papel")
    public UserResponseDTO assignRole(@PathVariable String id, @Valid @RequestBody AssignRoleDTO dto) {
        return userService.assignRole(id, dto.role());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa uma conta")
    public void deactivate(Authentication authentication, @PathVariable String id, @Valid @RequestBody DeactivateUserDTO dto) {
        userService.deactivateUser(id, dto.reason(), authentication.getName());
    }

    @PostMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativa uma conta")
    public void reactivate(@PathVariable String id) { userService.reactivateUser(id); }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
