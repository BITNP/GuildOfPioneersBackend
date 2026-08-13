package net.bitnp.guildofpioneers.todo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response body describing a todo task.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoTaskResponse {

    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private Instant createdDate;
    private Instant updatedDate;
    private Instant endDate;
    private List<Long> leaderIds;
    private List<Long> memberIds;
}
