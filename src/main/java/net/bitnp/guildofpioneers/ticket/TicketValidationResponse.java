package net.bitnp.guildofpioneers.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;

import java.time.Instant;

/**
 * Response body describing the validity of a registration ticket and the
 * department and role the holder is invited into.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketValidationResponse {

    private boolean valid;
    private boolean expired;
    private Department department;
    private DepartmentRole role;
    private Instant expiresAt;
}
