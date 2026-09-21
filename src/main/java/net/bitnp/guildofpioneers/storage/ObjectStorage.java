package net.bitnp.guildofpioneers.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stores and retrieves opaque objects by key.
 *
 * <p>This is the storage port injected into the application: production uses an
 * S3-compatible implementation (see {@link S3ObjectStorage}), while tests and local
 * runs can use {@link InMemoryObjectStorage}. Keys are slash-separated namespaces,
 * for example {@code avatars/42}.</p>
 */
public interface ObjectStorage {

    /**
     * Stores an object, replacing any existing object under the same key.
     *
     * @param objectKey     the slash-separated object key
     * @param content       the content stream, consumed by this method
     * @param contentLength the exact number of bytes in the stream
     * @param contentType   the MIME content type of the object
     * @throws IOException if the content cannot be read
     */
    void put(String objectKey, InputStream content, long contentLength, String contentType) throws IOException;

    /**
     * Retrieves an object's metadata and content stream. The caller is responsible
     * for closing the returned stream.
     *
     * @param objectKey the slash-separated object key
     * @return the object's content stream and metadata
     * @throws StoredFileNotFoundException if no object exists under the key
     */
    StoredObject get(String objectKey);

    /**
     * Reads an object's metadata without downloading its content.
     *
     * @param objectKey the slash-separated object key
     * @return the object's metadata, or {@code null} if no object exists under the key
     */
    ObjectInfo head(String objectKey);

    /**
     * Deletes the object under the key, if any.
     *
     * @param objectKey the slash-separated object key
     */
    void delete(String objectKey);
}
