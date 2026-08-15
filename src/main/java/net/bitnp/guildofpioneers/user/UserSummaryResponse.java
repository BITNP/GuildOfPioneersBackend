package net.bitnp.guildofpioneers.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Brief summary of a user, enough for the UI to render the user's avatar and
 * display name without exposing private contact information.
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
