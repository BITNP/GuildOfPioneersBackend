package net.bitnp.guildofpioneers.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Imports the default avatar image into object storage on every startup.
 *
 * <p>The source image is read from the filesystem location configured via
 * {@code app.default-avatar} and stored under the reserved {@code default} key of the
 * {@code avatars} namespace. The object is replaced in place on each boot, so editing
 * the source image and restarting the application updates the default avatar. A
 * missing or unreadable image is logged as an error but does not abort startup.
 * Active only when {@code app.default-avatar-enabled=true} (the default).</p>
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.default-avatar-enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAvatarInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAvatarInitializer.class);

    private static final String DEFAULT_AVATAR_CONTENT_TYPE = "image/jpeg";

    private final ObjectStorage objectStorage;
    private final Path defaultAvatarPath;

    /**
     * @param objectStorage  the storage backend to store the default avatar into
     * @param defaultAvatar  the filesystem location of the default avatar image
     */
    public DefaultAvatarInitializer(
            ObjectStorage objectStorage,
            @Value("${app.default-avatar:./default_avatar.jpg}") String defaultAvatar
    ) {
        this.objectStorage = objectStorage;
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
        try (InputStream source = Files.newInputStream(defaultAvatarPath)) {
            objectStorage.put(
                    FileStorageService.objectKey(
                            FileStorageService.NAMESPACE_AVATARS, FileStorageService.DEFAULT_AVATAR_KEY),
                    source,
                    Files.size(defaultAvatarPath),
                    DEFAULT_AVATAR_CONTENT_TYPE);
            log.info("Imported default avatar {} into object storage", defaultAvatarPath);
        } catch (IOException ex) {
            log.error("Failed to read default avatar {}", defaultAvatarPath, ex);
        } catch (RuntimeException ex) {
            // A broken default avatar must not prevent the application from starting.
            log.error("Failed to store default avatar {} in object storage", defaultAvatarPath, ex);
        }
    }
}
