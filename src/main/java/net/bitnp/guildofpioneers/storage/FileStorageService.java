package net.bitnp.guildofpioneers.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stores, retrieves, and deletes files through the injected {@link ObjectStorage}.
 *
 * <p>Provides generic namespace/key based operations plus avatar-specific convenience
 * methods. Stored files are exposed under the public {@code /uploads/} prefix.</p>
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final String PUBLIC_PREFIX = "/uploads/";

    public static final String NAMESPACE_AVATARS = "avatars";
    public static final String NAMESPACE_PROJECT_COVERS = "project_covers";

    /**
     * Reserved key under which the default avatar is stored in the {@code avatars}
     * namespace. Users without their own avatar fall back to this object.
     */
    public static final String DEFAULT_AVATAR_KEY = "default";

    private final ObjectStorage objectStorage;
    private final AvatarFileTypeHandler avatarFileTypeHandler;

    /**
     * @param objectStorage         the injected storage backend
     * @param avatarFileTypeHandler resolves accepted avatar image types
     */
    public FileStorageService(ObjectStorage objectStorage, AvatarFileTypeHandler avatarFileTypeHandler) {
        this.objectStorage = objectStorage;
        this.avatarFileTypeHandler = avatarFileTypeHandler;
    }

    /**
     * Builds the object key used by the storage backend.
     *
     * @param namespace the namespace of the object
     * @param key       the object key within the namespace
     * @return the full slash-separated object key
     */
    public static String objectKey(String namespace, String key) {
        return namespace + "/" + key;
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
        return store(NAMESPACE_AVATARS, String.valueOf(userId), file, avatarFileTypeHandler);
    }

    /**
     * Stores a cover image for a project and returns its public URL.
     *
     * @param file      the uploaded cover image
     * @param projectId the owning project's id
     * @return the public URL of the stored cover
     * @throws InvalidFileTypeException if the file is empty or not a supported image type
     */
    public String storeProjectCover(MultipartFile file, Long projectId) {
        return store(NAMESPACE_PROJECT_COVERS, String.valueOf(projectId), file, avatarFileTypeHandler);
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
        try (InputStream source = file.getInputStream()) {
            objectStorage.put(objectKey(namespace, key), source, file.getSize(), file.getContentType());
        } catch (IOException ex) {
            log.error("Failed to read upload for {} key {} from object storage", namespace, key, ex);
            throw new IllegalStateException("Failed to store file", ex);
        }
        log.info("Stored file {}/{}", namespace, key);
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
    public StoredObject get(String namespace, String key) {
        return objectStorage.get(objectKey(namespace, key));
    }

    /**
     * Deletes the file in the given namespace under the given key, if any.
     *
     * @param namespace the namespace of the object
     * @param key       the object key
     */
    public void delete(String namespace, String key) {
        objectStorage.delete(objectKey(namespace, key));
    }

    /**
     * Returns the public URL of the user's avatar, falling back to the default avatar
     * when the user has no stored avatar. A cache-busting {@code ?v=} version is
     * appended when the object's version marker is available.
     *
     * @param userId the owning user's id
     * @return the user's avatar URL, or the default avatar URL if the user has none
     */
    public String avatarUrl(Long userId) {
        String userUrl = urlFor(NAMESPACE_AVATARS, String.valueOf(userId));
        if (userUrl != null) {
            return userUrl;
        }
        String defaultUrl = urlFor(NAMESPACE_AVATARS, DEFAULT_AVATAR_KEY);
        return defaultUrl != null
                ? defaultUrl
                : PUBLIC_PREFIX + NAMESPACE_AVATARS + "/" + DEFAULT_AVATAR_KEY;
    }

    /**
     * Returns the public URL of the project's cover image, or {@code null} if the
     * project has no cover. A cache-busting {@code ?v=} version is appended when the
     * object's version marker is available.
     *
     * @param projectId the owning project's id
     * @return the cover URL, or {@code null} if no cover exists
     */
    public String projectCoverUrl(Long projectId) {
        return urlFor(NAMESPACE_PROJECT_COVERS, String.valueOf(projectId));
    }

    private String urlFor(String namespace, String key) {
        ObjectInfo info = objectStorage.head(objectKey(namespace, key));
        if (info == null) {
            return null;
        }
        String base = PUBLIC_PREFIX + namespace + "/" + key;
        return info.version() == null ? base : base + "?v=" + info.version();
    }
}
