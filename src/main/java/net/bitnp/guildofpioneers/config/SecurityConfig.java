package net.bitnp.guildofpioneers.config;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import net.bitnp.guildofpioneers.auth.GuildOidcUser;
import net.bitnp.guildofpioneers.auth.LazyOidcClientRegistrationRepository;
import net.bitnp.guildofpioneers.auth.LoginRedirects;
import net.bitnp.guildofpioneers.auth.service.OidcIdentityService;
import net.bitnp.guildofpioneers.auth.OidcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.ObjectMapper;

/** OIDC-only authentication, browser session protection and explicit membership gates. */
@Configuration
@EnableConfigurationProperties(OidcProperties.class)
public class SecurityConfig {
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OidcProperties properties) {
        return new LazyOidcClientRegistrationRepository(properties);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    /** Protects every business request with site membership; pending identities get admission endpoints only. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,
            ClientRegistrationRepository registrations, OidcIdentityService identities,
            SecurityContextRepository contexts) throws Exception {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/api/auth/oidc/authorize");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        http
                .securityContext(context -> context.securityContextRepository(contexts))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/login", "/api/auth/status", "/api/auth/csrf",
                                "/api/auth/oidc/authorize/**", "/api/auth/oidc/callback/**",
                                "/api/tickets/**", "/uploads/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ONBOARDING")
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .anyRequest().hasRole("MEMBER"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, error) ->
                                writeError(mapper, response, 401, "UNAUTHORIZED", "OIDC authentication required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, error) -> {
                            boolean csrf = error instanceof CsrfException;
                            writeError(mapper, response, 403, csrf ? "CSRF_INVALID" : "FORBIDDEN",
                                    csrf ? "Missing or invalid CSRF token" : "Site admission required or permission denied",
                                    request.getRequestURI());
                        }))
                .oauth2Login(login -> login
                        .loginPage("/api/auth/login")
                        .authorizedClientRepository(new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository())
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/oidc/callback/*"))
                        .userInfoEndpoint(endpoint -> endpoint.oidcUserService(identities))
                        .successHandler((request, response, authentication) -> {
                            var session = request.getSession(false);
                            String returnTo = session == null ? "/" : (String) session.getAttribute(LoginRedirects.SESSION_ATTRIBUTE);
                            if (session != null) session.removeAttribute(LoginRedirects.SESSION_ATTRIBUTE);
                            GuildOidcUser principal = OidcIdentityService.principal(authentication);
                            response.setHeader("Cache-Control", "no-store");
                            response.setHeader("Referrer-Policy", "no-referrer");
                            String target = LoginRedirects.safePath(returnTo);
                            if (principal.getUserId() == null) {
                                target = "/register?redirect=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8);
                            }
                            response.sendRedirect(target);
                        })
                        .failureHandler((request, response, error) -> {
                            response.setHeader("Cache-Control", "no-store");
                            response.setHeader("Referrer-Policy", "no-referrer");
                            response.sendRedirect("/login?error=oidc_failed");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));
        // Default session-backed CSRF repository and XOR request handler remain enabled.
        return http.build();
    }

    private static void writeError(ObjectMapper mapper, HttpServletResponse response, int status,
                                   String error, String message, String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), Map.of("status", status, "error", error,
                "message", message, "path", path, "timestamp", Instant.now().toString()));
    }
}
