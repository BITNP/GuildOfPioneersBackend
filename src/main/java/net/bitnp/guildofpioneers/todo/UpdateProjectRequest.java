package net.bitnp.guildofpioneers.todo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request body for updating a project's title, description, leaders, and members.
 * When {@code leaderIds} or {@code memberIds} is provided, the corresponding
 * membership list is replaced entirely; when absent, it is left unchanged.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    @NotBlank
    private String title;

    private String description;

    private List<Long> leaderIds;

    private List<Long> memberIds;
}
