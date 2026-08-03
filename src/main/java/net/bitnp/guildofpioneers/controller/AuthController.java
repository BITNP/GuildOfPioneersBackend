package net.bitnp.guildofpioneers.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import net.bitnp.guildofpioneers.dto.request.LoginRequest;
import net.bitnp.guildofpioneers.dto.request.RegisterRequest;
import net.bitnp.guildofpioneers.dto.response.AuthResponse;
import net.bitnp.guildofpioneers.service.AuthService;
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
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Authenticates a user and stores the session.
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
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return authService.getCurrentUser(authentication);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication);
    }

    @PutMapping("/avatar")
    public AuthResponse uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return authService.updateAvatar(authentication, file);
    }
}
