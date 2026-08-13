package net.bitnp.guildofpioneers.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite key for an action member: one user carrying out one action.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TodoActionMemberKey {

    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "user_id")
    private Long userId;
}
