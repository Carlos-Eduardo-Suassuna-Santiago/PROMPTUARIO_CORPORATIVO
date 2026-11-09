package br.edu.ifrn.promptuario.gateway.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

public class PolicyAwareRedisRateLimiter implements RateLimiter<RedisRateLimiter.Config> {

    public static final String RATE_LIMIT_REJECTED_HEADER = "X-Gateway-Rate-Limit-Rejected";
    private static final String API_KEY_PREFIX = "apikey:";
    private static final String USER_PREFIX = "user:";

    private final RedisRateLimiter anonRateLimiter;
    private final RedisRateLimiter authRateLimiter;
    private final RedisRateLimiter apiKeyRateLimiter;

    public PolicyAwareRedisRateLimiter(
            RedisRateLimiter anonRateLimiter,
            RedisRateLimiter authRateLimiter,
            RedisRateLimiter apiKeyRateLimiter) {
        this.anonRateLimiter = anonRateLimiter;
        this.authRateLimiter = authRateLimiter;
        this.apiKeyRateLimiter = apiKeyRateLimiter;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        RateLimitPolicy policy = policyFor(id);
        return policy.rateLimiter().isAllowed(policy.bucketId(), id)
                .map(response -> addRetryAfter(response, policy));
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return RedisRateLimiter.Config.class;
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return new RedisRateLimiter.Config();
    }

    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return Map.of();
    }

    private RateLimitPolicy policyFor(String key) {
        if (key.startsWith(API_KEY_PREFIX)) {
            return new RateLimitPolicy("api-key", apiKeyRateLimiter, 120, 60);
        }
        if (key.startsWith(USER_PREFIX)) {
            return new RateLimitPolicy("authenticated-user", authRateLimiter, 300, 60);
        }
        return new RateLimitPolicy("anonymous-ip", anonRateLimiter, 30, 60);
    }

    private Response addRetryAfter(Response response, RateLimitPolicy policy) {
        if (response.isAllowed()) {
            return response;
        }
        Map<String, String> headers = new HashMap<>(response.getHeaders());
        int remainingTokens = parseRemainingTokens(headers.get(policy.rateLimiter().getRemainingHeader()));
        int tokensToRefill = Math.max(1, policy.requestedTokens() - remainingTokens);
        int retryAfterSeconds = Math.max(
                1,
                (tokensToRefill + policy.replenishRate() - 1) / policy.replenishRate());
        headers.put("Retry-After", Integer.toString(retryAfterSeconds));
        headers.put(RATE_LIMIT_REJECTED_HEADER, "true");
        return new Response(false, headers);
    }

    private int parseRemainingTokens(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private record RateLimitPolicy(
            String bucketId,
            RedisRateLimiter rateLimiter,
            int replenishRate,
            int requestedTokens) {
    }
}