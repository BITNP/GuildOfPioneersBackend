package net.bitnp.guildofpioneers.todo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request body for updating an action's title, description, and members.
 * When {@code memberIds} is provided, the membership list is replaced entirely;
 * when absent, it is left unchanged.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActionRequest {

    @NotBlank
    private String title;

    private String description;

    private List<Long> memberIds;
}
