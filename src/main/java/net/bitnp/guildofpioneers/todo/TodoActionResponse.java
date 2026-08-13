package net.bitnp.guildofpioneers.todo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response body describing a todo action.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoActionResponse {

    private Long id;
    private Long taskId;
    private String title;
    private String description;
    private Instant createdDate;
    private Instant endDate;
    private List<Long> memberIds;
}
