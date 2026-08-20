package net.bitnp.guildofpioneers.ticket;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.PermissionDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Manages the lifecycle of registration tickets, including creation and validation.
 */
@Slf4j
@Service
public class RegistrationTicketService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12;
    private static final int CODE_GENERATION_ATTEMPTS = 5;

    private final RegistrationTicketRepository registrationTicketRepository;
    private final PermissionService permissionService;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegistrationTicketService(
            RegistrationTicketRepository registrationTicketRepository,
            PermissionService permissionService
    ) {
        this.registrationTicketRepository = registrationTicketRepository;
        this.permissionService = permissionService;
    }

    /**
     * Creates a new registration ticket for the given creator.
     *
     * @param request        the ticket creation data
     * @param authentication the current authentication
     * @return the created ticket
     * @throws PermissionDeniedException     if the current user is not an admin or presidium member
     * @throws InvalidTicketRequestException if the requested department is the ADMIN department
     */
    @Transactional
    public RegistrationTicketResponse create(CreateRegistrationTicketRequest request, Authentication authentication) {
        User creator = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOrPresidium(creator)) {
            throw new PermissionDeniedException("Only admins or presidium members can create registration tickets");
        }
        if (request.getDepartment() == Department.ADMIN) {
            throw new InvalidTicketRequestException("Registration tickets cannot be issued for the ADMIN department");
        }
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code(generateUniqueCode())
                .createdAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .createdBy(creator.getId())
                .department(request.getDepartment())
                .role(request.getRole())
                .build();
        RegistrationTicket saved = registrationTicketRepository.save(ticket);
        log.trace("Registration ticket {} created by user {}", saved.getId(), saved.getCreatedBy());
        return toResponse(saved);
    }

    /**
     * Validates that a registration ticket exists and has not expired.
     *
     * @param code the ticket code to validate
     * @throws TicketNotFoundException if the ticket does not exist
     * @throws TicketExpiredException  if the ticket has expired
     */
    @Transactional(readOnly = true)
    public void validate(String code) {
        RegistrationTicket ticket = registrationTicketRepository.findByCode(code)
                .orElseThrow(() -> new TicketNotFoundException(code));
        if (ticket.getExpiresAt().isBefore(Instant.now())) {
            throw new TicketExpiredException(code);
        }
    }

    /**
     * Looks up a ticket by code and reports whether it is still valid, along with
     * the department and role the holder is invited into.
     *
     * @param code the ticket code to look up
     * @return the validity status, expiration state, and invited department/role
     * @throws TicketCodeNotFoundException if no ticket with the code exists
     */
    @Transactional(readOnly = true)
    public TicketValidationResponse validateTicket(String code) {
        RegistrationTicket ticket = registrationTicketRepository.findByCode(code)
                .orElseThrow(() -> new TicketCodeNotFoundException(code));
        boolean expired = ticket.getExpiresAt().isBefore(Instant.now());
        return TicketValidationResponse.builder()
                .valid(!expired)
                .expired(expired)
                .department(ticket.getDepartment())
                .role(ticket.getRole())
                .expiresAt(ticket.getExpiresAt())
                .build();
    }

    /**
     * Returns the ticket with the given code so callers can read the department
     * and role the holder is invited into. The caller is expected to have
     * validated the ticket first.
     *
     * @param code the ticket code to look up
     * @return the matching ticket
     * @throws TicketNotFoundException if no ticket with the code exists
     */
    @Transactional(readOnly = true)
    public RegistrationTicket findByCode(String code) {
        return registrationTicketRepository.findByCode(code)
                .orElseThrow(() -> new TicketNotFoundException(code));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = generateCode();
            if (!registrationTicketRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique registration ticket code");
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private RegistrationTicketResponse toResponse(RegistrationTicket ticket) {
        return RegistrationTicketResponse.builder()
                .id(ticket.getId())
                .code(ticket.getCode())
                .createdAt(ticket.getCreatedAt())
                .expiresAt(ticket.getExpiresAt())
                .createdBy(ticket.getCreatedBy())
                .department(ticket.getDepartment())
                .role(ticket.getRole())
                .build();
    }
}
