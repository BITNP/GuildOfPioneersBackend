package net.bitnp.guildofpioneers.user.repository;

import net.bitnp.guildofpioneers.user.entity.UserCloak;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link UserCloak} persistence.
 */
public interface UserCloakRepository extends JpaRepository<UserCloak, Long> {
}
