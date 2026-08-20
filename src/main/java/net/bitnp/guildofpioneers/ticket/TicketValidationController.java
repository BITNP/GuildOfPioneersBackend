package net.bitnp.guildofpioneers.ticket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for validating registration tickets.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketValidationController {

    private final RegistrationTicketService registrationTicketService;

    public TicketValidationController(RegistrationTicketService registrationTicketService) {
        this.registrationTicketService = registrationTicketService;
    }

    @GetMapping("/{code}")
    public TicketValidationResponse validate(@PathVariable String code) {
        return registrationTicketService.validateTicket(code);
    }
}
