package br.edu.ifrn.promptuario.iam.controller;

import br.edu.ifrn.promptuario.iam.dto.TokenResponseDTO;
import br.edu.ifrn.promptuario.iam.service.OAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private final OAuthService oauthService;

    public OAuthController(OAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle() {
        return redirect(oauthService.googleAuthorizationUri());
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        if (error != null && !error.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autorização do Google recusada");
        }
        TokenResponseDTO tokens = oauthService.authenticateGoogleCallback(code, state);
        return redirect(oauthService.frontendCallbackUri(tokens));
    }

    @GetMapping("/accounts")
    public List<String> linkedAccounts(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária");
        }
        return oauthService.linkedProviders(authentication.getName());
    }

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }
}
