package net.bitnp.guildofpioneers.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for {@link RegistrationTicket} persistence and lookups.
 */
public interface RegistrationTicketRepository extends JpaRepository<RegistrationTicket, Long> {

    boolean existsByCode(String code);

    Optional<RegistrationTicket> findByCode(String code);
}
