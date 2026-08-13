package net.bitnp.guildofpioneers.storage;

import com.potato.object.ObjectData;
import com.potato.object.ObjectManager;
import com.potato.object.ObjectReference;
import com.potato.object.ObjectStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Stores, retrieves, and deletes files through the Veil library.
 *
 * <p>Provides generic namespace/key based operations plus avatar-specific convenience
 * methods. Stored files are exposed under the public {@code /uploads/} prefix.</p>
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final String PUBLIC_PREFIX = "/uploads/";

    private final ObjectManagerRegistry registry;
    private final AvatarFileTypeHandler avatarFileTypeHandler;
    private final Path uploadDir;

    /**
     * @param registry              the namespace managers
     * @param avatarFileTypeHandler resolves accepted avatar image types
     * @param uploadDir             the root directory that stored files are resolved against
     */
    public FileStorageService(
            ObjectManagerRegistry registry,
            AvatarFileTypeHandler avatarFileTypeHandler,
            @Value("${app.upload-dir:./uploads}") String uploadDir
    ) {
        this.registry = registry;
        this.avatarFileTypeHandler = avatarFileTypeHandler;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Stores an avatar image for a user and returns its public URL.
     *
     * @param file   the uploaded avatar image
     * @param userId the owning user's id
     * @return the public URL of the stored avatar
     * @throws InvalidFileTypeException if the file is empty or not a supported image type
     */
    public String storeAvatar(MultipartFile file, Long userId) {
        return store(ObjectManagerRegistry.NAMESPACE_AVATARS, String.valueOf(userId), file, avatarFileTypeHandler);
    }

    /**
     * Stores a file in the given namespace under the given key.
     *
     * @param namespace the namespace to store into
     * @param key       the object key
     * @param file      the uploaded file
     * @param handler   resolves the file's canonical extension from its content type
     * @return the public URL of the stored file
     * @throws InvalidFileTypeException if the file is empty or of an unsupported type
     */
    public String store(String namespace, String key, MultipartFile file, NamespaceFileTypeHandler handler) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("File is required");
        }
        String extension = handler.extensionForContentType(file.getContentType());
        if (extension == null) {
            throw new InvalidFileTypeException("Unsupported file type: " + file.getContentType());
        }
        String fileName = key + "." + extension;
        ObjectStatement statement = ObjectStatement.builder().key(key).build();
        try (InputStream source = file.getInputStream()) {
            requireManager(namespace).update(statement, fileName, source);
        } catch (IOException ex) {
            log.error("Failed to read upload for {} key {} in {}", namespace, key, uploadDir, ex);
            throw new IllegalStateException("Failed to store file", ex);
        }
        log.info("Stored file {}/{}", namespace, fileName);
        return PUBLIC_PREFIX + namespace + "/" + key;
    }

    /**
     * Retrieves a stored object's metadata and content stream. The caller is
     * responsible for closing the returned stream.
     *
     * @param namespace the namespace of the object
     * @param key       the object key
     * @return the object's metadata and content stream
     * @throws StoredFileNotFoundException if no object with the given key exists
     */
    public ObjectData get(String namespace, String key) {
        ObjectManager manager = registry.get(namespace);
        if (manager == null) {
            throw new StoredFileNotFoundException(namespace, key);
        }
        try {
            return manager.get(ObjectStatement.builder().key(key).build());
        } catch (IllegalArgumentException ex) {
            throw new StoredFileNotFoundException(namespace, key);
        }
    }

    /**
     * Deletes the file in the given namespace under the given key, if any.
     *
     * @param namespace the namespace of the object
     * @param key       the object key
     */
    public void delete(String namespace, String key) {
        ObjectManager manager = registry.get(namespace);
        if (manager == null) {
            return;
        }
        try {
            manager.remove(ObjectStatement.builder().key(key).build());
        } catch (IllegalArgumentException ex) {
            log.warn("Attempted to delete missing file {}/{}", namespace, key);
        }
    }

    /**
     * Returns the public URL of the user's avatar, or {@code null} if the user has no
     * avatar. A cache-busting {@code ?v=} version is appended when the file's
     * last-modified timestamp is available.
     *
     * @param userId the owning user's id
     * @return the avatar URL, or {@code null} if no avatar exists
     */
    public String avatarUrl(Long userId) {
        return urlFor(ObjectManagerRegistry.NAMESPACE_AVATARS, String.valueOf(userId));
    }

    /**
     * Returns the public URL of the project's cover image, or {@code null} if the
     * project has no cover. A cache-busting {@code ?v=} version is appended when the
     * file's last-modified timestamp is available.
     *
     * @param projectId the owning project's id
     * @return the cover URL, or {@code null} if no cover exists
     */
    public String projectCoverUrl(Long projectId) {
        return urlFor(ObjectManagerRegistry.NAMESPACE_PROJECT_COVERS, String.valueOf(projectId));
    }

    private String urlFor(String namespace, String key) {
        ObjectReference reference = find(namespace, key);
        if (reference == null) {
            return null;
        }
        String base = PUBLIC_PREFIX + namespace + "/" + key;
        Path file = uploadDir.resolve(reference.metadata().storageLocation()).normalize();
        try {
            return base + "?v=" + Files.getLastModifiedTime(file).toMillis();
        } catch (IOException ex) {
            log.warn("Failed to read file timestamp {}", file, ex);
            return base;
        }
    }

    private ObjectReference find(String namespace, String key) {
        ObjectManager manager = registry.get(namespace);
        if (manager == null) {
            return null;
        }
        List<ObjectReference> matches = manager.query(
                ObjectStatement.builder().where("key", ObjectStatement.Op.EQ, key).build());
        return matches.isEmpty() ? null : matches.get(0);
    }

    private ObjectManager requireManager(String namespace) {
        ObjectManager manager = registry.get(namespace);
        if (manager == null) {
            throw new IllegalArgumentException("Unknown storage namespace: " + namespace);
        }
        return manager;
    }
}
