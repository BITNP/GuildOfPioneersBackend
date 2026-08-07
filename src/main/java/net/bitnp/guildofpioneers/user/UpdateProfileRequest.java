package net.bitnp.guildofpioneers.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for updating the current user's profile.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "must be a valid Chinese mobile number")
    private String phone;

    @Email(message = "must be a valid email address")
    private String email;
}
