package net.bitnp.guildofpioneers.repository;

import net.bitnp.guildofpioneers.entity.UserCloak;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link UserCloak} persistence.
 */
public interface UserCloakRepository extends JpaRepository<UserCloak, Long> {
}
