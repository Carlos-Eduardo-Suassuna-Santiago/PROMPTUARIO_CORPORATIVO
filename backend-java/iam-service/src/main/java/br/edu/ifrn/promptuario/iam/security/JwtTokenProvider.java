package br.edu.ifrn.promptuario.iam.security;

import br.edu.ifrn.promptuario.iam.config.JwtProperties;
import br.edu.ifrn.promptuario.iam.model.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private final byte[] secret;
    private final JWSAlgorithm algorithm;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secret = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        this.algorithm = JWSAlgorithm.parse(jwtProperties.getAlgorithm());

        if (!JWSAlgorithm.HS256.equals(algorithm)) {
            throw new IllegalArgumentException("O IAM suporta somente JWT assinado com HS256");
        }
        if (secret.length < 32) {
            throw new IllegalArgumentException("A chave JWT deve possuir ao menos 256 bits");
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .claim("type", ACCESS_TOKEN_TYPE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .build();

        return sign(claims);
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .claim("type", REFRESH_TOKEN_TYPE)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiration))
                .build();

        return sign(claims);
    }

    public boolean validateToken(String token) {
        try {
            JWTClaimsSet claims = parseAndVerify(token);
            Date expiration = claims.getExpirationTime();
            return expiration != null && expiration.after(new Date());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return claims(token).getSubject();
    }

    public String extractRole(String token) {
        try {
            return claims(token).getStringClaim("role");
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Claim role inválida", ex);
        }
    }

    public Instant extractExpiration(String token) {
        Date expiration = claims(token).getExpirationTime();
        if (expiration == null) {
            throw new IllegalArgumentException("Token sem data de expiração");
        }
        return expiration.toInstant();
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    private String extractTokenType(String token) {
        try {
            return claims(token).getStringClaim("type");
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Claim type inválida", ex);
        }
    }

    private String sign(JWTClaimsSet claims) {
        try {
            SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(algorithm).build(), claims);
            signedJwt.sign(new MACSigner(secret));
            return signedJwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Não foi possível assinar o JWT", ex);
        }
    }

    private JWTClaimsSet claims(String token) {
        try {
            return parseAndVerify(token);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT inválido", ex);
        }
    }

    private JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(normalize(token));
            if (!algorithm.equals(signedJwt.getHeader().getAlgorithm())) {
                throw new IllegalArgumentException("Algoritmo JWT inesperado");
            }
            if (!signedJwt.verify(new MACVerifier(secret))) {
                throw new IllegalArgumentException("Assinatura JWT inválida");
            }
            return signedJwt.getJWTClaimsSet();
        } catch (ParseException | JOSEException ex) {
            throw new IllegalArgumentException("JWT malformado", ex);
        }
    }

    private String normalize(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT ausente");
        }
        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            normalized = normalized.substring(7).trim();
        }
        if (normalized.length() >= 2
                && normalized.charAt(0) == normalized.charAt(normalized.length() - 1)
                && (normalized.charAt(0) == '\"' || normalized.charAt(0) == '\'')) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }
}
