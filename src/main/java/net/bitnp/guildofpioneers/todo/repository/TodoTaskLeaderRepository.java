package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoTaskLeader;
import net.bitnp.guildofpioneers.todo.entity.TodoTaskLeaderKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link TodoTaskLeader} persistence.
 */
public interface TodoTaskLeaderRepository extends JpaRepository<TodoTaskLeader, TodoTaskLeaderKey> {

    List<TodoTaskLeader> findById_TaskId(Long taskId);
}
