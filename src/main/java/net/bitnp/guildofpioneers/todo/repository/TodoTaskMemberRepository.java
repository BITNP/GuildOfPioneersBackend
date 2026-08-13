package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoTaskMember;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link TodoTaskMember} persistence.
 */
public interface TodoTaskMemberRepository extends JpaRepository<TodoTaskMember, TodoTaskMemberKey> {

    List<TodoTaskMember> findById_TaskId(Long taskId);
}
