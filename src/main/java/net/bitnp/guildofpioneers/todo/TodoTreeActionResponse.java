package net.bitnp.guildofpioneers.todo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Response body describing a todo action within the hierarchy tree.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoTreeActionResponse {

    private Long id;
    private Long taskId;
    private String title;
    private Instant endDate;
}
