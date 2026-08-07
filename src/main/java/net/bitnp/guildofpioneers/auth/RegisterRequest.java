package net.bitnp.guildofpioneers.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "must be a valid Chinese mobile number")
    private String phone;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String userName;

    @NotBlank
    private String ticketCode;

    @Email(message = "must be a valid email address")
    private String email;
}
