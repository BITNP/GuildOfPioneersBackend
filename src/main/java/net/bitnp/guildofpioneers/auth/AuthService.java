package net.bitnp.guildofpioneers.auth;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketService;
import net.bitnp.guildofpioneers.user.AuthResponse;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationTicketService registrationTicketService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationTicketService registrationTicketService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationTicketService = registrationTicketService;
    }

    /**
     * Registers a new user after validating the provided registration ticket.
     *
     * @param request the validated registration data
     * @return the created user profile
     * @throws TicketExpiredException       if the ticket has expired
     * @throws TicketNotFoundException      if the ticket does not exist
     * @throws PhoneAlreadyExistsException  if the phone number is already registered
     * @throws UserNameAlreadyExistsException if the username is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        registrationTicketService.validate(request.getTicketCode());
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new PhoneAlreadyExistsException(request.getPhone());
        }
        if (userRepository.existsByUserNameIgnoreCase(request.getUserName())) {
            throw new UserNameAlreadyExistsException(request.getUserName());
        }
        User user = User.builder()
                .userName(request.getUserName())
                .phone(request.getPhone())
                .email(blankToNull(request.getEmail()))
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        User saved = userRepository.save(user);
        log.trace("User with phone {} registered as id {}", request.getPhone(), saved.getId());
        return toResponse(saved);
    }

    private AuthResponse toResponse(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
