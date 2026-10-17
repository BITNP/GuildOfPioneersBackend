package net.bitnp.guildofpioneers.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import net.bitnp.guildmaintenance.IdentityLinkConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import static org.assertj.core.api.Assertions.*;

/** Verifies the real maintenance context can migrate and bind without starting the web application. */
class IdentityLinkCommandTest {
    @TempDir Path directory;

    @Test void commandRunsOnlyBindingRunnerAndNeedsNoProviderOrStorageAccess() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("guild") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=KEY";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users (id,user_name,phone) VALUES (1,'ExistingAdmin','13800000000')");
        }
        Path bindings = directory.resolve("bindings.json");
        Files.writeString(bindings, """
                [{"userId":1,"issuer":"http://localhost:8082/realms/guild","subject":"operator-verified-subject"}]
                """);
        try (var context = new SpringApplicationBuilder(IdentityLinkConfiguration.class).web(WebApplicationType.NONE)
                .run("bind-identities", "--spring.datasource.url=" + url, "--spring.datasource.username=sa",
                        "--spring.datasource.password=", "--app.identity-link.file=" + bindings)) {
            assertThat(context.getBeansOfType(ApplicationRunner.class)).hasSize(1);
            assertThat(context.containsBean("securityFilterChain")).isFalse();
            assertThat(context.containsBean("dataSeeder")).isFalse();
            assertThat(context.containsBean("defaultAvatarInitializer")).isFalse();
            assertThat(context.containsBean("storageBucketInitializer")).isFalse();
            assertThat(context.getBean(OidcIdentityRepository.class).findById(1L).orElseThrow().getSubject())
                    .isEqualTo("operator-verified-subject");
        }
    }
}
