package net.bitnp.guildofpioneers.todo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response body for a project update. Carries the project's own fields and the
 * member user ids only; user name and avatar summaries are not resolved here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoProjectUpdateResponse {

    private Long id;
    private String title;
    private String cover;
    private String description;
    private Instant createdDate;
    private Instant updatedDate;
    private Instant endDate;
    private List<Long> leaderIds;
    private List<Long> memberIds;
}
