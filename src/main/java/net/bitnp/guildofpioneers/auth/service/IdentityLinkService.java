package net.bitnp.guildofpioneers.auth.service;

import net.bitnp.guildofpioneers.auth.dto.IdentityLinkRequest;

import net.bitnp.guildofpioneers.auth.IdentityConflictException;
import net.bitnp.guildofpioneers.auth.OidcIdentity;
import net.bitnp.guildofpioneers.auth.OidcIdentityRepository;
import jakarta.validation.Validator;
import java.util.List;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports operator-verified bindings atomically and never replaces existing associations. */
@Service
public class IdentityLinkService {
    private final OidcIdentityRepository identities;
    private final UserRepository users;
    private final Validator validator;

    public IdentityLinkService(OidcIdentityRepository identities, UserRepository users, Validator validator) {
        this.identities = identities;
        this.users = users;
        this.validator = validator;
    }

    /** Validates and imports an entire batch; any conflict rolls back all new bindings. */
    @Transactional
    public int bind(List<IdentityLinkRequest> requests, String configuredIssuer) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("Binding file must not be empty");
        int added = 0;
        for (IdentityLinkRequest request : requests) {
            if (request == null || !validator.validate(request).isEmpty()
                    || !request.issuer().equals(configuredIssuer)) {
                throw new IllegalArgumentException("Invalid binding or issuer does not match this deployment");
            }
            if (!users.existsById(request.userId())) throw new IllegalArgumentException("Binding references a missing site user");
            var existingUser = identities.findById(request.userId());
            var existingIdentity = identities.findByIssuerAndSubject(request.issuer(), request.subject());
            if (existingUser.isPresent()) {
                OidcIdentity existing = existingUser.get();
                if (!existing.getIssuer().equals(request.issuer()) || !existing.getSubject().equals(request.subject())) {
                    throw new IdentityConflictException("Site user is already bound to a different identity");
                }
                continue;
            }
            if (existingIdentity.isPresent()) throw new IdentityConflictException("Identity is already bound to a different site user");
            identities.saveAndFlush(new OidcIdentity(request.userId(), request.issuer(), request.subject()));
            added++;
        }
        return added;
    }
}
