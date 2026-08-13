package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeader;
import net.bitnp.guildofpioneers.todo.entity.TodoProjectLeaderKey;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link TodoProjectLeader} persistence.
 */
public interface TodoProjectLeaderRepository extends JpaRepository<TodoProjectLeader, TodoProjectLeaderKey> {
}
