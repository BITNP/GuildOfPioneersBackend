package net.bitnp.guildofpioneers.auth;

import net.bitnp.guildofpioneers.auth.dto.RegisterRequest;

import net.bitnp.guildofpioneers.auth.service.AuthService;

import net.bitnp.guildofpioneers.ticket.RegistrationTicket;
import net.bitnp.guildofpioneers.ticket.RegistrationTicketService;
import net.bitnp.guildofpioneers.ticket.TicketExpiredException;
import net.bitnp.guildofpioneers.user.entity.Department;
import net.bitnp.guildofpioneers.user.entity.DepartmentRole;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.entity.UserDepartment;
import net.bitnp.guildofpioneers.user.exception.PhoneAlreadyExistsException;
import net.bitnp.guildofpioneers.user.exception.UserNameAlreadyExistsException;
import net.bitnp.guildofpioneers.user.repository.UserDepartmentRepository;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Admission rules remain local and never trust provider profile claims. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock UserDepartmentRepository departments;
    @Mock RegistrationTicketService tickets;
    @Mock OidcIdentityRepository identities;
    AuthService service;
    GuildOidcUser principal = OidcTestUsers.principal(null, "new-subject");
    RegisterRequest request = RegisterRequest.builder().userName("Alice").phone("13800000000")
            .email("alice@example.com").ticketCode("VALIDCODE123").build();

    @BeforeEach void setUp() {
        service = new AuthService(users, tickets, departments, identities,
                new OidcProperties(OidcTestUsers.ISSUER, "test", "secret", "http://localhost:5173", "bootstrap"));
    }

    @Test void grantsTicketMembershipAndCreatesNoPassword() {
        when(tickets.findByCode("VALIDCODE123")).thenReturn(RegistrationTicket.builder()
                .department(Department.TECH).role(DepartmentRole.MEMBER).build());
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        assertThat(service.register(request, principal)).isEqualTo(7L);
        var user = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(user.capture());
        assertThat(user.getValue().getPassword()).isNull();
        var membership = ArgumentCaptor.forClass(UserDepartment.class);
        verify(departments).save(membership.capture());
        assertThat(membership.getValue().getDepartment()).isEqualTo(Department.TECH);
        assertThat(membership.getValue().getRole()).isEqualTo(DepartmentRole.MEMBER);
        var binding = ArgumentCaptor.forClass(OidcIdentity.class);
        verify(identities).saveAndFlush(binding.capture());
        assertThat(binding.getValue().getSubject()).isEqualTo("new-subject");
        assertThat(binding.getValue().getUserId()).isEqualTo(7L);
    }

    @Test void rejectsDuplicatePhone() {
        when(tickets.findByCode(any())).thenReturn(RegistrationTicket.builder().department(Department.TECH).role(DepartmentRole.MEMBER).build());
        when(users.existsByPhone(request.getPhone())).thenReturn(true);
        assertThatThrownBy(() -> service.register(request, principal)).isInstanceOf(PhoneAlreadyExistsException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test void rejectsDuplicateUserName() {
        when(tickets.findByCode(any())).thenReturn(RegistrationTicket.builder().department(Department.TECH).role(DepartmentRole.MEMBER).build());
        when(users.existsByUserNameIgnoreCase("Alice")).thenReturn(true);
        assertThatThrownBy(() -> service.register(request, principal)).isInstanceOf(UserNameAlreadyExistsException.class);
    }

    @Test void rejectsExpiredTicketBeforeCreatingAnAccount() {
        doThrow(new TicketExpiredException("VALIDCODE123")).when(tickets).validate("VALIDCODE123");
        assertThatThrownBy(() -> service.register(request, principal)).isInstanceOf(TicketExpiredException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test void boundIdentityCannotRegisterAgainOrAcquireNewMembership() {
        when(identities.findByIssuerAndSubject(OidcTestUsers.ISSUER, "new-subject"))
                .thenReturn(Optional.of(new OidcIdentity(7L, OidcTestUsers.ISSUER, "new-subject")));
        assertThatThrownBy(() -> service.register(request, principal)).isInstanceOf(IdentityConflictException.class);
        verifyNoInteractions(tickets);
    }

    @Test void bootstrapRequiresExactIdentityAndNoExistingAdministrator() {
        assertThat(service.isBootstrapAdmin(principal)).isFalse();
        GuildOidcUser bootstrap = OidcTestUsers.principal(null, "bootstrap");
        assertThat(service.isBootstrapAdmin(bootstrap)).isTrue();
        assertThat(service.isBootstrapAdmin(bootstrap.withUserId(7L))).isFalse();
        when(departments.existsByDepartment(Department.ADMIN)).thenReturn(true);
        assertThat(service.isBootstrapAdmin(bootstrap)).isFalse();
    }
}
