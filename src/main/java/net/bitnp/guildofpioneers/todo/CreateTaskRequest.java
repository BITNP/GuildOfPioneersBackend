package net.bitnp.guildofpioneers.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request body for creating a task under a project with its leaders and members.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String title;

    private String description;

    private List<Long> leaderIds;

    private List<Long> memberIds;
}
