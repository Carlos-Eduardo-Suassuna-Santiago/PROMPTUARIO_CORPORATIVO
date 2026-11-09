package br.edu.ifrn.promptuario.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2.google")
public class OAuthConfig {

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public boolean isConfigured() {
        return isUsable(clientId) && isUsable(clientSecret) && isUsable(redirectUri);
    }

    private boolean isUsable(String value) {
        return value != null && !value.isBlank() && !"not-configured".equals(value);
    }
}
