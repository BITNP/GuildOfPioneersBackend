package net.bitnp.guildofpioneers.auth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/** Discovers the provider on demand so Keycloak outages do not prevent server startup. */
public final class LazyOidcClientRegistrationRepository implements ClientRegistrationRepository {
    private final OidcProperties properties;
    private volatile ClientRegistration registration;

    public LazyOidcClientRegistrationRepository(OidcProperties properties) { this.properties = properties; }

    /** Caches successful discovery only; subsequent login attempts can retry failures. */
    @Override
    public synchronized ClientRegistration findByRegistrationId(String registrationId) {
        if (!"keycloak".equals(registrationId)) return null;
        if (registration == null) {
            try {
                registration = ClientRegistrations.fromIssuerLocation(properties.issuer())
                        .registrationId("keycloak")
                        .clientId(properties.clientId())
                        .clientSecret(properties.clientSecret())
                        .scope("openid", "profile", "email")
                        .redirectUri(properties.publicUrl() + "/api/auth/oidc/callback/keycloak")
                        .clientName("BITNP")
                        .build();
            } catch (IllegalArgumentException | org.springframework.web.client.RestClientException ex) {
                throw new OAuth2AuthenticationException(new OAuth2Error("provider_unavailable"),
                        "Identity provider is temporarily unavailable", ex);
            }
        }
        return registration;
    }
}
