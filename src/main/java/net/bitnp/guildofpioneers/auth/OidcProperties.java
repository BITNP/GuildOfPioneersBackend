package net.bitnp.guildofpioneers.auth;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Deployment-owned OIDC settings; secrets are never included in diagnostics. */
@ConfigurationProperties("app.oidc")
public record OidcProperties(String issuer, String clientId, String clientSecret,
                             String publicUrl, String bootstrapAdminSubject) {
    /** Validates required settings without allowing callback credentials or URL parameters. */
    public OidcProperties {
        requireValue(issuer, "OIDC issuer");
        requireValue(clientId, "OIDC client ID");
        requireValue(clientSecret, "OIDC client secret");
        requireValue(publicUrl, "Public URL");
        validateHttpUrl(issuer, "OIDC issuer");
        URI site = validateHttpUrl(publicUrl, "Public URL");
        if (site.getRawPath() != null && !site.getRawPath().isEmpty() && !site.getRawPath().equals("/")) {
            throw new IllegalArgumentException("Public URL must be an origin without a path");
        }
        publicUrl = publicUrl.replaceAll("/$", "");
        bootstrapAdminSubject = bootstrapAdminSubject == null ? "" : bootstrapAdminSubject;
    }

    @Override
    public String toString() {
        return "OidcProperties[issuer=" + issuer + ", clientId=" + clientId
                + ", clientSecret=<redacted>, publicUrl=" + publicUrl + "]";
    }

    private static void requireValue(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must be configured");
    }

    private static URI validateHttpUrl(String value, String label) {
        URI uri = URI.create(value);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(label + " must be an HTTP(S) URL without credentials, query or fragment");
        }
        return uri;
    }
}
