package net.bitnp.guildofpioneers.todo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response body describing a todo task within the hierarchy tree. The {@code actions}
 * list is only present when the requested depth includes actions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TodoTreeTaskResponse {

    private Long id;
    private Long projectId;
    private String title;
    private List<TodoTreeActionResponse> actions;
}
