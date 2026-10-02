package net.bitnp.guildofpioneers.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Associates one verified external identity with one stable site user ID. */
@Entity
@Table(name = "user_oidc_identities")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OidcIdentity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 1024)
    private String issuer;

    @Column(nullable = false)
    private String subject;
}
