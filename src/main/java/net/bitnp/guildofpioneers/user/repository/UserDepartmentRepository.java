package net.bitnp.guildofpioneers.user.repository;

import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link UserDepartment} persistence.
 */
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
}
