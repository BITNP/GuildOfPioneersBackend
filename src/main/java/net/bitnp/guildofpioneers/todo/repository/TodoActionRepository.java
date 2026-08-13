package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link TodoAction} persistence.
 */
public interface TodoActionRepository extends JpaRepository<TodoAction, Long> {

    List<TodoAction> findByTaskIdOrderByUpdatedDateDesc(Long taskId);
}
