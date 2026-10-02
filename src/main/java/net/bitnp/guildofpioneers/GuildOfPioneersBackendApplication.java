package net.bitnp.guildofpioneers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Guild of Pioneers backend application.
 */
@SpringBootApplication
public class GuildOfPioneersBackendApplication {

    /** Starts the site server, or the isolated one-shot identity-binding maintenance context. */
    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("bind-identities")) {
            try (var context = new org.springframework.boot.builder.SpringApplicationBuilder(
                    net.bitnp.guildmaintenance.IdentityLinkConfiguration.class)
                    .web(org.springframework.boot.WebApplicationType.NONE).run(args)) {
                // Closing the narrow context releases database connections after the batch.
            }
            return;
        }
        SpringApplication.run(GuildOfPioneersBackendApplication.class, args);
    }

}
