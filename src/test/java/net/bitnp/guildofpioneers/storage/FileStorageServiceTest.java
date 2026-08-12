package net.bitnp.guildofpioneers.storage;

import com.potato.object.ObjectData;
import com.potato.object.ObjectManager;
import com.potato.object.ObjectMetadata;
import com.potato.object.ObjectReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileStorageService}.
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private static final String NAMESPACE = ObjectManagerRegistry.NAMESPACE_AVATARS;

    @TempDir
    Path tempDir;

    @Mock
    private ObjectManagerRegistry registry;

    @Mock
    private ObjectManager manager;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(registry, new AvatarFileTypeHandler(), tempDir.toString());
    }

    @Test
    void storeAvatar_storesFileAndReturnsPublicUrl() throws IOException {
        when(registry.get(NAMESPACE)).thenReturn(manager);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );

        String url = service.storeAvatar(file, 42L);

        assertThat(url).isEqualTo("/uploads/avatars/42");
        verify(manager).update(argThat(s -> "42".equals(s.key())), eq("42.png"), any());
    }

    @Test
    void storeAvatar_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.storeAvatar(file, 42L))
                .isInstanceOf(InvalidFileTypeException.class);
        verify(registry, never()).get(any());
    }

    @Test
    void storeAvatar_rejectsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.txt", "text/plain", "hello".getBytes()
        );

        assertThatThrownBy(() -> service.storeAvatar(file, 42L))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    void get_returnsStoredObject() {
        ObjectMetadata metadata = new ObjectMetadata(
                "42.png", "png", 3L, "md5", "2026-01-01T00:00:00Z", null, "DISK", "avatars/42.png", 0L);
        ObjectData data = new ObjectData(metadata, new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(registry.get(NAMESPACE)).thenReturn(manager);
        when(manager.get(any())).thenReturn(data);

        assertThat(service.get(NAMESPACE, "42")).isSameAs(data);
    }

    @Test
    void get_missingKey_throwsNotFound() {
        when(registry.get(NAMESPACE)).thenReturn(manager);
        when(manager.get(any())).thenThrow(new IllegalArgumentException("does not exist"));

        assertThatThrownBy(() -> service.get(NAMESPACE, "42"))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void get_unknownNamespace_throwsNotFound() {
        when(registry.get("unknown")).thenReturn(null);

        assertThatThrownBy(() -> service.get("unknown", "42"))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void delete_removesStoredFile() {
        when(registry.get(NAMESPACE)).thenReturn(manager);

        service.delete(NAMESPACE, "42");

        verify(manager).remove(argThat(s -> "42".equals(s.key())));
    }

    @Test
    void delete_unknownNamespace_isNoOp() {
        when(registry.get("unknown")).thenReturn(null);

        service.delete("unknown", "42");

        verify(manager, never()).remove(any());
    }

    @Test
    void avatarUrl_returnsUrlWithVersion() throws IOException {
        when(registry.get(NAMESPACE)).thenReturn(manager);
        ObjectMetadata metadata = new ObjectMetadata(
                "42.png", "png", 3L, "md5", "2026-01-01T00:00:00Z", null, "DISK", "avatars/42.png", 0L);
        ObjectReference reference = new ObjectReference("42", Map.of(), metadata);
        when(manager.query(any())).thenReturn(List.of(reference));

        Path file = tempDir.resolve("avatars/42.png");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1, 2, 3});

        assertThat(service.avatarUrl(42L)).startsWith("/uploads/avatars/42?v=");
    }

    @Test
    void avatarUrl_returnsNullWhenMissing() {
        when(registry.get(NAMESPACE)).thenReturn(manager);
        when(manager.query(any())).thenReturn(List.of());

        assertThat(service.avatarUrl(42L)).isNull();
    }
}
