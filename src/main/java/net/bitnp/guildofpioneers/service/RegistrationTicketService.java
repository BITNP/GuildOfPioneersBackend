package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.dto.request.CreateRegistrationTicketRequest;
import net.bitnp.guildofpioneers.dto.response.RegistrationTicketResponse;
import net.bitnp.guildofpioneers.entity.RegistrationTicket;
import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.exception.TicketExpiredException;
import net.bitnp.guildofpioneers.exception.TicketNotFoundException;
import net.bitnp.guildofpioneers.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.repository.RegistrationTicketRepository;
import net.bitnp.guildofpioneers.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Manages the lifecycle of registration tickets, including creation and validation.
 */
@Service
public class RegistrationTicketService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12;
    private static final int CODE_GENERATION_ATTEMPTS = 5;

    private final RegistrationTicketRepository registrationTicketRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegistrationTicketService(
            RegistrationTicketRepository registrationTicketRepository,
            UserRepository userRepository
    ) {
        this.registrationTicketRepository = registrationTicketRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new registration ticket for the given creator.
     *
     * @param request      the ticket creation data
     * @param creatorPhone the phone number of the user creating the ticket
     * @return the created ticket
     * @throws UserNotFoundException if the creator does not exist
     */
    @Transactional
    public RegistrationTicketResponse create(CreateRegistrationTicketRequest request, String creatorPhone) {
        User creator = userRepository.findByPhone(creatorPhone)
                .orElseThrow(() -> new UserNotFoundException(creatorPhone));
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code(generateUniqueCode())
                .createdAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .createdBy(creator.getId())
                .build();
        return toResponse(registrationTicketRepository.save(ticket));
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
                .build();
    }
}
