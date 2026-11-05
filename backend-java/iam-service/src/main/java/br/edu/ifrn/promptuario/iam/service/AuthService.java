package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.dto.ChangePasswordDTO;
import br.edu.ifrn.promptuario.iam.dto.LoginRequestDTO;
import br.edu.ifrn.promptuario.iam.dto.RefreshRequestDTO;
import br.edu.ifrn.promptuario.iam.dto.ResetPasswordDTO;
import br.edu.ifrn.promptuario.iam.dto.TokenResponseDTO;
import br.edu.ifrn.promptuario.iam.dto.UserResponseDTO;
import br.edu.ifrn.promptuario.iam.exception.ForbiddenException;
import br.edu.ifrn.promptuario.iam.model.PasswordResetToken;
import br.edu.ifrn.promptuario.iam.model.RefreshToken;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.repository.PasswordResetRepository;
import br.edu.ifrn.promptuario.iam.repository.RefreshTokenRepository;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import br.edu.ifrn.promptuario.iam.security.JwtTokenProvider;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";
    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "Se o e-mail estiver cadastrado, você receberá instruções para redefinir a senha.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetRepository passwordResetRepository,
            JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmailIgnoreCase(dto.email().trim())
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(dto.password(), user.getHashedPassword())) {
            throw new IllegalArgumentException(INVALID_CREDENTIALS);
        }
        ensureUserIsActive(user);
        validateTwoFactorCode(user, dto.totpCode());
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponseDTO refresh(RefreshRequestDTO dto) {
        String rawRefreshToken = dto.refreshToken();
        if (!jwtTokenProvider.validateToken(rawRefreshToken) || !jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            throw new IllegalArgumentException("Refresh token inválido ou expirado");
        }

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido ou expirado"));

        currentToken.setRevoked(true);
        User user = currentToken.getUser();
        ensureUserIsActive(user);
        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String refreshToken, String rawAccessToken) {
        if (rawAccessToken != null && !rawAccessToken.isBlank() && jwtTokenProvider.validateToken(rawAccessToken)) {
            tokenBlacklistService.blacklistToken(rawAccessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(refreshToken))
                    .ifPresent(token -> token.setRevoked(true));
        }
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordDTO dto) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(dto.currentPassword(), user.getHashedPassword())) {
            throw new IllegalArgumentException("Senha atual inválida");
        }

        user.setHashedPassword(passwordEncoder.encode(dto.newPassword()));
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public String forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String rawToken = generateSecureToken();
            passwordResetRepository.save(new PasswordResetToken(
                    user,
                    hashToken(rawToken),
                    Instant.now().plus(2, ChronoUnit.HOURS)
            ));
            // O token em texto deve ser encaminhado exclusivamente pelo serviço de e-mail.
        });
        return GENERIC_FORGOT_PASSWORD_MESSAGE;
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        PasswordResetToken resetToken = passwordResetRepository
                .findByTokenHashAndUsedFalse(hashToken(dto.token()))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Token de redefinição inválido ou expirado"));

        User user = resetToken.getUser();
        user.setHashedPassword(passwordEncoder.encode(dto.newPassword()));
        resetToken.setUsed(true);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    /**
     * Emite um novo par de tokens para um usuário já autenticado por um provedor confiável.
     */
    public TokenResponseDTO issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenRepository.save(new RefreshToken(
                user,
                hashToken(refreshToken),
                jwtTokenProvider.extractExpiration(refreshToken)
        ));

        return new TokenResponseDTO(
                accessToken,
                refreshToken,
                jwtTokenProvider.extractExpiration(accessToken).getEpochSecond() - Instant.now().getEpochSecond(),
                UserResponseDTO.fromEntity(user)
        );
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private void ensureUserIsActive(User user) {
        if (!user.isActive()) {
            throw new ForbiddenException("Usuário inativo");
        }
    }

    private void validateTwoFactorCode(User user, String totpCode) {
        if (!user.isTwoFactorEnabled()) {
            return;
        }
        if (totpCode == null || totpCode.isBlank() || user.getTwoFactorSecret() == null || user.getTwoFactorSecret().isBlank()) {
            throw new ForbiddenException("Código de autenticação em dois fatores é obrigatório");
        }
        try {
            if (!googleAuthenticator.authorize(user.getTwoFactorSecret(), Integer.parseInt(totpCode))) {
                throw new ForbiddenException("Código de autenticação em dois fatores inválido");
            }
        } catch (NumberFormatException ex) {
            throw new ForbiddenException("Código de autenticação em dois fatores inválido");
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível na JVM", ex);
        }
    }
}
