package br.edu.ifrn.promptuario.iam.controller;

import br.edu.ifrn.promptuario.iam.dto.ChangePasswordDTO;
import br.edu.ifrn.promptuario.iam.dto.ForgotPasswordDTO;
import br.edu.ifrn.promptuario.iam.dto.LoginRequestDTO;
import br.edu.ifrn.promptuario.iam.dto.PatientRegisterDTO;
import br.edu.ifrn.promptuario.iam.dto.RefreshRequestDTO;
import br.edu.ifrn.promptuario.iam.dto.ResetPasswordDTO;
import br.edu.ifrn.promptuario.iam.dto.TokenResponseDTO;
import br.edu.ifrn.promptuario.iam.dto.UserResponseDTO;
import br.edu.ifrn.promptuario.iam.service.AuthService;
import br.edu.ifrn.promptuario.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário")
    @ApiResponse(responseCode = "200", description = "Tokens emitidos")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) { return authService.login(dto); }

    @PostMapping("/refresh")
    @Operation(summary = "Rotaciona o refresh token")
    public TokenResponseDTO refresh(@Valid @RequestBody RefreshRequestDTO dto) { return authService.refresh(dto); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoga tokens da sessão")
    public void logout(@Valid @RequestBody RefreshRequestDTO dto, @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(dto.refreshToken(), extractBearerToken(authorization));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Altera a senha do usuário autenticado")
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(authentication.getName(), dto);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita recuperação de senha")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        return Map.of("message", authService.forgotPassword(dto.email()));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha")
    public void resetPassword(@Valid @RequestBody ResetPasswordDTO dto) { authService.resetPassword(dto); }

    @PostMapping("/register-patient")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Realiza o autocadastro de paciente")
    public UserResponseDTO registerPatient(@Valid @RequestBody PatientRegisterDTO dto) { return userService.registerPatient(dto); }

    private String extractBearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        return authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim()
                : authorization.trim();
    }
}
