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
    private Department department;
    private DepartmentRole role;
}
