package net.bitnp.guildofpioneers.todo.repository;

import net.bitnp.guildofpioneers.todo.entity.TodoActionMember;
import net.bitnp.guildofpioneers.todo.entity.TodoActionMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link TodoActionMember} persistence.
 */
public interface TodoActionMemberRepository extends JpaRepository<TodoActionMember, TodoActionMemberKey> {

    List<TodoActionMember> findById_ActionId(Long actionId);
}
