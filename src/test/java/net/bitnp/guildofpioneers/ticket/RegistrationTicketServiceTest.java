package net.bitnp.guildofpioneers.ticket;

import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.exception.UserNotFoundException;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationTicketService}.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationTicketServiceTest {

    @Mock
    private RegistrationTicketRepository registrationTicketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegistrationTicketService registrationTicketService;

    @Test
    void create_generatesCodeAndPersistsTicket() {
        User creator = User.builder().id(7L).phone("13800000000").build();
        when(userRepository.findByPhone("13800000000")).thenReturn(Optional.of(creator));
        when(registrationTicketRepository.existsByCode(anyString())).thenReturn(false);
        when(registrationTicketRepository.save(any(RegistrationTicket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateRegistrationTicketRequest request = CreateRegistrationTicketRequest.builder()
                .expiresAt(Instant.parse("2026-09-01T00:00:00Z"))
                .build();

        RegistrationTicketResponse response = registrationTicketService.create(request, "13800000000");

        ArgumentCaptor<RegistrationTicket> captor = ArgumentCaptor.forClass(RegistrationTicket.class);
        verify(registrationTicketRepository).save(captor.capture());
        RegistrationTicket saved = captor.getValue();
        assertThat(saved.getCode()).hasSize(12).matches("[A-Z2-9]+");
        assertThat(saved.getCreatedBy()).isEqualTo(7L);
        assertThat(saved.getExpiresAt()).isEqualTo(request.getExpiresAt());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(response.getCode()).isEqualTo(saved.getCode());
        assertThat(response.getCreatedBy()).isEqualTo(7L);
    }

    @Test
    void create_throwsWhenCreatorNotFound() {
        when(userRepository.findByPhone("13800000000")).thenReturn(Optional.empty());

        CreateRegistrationTicketRequest request = CreateRegistrationTicketRequest.builder()
                .expiresAt(Instant.parse("2026-09-01T00:00:00Z"))
                .build();

        assertThatThrownBy(() -> registrationTicketService.create(request, "13800000000"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void validate_passesForValidTicket() {
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code("VALIDCODE123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(registrationTicketRepository.findByCode("VALIDCODE123")).thenReturn(Optional.of(ticket));

        registrationTicketService.validate("VALIDCODE123");
    }

    @Test
    void validate_throwsWhenTicketNotFound() {
        when(registrationTicketRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationTicketService.validate("MISSING"))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void validate_throwsWhenTicketExpired() {
        RegistrationTicket ticket = RegistrationTicket.builder()
                .code("EXPIRED123")
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(registrationTicketRepository.findByCode("EXPIRED123")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> registrationTicketService.validate("EXPIRED123"))
                .isInstanceOf(TicketExpiredException.class);
    }
}
