package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.dto.request.RegisterRequest;
import net.bitnp.guildofpioneers.dto.response.AuthResponse;
import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        registrationTicketService.validate(request.getTicketCode());
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new PhoneAlreadyExistsException(request.getPhone());
        }
        User user = User.builder()
                .userName(request.getUserName())
                .avatar(request.getAvatar())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        return toResponse(userRepository.save(user));
    }

    public AuthResponse getCurrentUser(Authentication authentication) {
        User user = userRepository.findByPhone(authentication.getName()).orElseThrow();
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return AuthResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }
}
