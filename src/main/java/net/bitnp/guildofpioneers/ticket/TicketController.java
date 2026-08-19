package net.bitnp.guildofpioneers.ticket;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoints for managing registration tickets.
 */
@RestController
@RequestMapping("/api/admin/tickets")
public class TicketController {

    private final RegistrationTicketService registrationTicketService;

    public TicketController(RegistrationTicketService registrationTicketService) {
        this.registrationTicketService = registrationTicketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationTicketResponse create(
            @Valid @RequestBody CreateRegistrationTicketRequest request,
            Authentication authentication
    ) {
        return registrationTicketService.create(request, authentication);
    }
}
