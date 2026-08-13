package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoProjectMember;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link TodoProjectMember} persistence.
 */
public interface TodoProjectMemberRepository extends JpaRepository<TodoProjectMember, TodoProjectMemberKey> {
}
