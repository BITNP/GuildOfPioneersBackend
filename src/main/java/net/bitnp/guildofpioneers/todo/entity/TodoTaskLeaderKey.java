package net.bitnp.guildofpioneers.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite key for a task leader: one user leading one task.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TodoTaskLeaderKey {

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "user_id")
    private Long userId;
}
