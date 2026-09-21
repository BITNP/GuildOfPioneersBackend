package net.bitnp.guildofpioneers.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link ObjectStorage} used for tests and local runs without a storage
 * server. Objects live only for the lifetime of the JVM.
 */
public class InMemoryObjectStorage implements ObjectStorage {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final Map<String, String> contentTypes = new ConcurrentHashMap<>();

    @Override
    public void put(String objectKey, InputStream content, long contentLength, String contentType) throws IOException {
        objects.put(objectKey, content.readAllBytes());
        if (contentType != null) {
            contentTypes.put(objectKey, contentType);
        } else {
            contentTypes.remove(objectKey);
        }
    }

    @Override
    public StoredObject get(String objectKey) {
        byte[] content = objects.get(objectKey);
        if (content == null) {
            throw new StoredFileNotFoundException(objectKey);
        }
        return new StoredObject(new ByteArrayInputStream(content), contentTypes.get(objectKey), content.length);
    }

    @Override
    public ObjectInfo head(String objectKey) {
        byte[] content = objects.get(objectKey);
        if (content == null) {
            return null;
        }
        return new ObjectInfo(
                contentTypes.get(objectKey),
                content.length,
                String.valueOf(Arrays.hashCode(content))
        );
    }

    @Override
    public void delete(String objectKey) {
        objects.remove(objectKey);
        contentTypes.remove(objectKey);
    }
}
