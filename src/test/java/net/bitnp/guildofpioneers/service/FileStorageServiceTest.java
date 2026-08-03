package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.exception.InvalidFileTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileStorageService}.
 */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService() {
        return new FileStorageService(tempDir.toString());
    }

    @Test
    void storeAvatar_writesFileAndReturnsPublicPath() throws IOException {
        FileStorageService service = fileStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}
        );

        String path = service.storeAvatar(file, 42L);

        assertThat(path).isEqualTo("/uploads/avatars/42.png");
        assertThat(tempDir.resolve("avatars/42.png")).exists();
    }

    @Test
    void storeAvatar_rejectsUnsupportedType() {
        FileStorageService service = fileStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.txt", "text/plain", "hello".getBytes()
        );

        assertThatThrownBy(() -> service.storeAvatar(file, 42L))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void storeAvatar_rejectsEmptyFile() {
        FileStorageService service = fileStorageService();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.storeAvatar(file, 42L))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void getVersion_returnsTimestampOfStoredFile() {
        FileStorageService service = fileStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}
        );
        String path = service.storeAvatar(file, 42L);

        assertThat(service.getVersion(path)).isNotNull();
        assertThat(service.getVersion("https://example.com/avatar.png")).isNull();
        assertThat(service.getVersion(null)).isNull();
    }

    @Test
    void deleteAvatar_removesStoredFile() throws IOException {
        FileStorageService service = fileStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}
        );
        String path = service.storeAvatar(file, 42L);

        service.deleteAvatar(path);

        assertThat(tempDir.resolve("avatars/42.png")).doesNotExist();
    }

    @Test
    void deleteAvatar_ignoresExternalUrls() throws IOException {
        FileStorageService service = fileStorageService();
        Path external = tempDir.resolve("keep.png");
        Files.write(external, new byte[]{1});

        service.deleteAvatar("https://example.com/avatar.png");
        service.deleteAvatar(null);

        assertThat(external).exists();
    }

    @Test
    void deleteAvatar_blocksPathTraversal() throws IOException {
        FileStorageService service = fileStorageService();
        Path secret = tempDir.resolve("secret.txt");
        Files.write(secret, new byte[]{1});

        service.deleteAvatar("/uploads/../secret.txt");

        assertThat(secret).exists();
    }
}
