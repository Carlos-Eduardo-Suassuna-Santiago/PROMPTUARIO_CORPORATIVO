package br.edu.ifrn.promptuario.gateway.config;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-Api-Key");
            if (StringUtils.hasText(apiKey)) {
                return Mono.fromSupplier(() -> "apikey:" + sha256(apiKey));
            }

            return ReactiveSecurityContextHolder.getContext()
                    .map(context -> context.getAuthentication())
                    .filter(RateLimiterConfig::isAuthenticatedUser)
                    .map(Authentication::getName)
                    .filter(StringUtils::hasText)
                    .map(userId -> "user:" + userId)
                    .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange)));
        };
    }

    @Bean("anonRateLimiter")
    public RedisRateLimiter anonRateLimiter() {
        return new RedisRateLimiter(30, 60, 60);
    }

    @Bean("authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(300, 400, 60);
    }

    @Bean("apiKeyRateLimiter")
    public RedisRateLimiter apiKeyRateLimiter() {
        return new RedisRateLimiter(120, 150, 60);
    }

    @Bean
    @Primary
    public PolicyAwareRedisRateLimiter policyAwareRedisRateLimiter(
            @Qualifier("anonRateLimiter") RedisRateLimiter anonRateLimiter,
            @Qualifier("authRateLimiter") RedisRateLimiter authRateLimiter,
            @Qualifier("apiKeyRateLimiter") RedisRateLimiter apiKeyRateLimiter) {
        return new PolicyAwareRedisRateLimiter(
                anonRateLimiter,
                authRateLimiter,
                apiKeyRateLimiter);
    }

    private static boolean isAuthenticatedUser(Authentication authentication) {
        return authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static String clientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String firstAddress = forwardedFor.split(",", 2)[0].trim();
            if (StringUtils.hasText(firstAddress)) {
                return firstAddress;
            }
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return "unknown";
        }
        return remoteAddress.getAddress() == null
                ? remoteAddress.getHostString()
                : remoteAddress.getAddress().getHostAddress();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }
}