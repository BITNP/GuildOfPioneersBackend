package net.bitnp.guildofpioneers.controller;

import net.bitnp.guildofpioneers.config.SecurityConfig;
import net.bitnp.guildofpioneers.dto.response.AuthResponse;
import net.bitnp.guildofpioneers.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AuthController}.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser(username = "13800000000")
    void uploadAvatar_returnsUpdatedUser() throws Exception {
        when(authService.updateAvatar(any(Authentication.class), any()))
                .thenReturn(AuthResponse.builder()
                        .id(42L)
                        .userName("Alice")
                        .avatar("/uploads/avatars/42.png?v=1720000000000")
                        .phone("13800000000")
                        .build());
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
        );

        mockMvc.perform(multipart("/api/auth/avatar").file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.avatar").value("/uploads/avatars/42.png?v=1720000000000"));
    }

    @Test
    void uploadAvatar_requiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1}
        );

        mockMvc.perform(multipart("/api/auth/avatar").file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "Alice")
    void updateProfile_returnsUpdatedUser() throws Exception {
        when(authService.updateProfile(any(Authentication.class), any()))
                .thenReturn(AuthResponse.builder()
                        .id(7L)
                        .userName("Alice")
                        .phone("13900000000")
                        .email("new@example.com")
                        .build());

        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13900000000",
                                  "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.userName").value("Alice"))
                .andExpect(jsonPath("$.phone").value("13900000000"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void updateProfile_requiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800000000"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_rejectsInvalidPhone() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "12345",
                                  "password": "password123",
                                  "userName": "Alice",
                                  "ticketCode": "VALIDCODE123",
                                  "email": "alice@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("phone must be a valid Chinese mobile number"));
        verify(authService, never()).register(any());
    }

    @Test
    void register_rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800000000",
                                  "password": "password123",
                                  "userName": "Alice",
                                  "ticketCode": "VALIDCODE123",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email must be a valid email address"));
        verify(authService, never()).register(any());
    }

    @Test
    @WithMockUser(username = "Alice")
    void updateProfile_rejectsInvalidPhone() throws Exception {
        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "12345",
                                  "email": "alice@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("phone must be a valid Chinese mobile number"));
        verify(authService, never()).updateProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "Alice")
    void updateProfile_rejectsInvalidEmail() throws Exception {
        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800000000",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email must be a valid email address"));
        verify(authService, never()).updateProfile(any(), any());
    }
}
