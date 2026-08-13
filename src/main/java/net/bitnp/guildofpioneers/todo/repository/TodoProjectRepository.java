package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link TodoProject} persistence.
 */
public interface TodoProjectRepository extends JpaRepository<TodoProject, Long> {

    List<TodoProject> findAllByOrderByCreatedDateDesc();
}
