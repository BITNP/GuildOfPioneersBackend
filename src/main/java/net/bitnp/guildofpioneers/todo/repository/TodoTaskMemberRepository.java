package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoTaskMember;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link TodoTaskMember} persistence.
 */
public interface TodoTaskMemberRepository extends JpaRepository<TodoTaskMember, TodoTaskMemberKey> {
}
