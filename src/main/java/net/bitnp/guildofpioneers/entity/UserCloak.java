package net.bitnp.guildofpioneers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the cloak identifier assigned to a user.
 */
@Entity
@Table(name = "user_cloaks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCloak {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "cloak_id")
    private String cloakId;
}
