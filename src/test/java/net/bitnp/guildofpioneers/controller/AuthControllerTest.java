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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
