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
 * Join entity linking a user as a member of a project.
 */
@Entity
@Table(name = "todo_project_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoProjectMember {

    @EmbeddedId
    private TodoProjectMemberKey id;
}
