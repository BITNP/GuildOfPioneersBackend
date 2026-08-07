package net.bitnp.guildofpioneers.user.repository;

import net.bitnp.guildofpioneers.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for {@link User} persistence and lookups.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhone(String phone);

    Optional<User> findByPhone(String phone);

    boolean existsByUserNameIgnoreCase(String userName);

    Optional<User> findByUserNameIgnoreCase(String userName);
}
