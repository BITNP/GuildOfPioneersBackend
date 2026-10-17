package net.bitnp.guildofpioneers.auth;

import net.bitnp.guildofpioneers.auth.dto.AuthStatusResponse;

import net.bitnp.guildofpioneers.auth.dto.RegisterRequest;

import net.bitnp.guildofpioneers.auth.service.OidcIdentityService;

import net.bitnp.guildofpioneers.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import net.bitnp.guildofpioneers.user.AuthResponse;
import net.bitnp.guildofpioneers.user.UpdateProfileRequest;
import net.bitnp.guildofpioneers.user.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** OIDC login, admission and site-owned profile endpoints; never exposes provider tokens. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final ClientRegistrationRepository registrations;
    private final SecurityContextRepository contexts;

    public AuthController(AuthService authService, UserService userService,
                          ClientRegistrationRepository registrations, SecurityContextRepository contexts) {
        this.authService = authService;
        this.userService = userService;
        this.registrations = registrations;
        this.contexts = contexts;
    }

    /** Starts login with a site-relative return path and a browser-bound authorization request. */
    @GetMapping("/login")
    public void login(@RequestParam(defaultValue = "/") String returnTo,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            registrations.findByRegistrationId("keycloak");
        } catch (OAuth2AuthenticationException ex) {
            response.sendRedirect("/login?error=provider_unavailable");
            return;
        }
        request.getSession().setAttribute(LoginRedirects.SESSION_ATTRIBUTE, LoginRedirects.safePath(returnTo));
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect("/api/auth/oidc/authorize/keycloak");
    }

    /** Reports admission separately from authentication, with suggestions only for the current identity. */
    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> status(Authentication authentication) {
        AuthStatusResponse result;
        if (authentication == null || !(authentication.getPrincipal() instanceof GuildOidcUser principal)) {
            result = new AuthStatusResponse("anonymous", null, null, false);
        } else if (principal.getUserId() == null) {
            String name = principal.getPreferredUsername();
            if (name == null || name.isBlank()) name = principal.getFullName();
            String email = Boolean.TRUE.equals(principal.getEmailVerified()) ? principal.getEmail() : null;
            result = new AuthStatusResponse("onboarding_required", null,
                    new AuthStatusResponse.ProfileSuggestions(name, email), authService.isBootstrapAdmin(principal));
        } else {
            result = new AuthStatusResponse("active", userService.getCurrentUser(authentication), null, false);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    /** Materializes the current session's masked CSRF token for same-origin write requests. */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }

    /** Completes admission, then replaces the session principal with the stable site identity. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, Authentication authentication,
                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        GuildOidcUser principal = OidcIdentityService.principal(authentication);
        Long id = authService.register(request, principal);
        GuildOidcUser admitted = principal.withUserId(id);
        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(admitted, admitted.getAuthorities(), "keycloak");
        httpRequest.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, httpRequest, httpResponse);
        return userService.getCurrentUser(token);
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) { return userService.getCurrentUser(authentication); }

    @PutMapping("/avatar")
    public AuthResponse uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return userService.updateAvatar(authentication, file);
    }

    /** Updates site-owned contact fields without changing the verified identity or username. */
    @PutMapping("/profile")
    public AuthResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
        return userService.updateProfile(authentication, request);
    }
}
