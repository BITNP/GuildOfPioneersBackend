package net.bitnp.guildofpioneers.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for viewing user profiles.
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
}
