package net.bitnp.guildofpioneers.todo.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join entity linking a user as a leader of a task.
 */
@Entity
@Table(name = "todo_task_leaders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoTaskLeader {

    @EmbeddedId
    private TodoTaskLeaderKey id;
}
