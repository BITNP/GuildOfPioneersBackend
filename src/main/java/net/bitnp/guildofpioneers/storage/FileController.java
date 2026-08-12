package net.bitnp.guildofpioneers.storage;

import com.potato.object.ObjectData;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Serves stored files through the application so authorization can be added later.
 */
@RestController
@RequestMapping("/uploads")
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * @param fileStorageService the file storage used to stream stored objects
     */
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Streams a stored file to the client.
     *
     * @param namespace the namespace the file belongs to
     * @param fileName  the stored file name, including its extension
     * @param response  the servlet response to write the file to
     * @throws IOException if the file cannot be streamed
     */
    @GetMapping("/{namespace}/{fileName}")
    public void serve(
            @PathVariable String namespace,
            @PathVariable String fileName,
            HttpServletResponse response
    ) throws IOException {
        String key = stripExtension(fileName);
        ObjectData data = fileStorageService.get(namespace, key);
        response.setContentType(contentTypeFor(data.metadata().fileExtension()));
        response.setContentLengthLong(data.metadata().fileSize());
        try (InputStream stream = data.stream()) {
            stream.transferTo(response.getOutputStream());
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? fileName : fileName.substring(0, dot);
    }

    private static String contentTypeFor(String extension) {
        return switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "webp" -> "image/webp";
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }
}
