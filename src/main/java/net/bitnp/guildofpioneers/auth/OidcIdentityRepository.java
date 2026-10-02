package net.bitnp.guildofpioneers.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores explicit identity bindings without matching mutable profile claims. */
public interface OidcIdentityRepository extends JpaRepository<OidcIdentity, Long> {
    Optional<OidcIdentity> findByIssuerAndSubject(String issuer, String subject);
}
