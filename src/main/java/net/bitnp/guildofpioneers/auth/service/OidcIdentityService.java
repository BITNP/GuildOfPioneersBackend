package net.bitnp.guildofpioneers.auth.service;

import net.bitnp.guildofpioneers.auth.GuildOidcUser;
import net.bitnp.guildofpioneers.auth.OidcIdentity;
import net.bitnp.guildofpioneers.auth.OidcIdentityRepository;
import net.bitnp.guildofpioneers.auth.OidcProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Resolves site membership only after Spring has verified the OIDC ID token. */
@Service
public class OidcIdentityService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OidcIdentityRepository repository;
    private final OidcProperties properties;
    private final OidcUserService delegate = new OidcUserService();

    public OidcIdentityService(OidcIdentityRepository repository, OidcProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Maps issuer/subject to a stable site ID; external roles and names grant no access. */
    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser verified = delegate.loadUser(request);
        String issuer = verified.getIdToken().getIssuer().toString();
        if (!properties.issuer().equals(issuer) || verified.getSubject() == null || verified.getSubject().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_identity"));
        }
        Long userId = repository.findByIssuerAndSubject(issuer, verified.getSubject())
                .map(OidcIdentity::getUserId).orElse(null);
        return new GuildOidcUser(verified, userId);
    }

    /** Returns the verified site principal, rejecting all other authentication mechanisms. */
    public static GuildOidcUser principal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof GuildOidcUser principal)) {
            throw new BadCredentialsException("OIDC authentication required");
        }
        return principal;
    }
}
