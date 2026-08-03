package net.bitnp.guildofpioneers.repository;

import net.bitnp.guildofpioneers.entity.RegistrationTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationTicketRepository extends JpaRepository<RegistrationTicket, Long> {

    boolean existsByCode(String code);

    Optional<RegistrationTicket> findByCode(String code);
}
