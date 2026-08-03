package net.bitnp.guildofpioneers.repository;

import net.bitnp.guildofpioneers.entity.UserStudent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link UserStudent} persistence.
 */
public interface UserStudentRepository extends JpaRepository<UserStudent, Long> {
}
