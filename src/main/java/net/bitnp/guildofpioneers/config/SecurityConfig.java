package net.bitnp.guildofpioneers.config;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configures Spring Security beans and the HTTP security filter chain.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Builds the security filter chain, permitting login, registration, and static uploads
     * while requiring authentication for all other requests.
     *
     * @param http         the HTTP security builder
     * @param objectMapper the Jackson mapper used for the unauthorized entry point body
     * @return the configured security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint unauthorizedEntryPoint = (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 401);
            body.put("error", "UNAUTHORIZED");
            body.put("message", "Authentication required");
            body.put("path", request.getRequestURI());
            body.put("timestamp", Instant.now().toString());
            objectMapper.writeValue(response.getWriter(), body);
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/uploads/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
        return http.build();
    }
}
