package net.bitnp.guildofpioneers.ticket;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;

import java.time.Instant;

/**
 * Request body for creating a new registration ticket.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegistrationTicketRequest {

    @NotNull
    @Future
    private Instant expiresAt;

    @NotNull
    private Department department;

    @NotNull
    private DepartmentRole role;
}
