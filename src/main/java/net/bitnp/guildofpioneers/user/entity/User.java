package net.bitnp.guildofpioneers.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a registered user of the platform.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "avatar")
    private String avatar;

    @NotBlank
    @Column(name = "phone", nullable = false)
    private String phone;

    @Email
    @Column(name = "email")
    private String email;

    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;
}
