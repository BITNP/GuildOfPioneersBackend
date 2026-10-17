package net.bitnp.guildofpioneers.auth;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Keeps verified OIDC identity separate from optional site membership. */
public final class GuildOidcUser implements OidcUser, Serializable {
    private static final long serialVersionUID = 1L;
    private final OidcUser delegate;
    private final Long userId;

    public GuildOidcUser(OidcUser delegate, Long userId) {
        this.delegate = delegate;
        this.userId = userId;
    }

    public Long getUserId() { return userId; }

    /** Returns a new principal after admission without changing the verified identity. */
    public GuildOidcUser withUserId(Long id) { return new GuildOidcUser(delegate, id); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userId == null ? "ROLE_ONBOARDING" : "ROLE_MEMBER"));
    }

    @Override public Map<String, Object> getClaims() { return delegate.getClaims(); }
    @Override public OidcUserInfo getUserInfo() { return delegate.getUserInfo(); }
    @Override public OidcIdToken getIdToken() { return delegate.getIdToken(); }
    @Override public Map<String, Object> getAttributes() { return delegate.getAttributes(); }
    @Override public String getName() { return delegate.getSubject(); }
}
