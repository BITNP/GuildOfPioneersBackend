package net.bitnp.guildofpioneers.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Response body describing a registration ticket.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationTicketResponse {

    private Long id;
    private String code;
    private Instant createdAt;
    private Instant expiresAt;
    private Long createdBy;
}
