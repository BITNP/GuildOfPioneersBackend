package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.exception.InvalidFileTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final String AVATAR_DIR = "avatars";
    private static final String PUBLIC_PREFIX = "/uploads/";

    private static final Map<String, String> AVATAR_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("Avatar file is required");
        }
        String extension = AVATAR_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new InvalidFileTypeException("Unsupported image type: " + file.getContentType());
        }
        Path dir = uploadDir.resolve(AVATAR_DIR);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(userId + "." + extension);
            Files.write(target, file.getBytes());
            log.info("Stored avatar for user {} at {}", userId, target);
            return PUBLIC_PREFIX + AVATAR_DIR + "/" + target.getFileName();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store avatar", ex);
        }
    }

    public void deleteAvatar(String avatarUrl) {
        Path file = toFilePath(avatarUrl);
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
            log.info("Deleted avatar file {}", file);
        } catch (IOException ex) {
            log.warn("Failed to delete avatar file {}", file, ex);
        }
    }

    public Long getVersion(String avatarUrl) {
        Path file = toFilePath(avatarUrl);
        if (file == null || !Files.exists(file)) {
            return null;
        }
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException ex) {
            log.warn("Failed to read avatar file timestamp {}", file, ex);
            return null;
        }
    }

    private Path toFilePath(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank() || !avatarUrl.startsWith(PUBLIC_PREFIX)) {
            return null;
        }
        String relative = avatarUrl.substring(PUBLIC_PREFIX.length());
        Path file = uploadDir.resolve(relative).normalize();
        if (!file.startsWith(uploadDir)) {
            log.warn("Rejected avatar path escaping upload dir: {}", avatarUrl);
            return null;
        }
        return file;
    }
}
