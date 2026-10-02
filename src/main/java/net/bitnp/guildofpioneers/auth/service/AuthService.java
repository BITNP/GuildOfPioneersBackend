package net.bitnp.guildofpioneers.auth.service;

import net.bitnp.guildofpioneers.auth.dto.RegisterRequest;

import net.bitnp.guildofpioneers.auth.GuildOidcUser;
import net.bitnp.guildofpioneers.auth.IdentityConflictException;
import net.bitnp.guildofpioneers.auth.OidcIdentity;
import net.bitnp.guildofpioneers.auth.OidcIdentityRepository;
import net.bitnp.guildofpioneers.auth.OidcProperties;
import net.bitnp.guildofpioneers.ticket.InvalidTicketRequestException;
import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketService;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

/** Admits verified OIDC identities without trusting mutable claims or provider roles. */
@Service
public class AuthService {
    private final UserRepository users;
    private final RegistrationTicketService tickets;
    private final UserDepartmentRepository departments;
    private final OidcIdentityRepository identities;
    private final OidcProperties properties;

    public AuthService(UserRepository users, RegistrationTicketService tickets,
                       UserDepartmentRepository departments, OidcIdentityRepository identities,
                       OidcProperties properties) {
        this.users = users;
        this.tickets = tickets;
        this.departments = departments;
        this.identities = identities;
        this.properties = properties;
    }

    /** True only for the configured unbound identity while no site administrator exists. */
    @Transactional(readOnly = true)
    public boolean isBootstrapAdmin(GuildOidcUser principal) {
        return principal.getUserId() == null
                && properties.issuer().equals(principal.getIdToken().getIssuer().toString())
                && !properties.bootstrapAdminSubject().isBlank()
                && properties.bootstrapAdminSubject().equals(principal.getSubject())
                && !departments.existsByDepartment(Department.ADMIN);
    }

    /** Creates the user, invited membership and identity binding in one transaction. */
    @Transactional
    public Long register(RegisterRequest request, GuildOidcUser principal) {
        String issuer = principal.getIdToken().getIssuer().toString();
        String subject = principal.getSubject();
        if (!properties.issuer().equals(issuer) || subject == null || subject.isBlank()) {
            throw new BadCredentialsException("Invalid OIDC identity");
        }
        if (principal.getUserId() != null || identities.findByIssuerAndSubject(issuer, subject).isPresent()) {
            throw new IdentityConflictException("This identity already has a site account");
        }
        Department department;
        DepartmentRole role;
        if (isBootstrapAdmin(principal)) {
            department = Department.ADMIN;
            role = DepartmentRole.LEADER;
        } else {
            if (request.getTicketCode() == null || request.getTicketCode().isBlank()) {
                throw new InvalidTicketRequestException("Registration ticket code is required");
            }
            tickets.validate(request.getTicketCode());
            RegistrationTicket ticket = tickets.findByCode(request.getTicketCode());
            department = ticket.getDepartment();
            role = ticket.getRole();
        }
        if (users.existsByPhone(request.getPhone())) throw new PhoneAlreadyExistsException(request.getPhone());
        String userName = request.getUserName().trim();
        if (users.existsByUserNameIgnoreCase(userName)) throw new UserNameAlreadyExistsException(userName);
        User saved = users.saveAndFlush(User.builder().userName(userName).phone(request.getPhone())
                .email(blankToNull(request.getEmail())).build());
        departments.save(UserDepartment.builder().userId(saved.getId()).department(department).role(role).build());
        identities.saveAndFlush(new OidcIdentity(saved.getId(), issuer, subject));
        return saved.getId();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
