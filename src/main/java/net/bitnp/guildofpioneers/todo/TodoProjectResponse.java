package net.bitnp.guildofpioneers.todo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response body describing a todo project.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoProjectResponse {

    private Long id;
    private String title;
    private String cover;
    private String description;
    private Instant createdDate;
    private Instant endDate;
    private List<Long> leaderIds;
    private List<Long> memberIds;
}
