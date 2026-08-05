package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.dto.request.RegisterRequest;
import net.bitnp.guildofpioneers.dto.response.AuthResponse;
import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.entity.UserDepartment;
import net.bitnp.guildofpioneers.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles user registration, profile retrieval, and avatar updates.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationTicketService registrationTicketService;
    private final FileStorageService fileStorageService;
    private final UserDepartmentRepository userDepartmentRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationTicketService registrationTicketService,
            FileStorageService fileStorageService,
            UserDepartmentRepository userDepartmentRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationTicketService = registrationTicketService;
        this.fileStorageService = fileStorageService;
        this.userDepartmentRepository = userDepartmentRepository;
    }

    /**
     * Registers a new user after validating the provided registration ticket.
     *
     * @param request the validated registration data
     * @return the created user
     * @throws TicketExpiredException   if the ticket has expired
     * @throws TicketNotFoundException  if the ticket does not exist
     * @throws PhoneAlreadyExistsException if the phone number is already registered
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
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        User saved = userRepository.save(user);
        log.trace("User with phone {} registered as id {}", request.getPhone(), saved.getId());
        return toResponse(saved);
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param authentication the current authentication
     * @return the current user's profile
     */
    public AuthResponse getCurrentUser(Authentication authentication) {
        User user = findByAuthentication(authentication);
        String department = userDepartmentRepository.findById(user.getId())
                .map(UserDepartment::getDepartment)
                .orElse(null);
        AuthResponse response = toResponse(user);
        response.setDepartment(department);
        return response;
    }

    /**
     * Replaces the authenticated user's avatar with the uploaded file.
     *
     * @param authentication the current authentication
     * @param file           the new avatar image
     * @return the updated user profile
     */
    @Transactional
    public AuthResponse updateAvatar(Authentication authentication, MultipartFile file) {
        User user = findByAuthentication(authentication);
        String storedPath = fileStorageService.storeAvatar(file, user.getId());
        fileStorageService.deleteAvatar(user.getAvatar());
        user.setAvatar(storedPath);
        AuthResponse response = toResponse(userRepository.save(user));
        log.trace("User {} updated their avatar", user.getId());
        return response;
    }

    private User findByAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUserNameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private AuthResponse toResponse(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .avatar(withVersion(user.getAvatar()))
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private String withVersion(String avatar) {
        Long version = fileStorageService.getVersion(avatar);
        return version != null ? avatar + "?v=" + version : avatar;
    }
}
