package net.bitnp.guildofpioneers.storage;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves accepted image content types to file extensions for the avatar namespace.
 */
@Component
public class AvatarFileTypeHandler implements NamespaceFileTypeHandler {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    /**
     * Resolves an accepted image content type to its canonical file extension.
     *
     * @param contentType the MIME content type of the uploaded file
     * @return the file extension, or {@code null} if the type is not an allowed image
     */
    @Override
    public String extensionForContentType(String contentType) {
        return CONTENT_TYPES.get(contentType);
    }
}
