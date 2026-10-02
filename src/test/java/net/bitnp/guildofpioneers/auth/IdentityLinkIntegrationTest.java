package net.bitnp.guildofpioneers.auth;

import net.bitnp.guildofpioneers.auth.dto.IdentityLinkRequest;

import net.bitnp.guildofpioneers.auth.dto.RegisterRequest;

import net.bitnp.guildofpioneers.auth.service.IdentityLinkService;

import net.bitnp.guildofpioneers.auth.service.AuthService;

import java.util.List;
import net.bitnp.guildofpioneers.user.entity.*;
import net.bitnp.guildofpioneers.user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** Database tests for atomic, idempotent operator-driven account migration. */
@SpringBootTest(properties = {"app.seed-data=false",
        "spring.datasource.url=jdbc:h2:mem:identitylinks;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=KEY"})
class IdentityLinkIntegrationTest {
    @Autowired IdentityLinkService service;
    @Autowired UserRepository users;
    @Autowired OidcIdentityRepository identities;
    @Autowired UserDepartmentRepository departments;
    @Autowired AuthService admissions;
    @Autowired net.bitnp.guildofpioneers.ticket.RegistrationTicketRepository tickets;
    Long first;
    Long second;

    @BeforeEach void setUp() {
        tickets.deleteAll();
        users.deleteAll();
        first = users.save(User.builder().userName("ExistingAdmin").phone("13800000000").password("historical-hash").build()).getId();
        second = users.save(User.builder().userName("ExistingMember").phone("13900000000").build()).getId();
        departments.save(UserDepartment.builder().userId(first).department(Department.ADMIN).role(DepartmentRole.LEADER).build());
    }

    IdentityLinkRequest binding(Long id, String subject) { return new IdentityLinkRequest(id, OidcTestUsers.ISSUER, subject); }

    @Test void bindsStableIdWithoutChangingDataAndReimportIsIdempotent() {
        assertThat(service.bind(List.of(binding(first, "admin-subject")), OidcTestUsers.ISSUER)).isEqualTo(1);
        assertThat(service.bind(List.of(binding(first, "admin-subject")), OidcTestUsers.ISSUER)).isZero();
        assertThat(identities.findByIssuerAndSubject(OidcTestUsers.ISSUER, "admin-subject").orElseThrow().getUserId()).isEqualTo(first);
        assertThat(users.findById(first).orElseThrow().getPassword()).isEqualTo("historical-hash");
        assertThat(departments.findByUserId(first)).extracting(UserDepartment::getDepartment).containsExactly(Department.ADMIN);
    }

    @Test void conflictRollsBackEveryNewBindingInTheBatch() {
        service.bind(List.of(binding(first, "admin-subject")), OidcTestUsers.ISSUER);
        assertThatThrownBy(() -> service.bind(List.of(binding(second, "new-subject"), binding(first, "other-subject")), OidcTestUsers.ISSUER))
                .isInstanceOf(IdentityConflictException.class);
        assertThat(identities.findById(second)).isEmpty();
        assertThat(identities.findById(first).orElseThrow().getSubject()).isEqualTo("admin-subject");
    }

    @Test void rejectsIdentityReuseMissingUsersAndOtherIssuers() {
        service.bind(List.of(binding(first, "admin-subject")), OidcTestUsers.ISSUER);
        assertThatThrownBy(() -> service.bind(List.of(binding(second, "admin-subject")), OidcTestUsers.ISSUER))
                .isInstanceOf(IdentityConflictException.class);
        assertThatThrownBy(() -> service.bind(List.of(binding(999999L, "unknown")), OidcTestUsers.ISSUER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.bind(List.of(new IdentityLinkRequest(second, "https://other.example", "new-subject")), OidcTestUsers.ISSUER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(identities.count()).isEqualTo(1);
    }

    @Test void concurrentAdmissionKeepsExactlyOneUserAndBinding() throws Exception {
        tickets.save(net.bitnp.guildofpioneers.ticket.RegistrationTicket.builder().code("CONCURRENT")
                .createdAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(3600))
                .createdBy(first).department(Department.TECH).role(DepartmentRole.MEMBER).build());
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.List<java.util.concurrent.Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int number = i;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    var request = RegisterRequest.builder().userName("Concurrent" + number)
                            .phone(number == 0 ? "13100000000" : "13200000000").ticketCode("CONCURRENT").build();
                    try {
                        admissions.register(request, OidcTestUsers.principal(null, "same-subject"));
                        return true;
                    } catch (IdentityConflictException | org.springframework.dao.DataIntegrityViolationException expected) {
                        return false;
                    }
                }));
            }
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successful = 0;
            for (var result : results) if (result.get(10, java.util.concurrent.TimeUnit.SECONDS)) successful++;
            assertThat(successful).isEqualTo(1);
            assertThat(identities.count()).isEqualTo(1);
            assertThat(users.count()).isEqualTo(3);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
