package net.bitnp.guildofpioneers.user;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.storage.FileStorageService;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles profile retrieval, profile updates, and avatar management for users.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final FileStorageService fileStorageService;

    public UserService(
            UserRepository userRepository,
            UserDepartmentRepository userDepartmentRepository,
            FileStorageService fileStorageService
    ) {
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.fileStorageService = fileStorageService;
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
    public AuthResponse updateAvatar(Authentication authentication, MultipartFile file) {
        User user = findByAuthentication(authentication);
        fileStorageService.storeAvatar(file, user.getId());
        log.trace("User {} updated their avatar", user.getId());
        return toResponse(user);
    }

    /**
     * Updates the authenticated user's phone and email. The username is not editable.
     * A phone already owned by the user is allowed unchanged; only a genuinely new
     * value is checked for uniqueness.
     *
     * @param authentication the current authentication
     * @param request        the validated profile data
     * @return the updated user profile
     * @throws PhoneAlreadyExistsException if the new phone belongs to another user
     */
    @Transactional
    public AuthResponse updateProfile(Authentication authentication, UpdateProfileRequest request) {
        User user = findByAuthentication(authentication);
        if (!request.getPhone().equals(user.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new PhoneAlreadyExistsException(request.getPhone());
        }
        user.setPhone(request.getPhone());
        user.setEmail(blankToNull(request.getEmail()));
        log.trace("User {} updated their profile", user.getId());
        return toResponse(userRepository.save(user));
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
                .avatar(fileStorageService.avatarUrl(user.getId()))
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
