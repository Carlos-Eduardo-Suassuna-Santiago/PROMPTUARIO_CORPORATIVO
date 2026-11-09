package br.edu.ifrn.promptuario.iam.service;

import br.edu.ifrn.promptuario.iam.config.OAuthConfig;
import br.edu.ifrn.promptuario.iam.dto.TokenResponseDTO;
import br.edu.ifrn.promptuario.iam.exception.ForbiddenException;
import br.edu.ifrn.promptuario.iam.messaging.IamEventPublisher;
import br.edu.ifrn.promptuario.iam.messaging.events.UserCreatedEvent;
import br.edu.ifrn.promptuario.iam.model.OAuthAccount;
import br.edu.ifrn.promptuario.iam.model.Role;
import br.edu.ifrn.promptuario.iam.model.User;
import br.edu.ifrn.promptuario.iam.repository.OAuthAccountRepository;
import br.edu.ifrn.promptuario.iam.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class OAuthService {

    private static final String GOOGLE_PROVIDER = "google";
    private static final String GOOGLE_AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String STATE_KEY_PREFIX = "oauth2:google:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final OAuthConfig oauthConfig;
    private final String frontendCallbackUrl;
    private final StringRedisTemplate redisTemplate;
    private final RestClient restClient;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final AuthService authService;
    private final IamEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthService(
            OAuthConfig oauthConfig,
            @Value("${app.frontend-callback-url}") String frontendCallbackUrl,
            StringRedisTemplate redisTemplate,
            RestClient.Builder restClientBuilder,
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            AuthService authService,
            IamEventPublisher eventPublisher,
            PasswordEncoder passwordEncoder
    ) {
        this.oauthConfig = oauthConfig;
        this.frontendCallbackUrl = frontendCallbackUrl;
        this.redisTemplate = redisTemplate;
        this.restClient = restClientBuilder.build();
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.authService = authService;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }

    public URI googleAuthorizationUri() {
        ensureGoogleIsConfigured();
        String state = generateState();
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, "pending", STATE_TTL);

        return UriComponentsBuilder.fromUriString(GOOGLE_AUTHORIZATION_URI)
                .queryParam("client_id", oauthConfig.getClientId())
                .queryParam("redirect_uri", oauthConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }

    @Transactional
    public TokenResponseDTO authenticateGoogleCallback(String authorizationCode, String state) {
        ensureGoogleIsConfigured();
        validateAndConsumeState(state);
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Código de autorização do Google ausente");
        }

        GoogleTokenResponse tokenResponse = exchangeAuthorizationCode(authorizationCode);
        GoogleUserInfo googleUser = fetchUserInfo(tokenResponse.accessToken());
        User user = findOrProvisionUser(googleUser);
        if (!user.isActive()) {
            throw new ForbiddenException("Usuário inativo");
        }
        return authService.issueTokenPair(user);
    }

    @Transactional(readOnly = true)
    public List<String> linkedProviders(String userId) {
        return oauthAccountRepository.findByUserId(userId).stream()
                .map(OAuthAccount::getProvider)
                .distinct()
                .sorted()
                .toList();
    }

    public URI frontendCallbackUri(TokenResponseDTO tokens) {
        return UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("access_token", tokens.accessToken())
                .queryParam("refresh_token", tokens.refreshToken())
                .build()
                .encode()
                .toUri();
    }

    private User findOrProvisionUser(GoogleUserInfo googleUser) {
        return oauthAccountRepository.findByProviderAndProviderUserId(GOOGLE_PROVIDER, googleUser.subject())
                .map(OAuthAccount::getUser)
                .orElseGet(() -> linkOrCreateUser(googleUser));
    }

    private User linkOrCreateUser(GoogleUserInfo googleUser) {
        User user = userRepository.findByEmailIgnoreCase(googleUser.email()).orElseGet(() -> {
            User createdUser = new User(
                    googleUser.email(),
                    passwordEncoder.encode(generateRandomPassword()),
                    googleUser.name(),
                    Role.PATIENT
            );
            createdUser.setVerified(true);
            createdUser = userRepository.saveAndFlush(createdUser);
            eventPublisher.publishUserCreated(new UserCreatedEvent(
                    createdUser.getId(),
                    createdUser.getEmail(),
                    createdUser.getFullName(),
                    createdUser.getRole().name(),
                    createdUser.getCpf(),
                    createdUser.getCreatedAt()
            ));
            return createdUser;
        });

        oauthAccountRepository.save(new OAuthAccount(user, GOOGLE_PROVIDER, googleUser.subject(), googleUser.email()));
        return user;
    }

    private GoogleTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", authorizationCode);
        form.add("client_id", oauthConfig.getClientId());
        form.add("client_secret", oauthConfig.getClientSecret());
        form.add("redirect_uri", oauthConfig.getRedirectUri());
        form.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(GOOGLE_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || isBlank(response.accessToken()) || isBlank(response.idToken())) {
                throw new ResponseStatusException(BAD_GATEWAY, "Resposta inválida do Google OAuth2");
            }
            return response;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Não foi possível validar a autorização com o Google", ex);
        }
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) {
        try {
            GoogleUserInfo response = restClient.get()
                    .uri(GOOGLE_USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfo.class);
            if (response == null || isBlank(response.subject()) || isBlank(response.email()) || !response.emailVerified()) {
                throw new ResponseStatusException(BAD_GATEWAY, "Dados de perfil do Google inválidos");
            }
            return new GoogleUserInfo(
                    response.subject(),
                    response.email().trim().toLowerCase(Locale.ROOT),
                    fallbackName(response.name(), response.email()),
                    true
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Não foi possível obter o perfil do Google", ex);
        }
    }

    private void validateAndConsumeState(String state) {
        if (state == null || state.isBlank() || redisTemplate.opsForValue().getAndDelete(STATE_KEY_PREFIX + state) == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Estado OAuth2 inválido ou expirado");
        }
    }

    private void ensureGoogleIsConfigured() {
        if (!oauthConfig.isConfigured()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Google OAuth2 não está configurado");
        }
    }

    private String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String fallbackName(String name, String email) {
        return isBlank(name) ? email.substring(0, email.indexOf('@')) : name.trim();
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken
    ) {
    }

    private record GoogleUserInfo(
            @JsonProperty("sub") String subject,
            String email,
            @JsonProperty("name") String name,
            @JsonProperty("email_verified") boolean emailVerified
    ) {
    }
}
