package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoTask;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link TodoTask} persistence.
 */
public interface TodoTaskRepository extends JpaRepository<TodoTask, Long> {
}
