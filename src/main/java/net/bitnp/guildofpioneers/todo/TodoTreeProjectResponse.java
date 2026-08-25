package net.bitnp.guildofpioneers.todo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response body describing a todo project within the hierarchy tree. The {@code tasks}
 * list is only present when the requested depth includes tasks.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TodoTreeProjectResponse {

    private Long id;
    private String title;
    private List<TodoTreeTaskResponse> tasks;
}
