package net.bitnp.guildofpioneers.user;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints for viewing and editing user profiles. A user may edit their own
 * profile, and admins may edit any user's profile.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the profile of a user by id.
     *
     * @param id the user's id
     * @return the user's profile
     */
    @GetMapping("/{id}")
    public AuthResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    /**
     * Updates the phone and email of the user with the given id. The user themselves
     * may edit their own profile; admins may edit any user's.
     *
     * @param id             the target user's id
     * @param request        the validated profile data
     * @param authentication the current authentication
     * @return the updated target profile
     */
    @PutMapping("/{id}")
    public AuthResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        return userService.updateProfile(id, request, authentication);
    }

    /**
     * Replaces the avatar of the user with the given id. The user themselves may
     * update their own avatar; admins may edit any user's.
     *
     * @param id             the target user's id
     * @param file           the new avatar image
     * @param authentication the current authentication
     * @return the updated target profile
     */
    @PutMapping("/{id}/avatar")
    public AuthResponse updateUserAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return userService.updateAvatar(id, file, authentication);
    }
}
