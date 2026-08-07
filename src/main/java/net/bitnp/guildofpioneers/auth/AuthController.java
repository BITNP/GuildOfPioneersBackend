package net.bitnp.guildofpioneers.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.bitnp.guildofpioneers.user.AuthResponse;
import net.bitnp.guildofpioneers.user.UpdateProfileRequest;
import net.bitnp.guildofpioneers.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints for registration, login, profile retrieval, and avatar upload.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int REMEMBER_ME_MAX_AGE_SECONDS = 30 * 24 * 60 * 60;
    private static final int REMEMBER_ME_SESSION_TIMEOUT_SECONDS = REMEMBER_ME_MAX_AGE_SECONDS;

    private final AuthService authService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            UserService userService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authService = authService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Authenticates a user and stores the session. When {@code rememberMe} is set,
     * the session and its cookie are extended to 30 days so that the user stays
     * logged in across browser restarts.
     *
     * @param request      the login credentials
     * @param httpRequest  the servlet request used for session storage
     * @param httpResponse the servlet response used for session storage
     * @return the authenticated user profile
     */
    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            HttpSession session = httpRequest.getSession();
            session.setMaxInactiveInterval(REMEMBER_ME_SESSION_TIMEOUT_SECONDS);
            httpResponse.addCookie(buildSessionCookie(session));
        }
        log.trace("User {} logged in", request.getUsername());
        return userService.getCurrentUser(authentication);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        return userService.getCurrentUser(authentication);
    }

    @PutMapping("/avatar")
    public AuthResponse uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return userService.updateAvatar(authentication, file);
    }

    /**
     * Updates the authenticated user's phone and email. The username is not editable.
     *
     * @param request        the validated profile data
     * @param authentication the current authentication
     * @return the updated user profile
     */
    @PutMapping("/profile")
    public AuthResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        log.trace("User {} updated their profile", authentication.getName());
        return userService.updateProfile(authentication, request);
    }

    /**
     * Builds a persistent copy of the session cookie so that the session
     * survives a browser restart.
     *
     * @param session the session to remember
     * @return the cookie to write on the response
     */
    private Cookie buildSessionCookie(HttpSession session) {
        var cookieConfig = session.getServletContext().getSessionCookieConfig();
        String name = cookieConfig.getName();
        if (name == null || name.isEmpty()) {
            name = "JSESSIONID";
        }
        Cookie cookie = new Cookie(name, session.getId());
        cookie.setPath(cookieConfig.getPath() != null ? cookieConfig.getPath() : "/");
        cookie.setMaxAge(REMEMBER_ME_MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieConfig.isSecure());
        return cookie;
    }
}
