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
