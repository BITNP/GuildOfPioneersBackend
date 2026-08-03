package net.bitnp.guildofpioneers.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for new user registration.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    private String phone;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String userName;

    @NotBlank
    private String ticketCode;

    @Email
    private String email;
}
