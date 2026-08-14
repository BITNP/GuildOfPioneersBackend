package net.bitnp.guildofpioneers.user;

import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.common.PermissionService;
import net.bitnp.guildofpioneers.storage.FileStorageService;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.PermissionDeniedException;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Handles profile retrieval, profile updates, and avatar management for users.
 * Profile and avatar editing is available to the user themselves or to admins,
 * who may edit any user.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final FileStorageService fileStorageService;
    private final PermissionService permissionService;

    public UserService(
            UserRepository userRepository,
            UserDepartmentRepository userDepartmentRepository,
            FileStorageService fileStorageService,
            PermissionService permissionService
    ) {
        this.userRepository = userRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.fileStorageService = fileStorageService;
        this.permissionService = permissionService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param authentication the current authentication
     * @return the current user's profile
     */
    public AuthResponse getCurrentUser(Authentication authentication) {
        User user = findByAuthentication(authentication);
        return toFullResponse(user);
    }

    /**
     * Returns the profile of a user by id.
     *
     * @param id the user's id
     * @return the user's profile
     * @throws UserNotFoundException if no user with the given id exists
     */
    @Transactional(readOnly = true)
    public AuthResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toFullResponse(user);
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
        return toFullResponse(user);
    }

    /**
     * Replaces the avatar of the user with the given id. The target user may edit
     * their own avatar, and admins may edit anyone's.
     *
     * @param userId         the target user's id
     * @param file           the new avatar image
     * @param authentication the current authentication
     * @return the updated target profile
     * @throws UserNotFoundException    if the target user does not exist
     * @throws PermissionDeniedException if the current user is neither the target nor an admin
     */
    public AuthResponse updateAvatar(Long userId, MultipartFile file, Authentication authentication) {
        User target = findUser(userId);
        requireSelfOrAdmin(target, authentication);
        fileStorageService.storeAvatar(file, target.getId());
        log.trace("User {} avatar updated by {}", target.getId(), authentication.getName());
        return toFullResponse(target);
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
        return applyProfileUpdate(user, request);
    }

    /**
     * Updates the phone and email of the user with the given id. The target user may
     * edit their own profile, and admins may edit anyone's.
     *
     * @param userId         the target user's id
     * @param request        the validated profile data
     * @param authentication the current authentication
     * @return the updated target profile
     * @throws UserNotFoundException     if the target user does not exist
     * @throws PermissionDeniedException if the current user is neither the target nor an admin
     * @throws PhoneAlreadyExistsException if the new phone belongs to another user
     */
    @Transactional
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request, Authentication authentication) {
        User target = findUser(userId);
        requireSelfOrAdmin(target, authentication);
        return applyProfileUpdate(target, request);
    }

    private void requireSelfOrAdmin(User target, Authentication authentication) {
        User current = permissionService.currentUser(authentication);
        if (!permissionService.isAdminOr(current, () -> target.getId().equals(current.getId()))) {
            throw new PermissionDeniedException("You are not allowed to edit this user's profile");
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private AuthResponse applyProfileUpdate(User user, UpdateProfileRequest request) {
        if (!request.getPhone().equals(user.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new PhoneAlreadyExistsException(request.getPhone());
        }
        user.setPhone(request.getPhone());
        user.setEmail(blankToNull(request.getEmail()));
        log.trace("Profile of user {} updated", user.getId());
        return toFullResponse(userRepository.save(user));
    }

    private AuthResponse toFullResponse(User user) {
        List<UserDepartmentDto> departments = userDepartmentRepository.findByUserId(user.getId())
                .stream()
                .map(department -> UserDepartmentDto.builder()
                        .department(department.getDepartment())
                        .role(department.getRole())
                        .build())
                .toList();
        AuthResponse response = toResponse(user);
        response.setDepartments(departments);
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
                .avatar(fileStorageService.avatarUrl(user.getId()))
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
