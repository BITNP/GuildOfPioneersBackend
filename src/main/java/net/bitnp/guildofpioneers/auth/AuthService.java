package net.bitnp.guildofpioneers.auth;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketService;
import net.bitnp.guildofpioneers.user.AuthResponse;
import net.bitnp.guildofpioneers.user.UserDepartmentDto;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles user registration.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationTicketService registrationTicketService;
    private final UserDepartmentRepository userDepartmentRepository;
    private final PermissionService permissionService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationTicketService registrationTicketService,
            UserDepartmentRepository userDepartmentRepository,
            PermissionService permissionService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationTicketService = registrationTicketService;
        this.userDepartmentRepository = userDepartmentRepository;
        this.permissionService = permissionService;
    }

    /**
     * Registers a new user after validating the provided registration ticket.
     * The new user is granted the department and role invited by the ticket.
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
        RegistrationTicket ticket = registrationTicketService.findByCode(request.getTicketCode());
        User user = User.builder()
                .userName(request.getUserName())
                .phone(request.getPhone())
                .email(blankToNull(request.getEmail()))
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        User saved = userRepository.save(user);
        userDepartmentRepository.save(UserDepartment.builder()
                .userId(saved.getId())
                .department(ticket.getDepartment())
                .role(ticket.getRole())
                .build());
        log.trace("User with phone {} registered as id {} in {} as {}",
                request.getPhone(), saved.getId(), ticket.getDepartment(), ticket.getRole());
        return toResponse(saved);
    }

    private AuthResponse toResponse(User user) {
        List<UserDepartmentDto> departments = userDepartmentRepository.findByUserId(user.getId())
                .stream()
                .map(department -> UserDepartmentDto.builder()
                        .department(department.getDepartment())
                        .role(department.getRole())
                        .build())
                .toList();
        return AuthResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .departments(departments)
                .isManager(permissionService.isManager(user))
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
