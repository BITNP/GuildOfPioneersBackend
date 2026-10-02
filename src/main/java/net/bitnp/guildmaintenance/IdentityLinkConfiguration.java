package net.bitnp.guildmaintenance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.bitnp.guildofpioneers.auth.dto.IdentityLinkRequest;
import net.bitnp.guildofpioneers.auth.service.IdentityLinkService;
import net.bitnp.guildofpioneers.auth.OidcIdentity;
import net.bitnp.guildofpioneers.auth.OidcIdentityRepository;
import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Narrow maintenance context: database and binding service only, without HTTP or seed/storage runners. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {User.class, OidcIdentity.class})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, OidcIdentityRepository.class})
@Import(IdentityLinkService.class)
public class IdentityLinkConfiguration {
    /** Reads an operator-supplied file; invalid batches fail before the application exits. */
    @Bean
    public ApplicationRunner identityLinkRunner(IdentityLinkService service, ObjectMapper mapper,
            @Value("${app.identity-link.file}") String file, @Value("${app.oidc.issuer}") String issuer) {
        return args -> {
            List<IdentityLinkRequest> requests = mapper.readValue(Files.readString(Path.of(file)), new TypeReference<>() { });
            int added = service.bind(requests, issuer);
            org.slf4j.LoggerFactory.getLogger(IdentityLinkConfiguration.class)
                    .info("Identity binding batch completed: {} new associations", added);
        };
    }
}
