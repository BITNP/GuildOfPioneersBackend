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
 * Request body for creating an action under a task with its members.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActionRequest {

    @NotNull
    private Long taskId;

    @NotBlank
    private String title;

    private String description;

    private List<Long> memberIds;
}
