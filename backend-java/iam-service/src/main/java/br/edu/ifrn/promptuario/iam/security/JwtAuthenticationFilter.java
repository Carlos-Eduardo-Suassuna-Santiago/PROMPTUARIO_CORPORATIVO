package br.edu.ifrn.promptuario.iam.security;

import br.edu.ifrn.promptuario.iam.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenBlacklistService tokenBlacklistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (tokenBlacklistService.isBlacklisted(token)
                    || !jwtTokenProvider.validateToken(token)
                    || !jwtTokenProvider.isAccessToken(token)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "JWT inválido ou revogado");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String userId = jwtTokenProvider.extractUserId(token);
                String role = jwtTokenProvider.extractRole(token);
                if (role == null || role.isBlank()) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "JWT sem papel de acesso");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "JWT inválido");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
