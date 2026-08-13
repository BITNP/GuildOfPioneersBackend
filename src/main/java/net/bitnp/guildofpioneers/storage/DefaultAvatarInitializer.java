package net.bitnp.guildofpioneers.storage;

import com.potato.object.ObjectManager;
import com.potato.object.ObjectStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Imports the default avatar image into Veil storage on every startup.
 *
 * <p>The source image is read from the filesystem location configured via
 * {@code app.default-avatar} and stored under the reserved {@code default} key of the
 * {@code avatars} namespace. The object is replaced in place on each boot, so editing
 * the source image and restarting the application updates the default avatar. A
 * missing or unreadable image is logged as an error but does not abort startup.
 * Active only when {@code app.default-avatar-enabled=true} (the default).</p>
 */
@Component
@ConditionalOnProperty(name = "app.default-avatar-enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAvatarInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAvatarInitializer.class);

    private final ObjectManagerRegistry registry;
    private final Path defaultAvatarPath;

    /**
     * @param registry       the namespace managers to store the default avatar into
     * @param defaultAvatar  the filesystem location of the default avatar image
     */
    public DefaultAvatarInitializer(
            ObjectManagerRegistry registry,
            @Value("${app.default-avatar:./default_avatar.jpg}") String defaultAvatar
    ) {
        this.registry = registry;
        this.defaultAvatarPath = Paths.get(defaultAvatar).toAbsolutePath().normalize();
    }

    /**
     * Stores the configured default avatar image under the {@code default} key of the
     * {@code avatars} namespace, replacing any previously stored default avatar.
     *
     * @param args the application arguments (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        ObjectManager manager = registry.get(ObjectManagerRegistry.NAMESPACE_AVATARS);
        if (manager == null) {
            log.warn("Cannot import default avatar: unknown namespace {}",
                    ObjectManagerRegistry.NAMESPACE_AVATARS);
            return;
        }
        try (InputStream source = Files.newInputStream(defaultAvatarPath)) {
            manager.update(
                    ObjectStatement.builder().key(FileStorageService.DEFAULT_AVATAR_KEY).build(),
                    FileStorageService.DEFAULT_AVATAR_KEY + ".jpg",
                    source);
            log.info("Imported default avatar {} into Veil storage", defaultAvatarPath);
        } catch (IOException ex) {
            log.error("Failed to read default avatar {}", defaultAvatarPath, ex);
        } catch (RuntimeException ex) {
            // Veil wraps its storage/database failures in runtime exceptions; a broken
            // default avatar must not prevent the application from starting.
            log.error("Failed to store default avatar {} in Veil storage", defaultAvatarPath, ex);
        }
    }
}
