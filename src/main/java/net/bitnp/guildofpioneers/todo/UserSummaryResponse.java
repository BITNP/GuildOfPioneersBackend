package net.bitnp.guildofpioneers.todo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Brief summary of a user embedded in todo responses, enough for the UI to render
 * the user's avatar and display name without additional requests.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String userName;
    private String avatar;
}
