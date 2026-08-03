package net.bitnp.guildofpioneers.repository;

import net.bitnp.guildofpioneers.entity.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link UserDepartment} persistence.
 */
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
}
