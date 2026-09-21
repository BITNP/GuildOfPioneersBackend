package net.bitnp.guildofpioneers.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileStorageService}.
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private static final String NAMESPACE = FileStorageService.NAMESPACE_AVATARS;

    @Mock
    private ObjectStorage objectStorage;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(objectStorage, new AvatarFileTypeHandler());
    }

    @Test
    void storeAvatar_storesFileAndReturnsPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );

        String url = service.storeAvatar(file, 42L);

        assertThat(url).isEqualTo("/uploads/avatars/42");
        verify(objectStorage).put(eq("avatars/42"), any(), eq(3L), eq("image/png"));
    }

    @Test
    void storeAvatar_rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.storeAvatar(file, 42L))
                .isInstanceOf(InvalidFileTypeException.class);
        verify(objectStorage, never()).put(any(), any(), anyLong(), any());
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
        StoredObject stored = new StoredObject(new ByteArrayInputStream(new byte[]{1, 2, 3}), "image/png", 3L);
        when(objectStorage.get("avatars/42")).thenReturn(stored);

        assertThat(service.get(NAMESPACE, "42")).isSameAs(stored);
    }

    @Test
    void get_missingKey_throwsNotFound() {
        when(objectStorage.get("avatars/42")).thenThrow(new StoredFileNotFoundException("avatars/42"));

        assertThatThrownBy(() -> service.get(NAMESPACE, "42"))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void delete_removesStoredFile() {
        service.delete(NAMESPACE, "42");

        verify(objectStorage).delete("avatars/42");
    }

    @Test
    void avatarUrl_returnsUrlWithVersion() {
        when(objectStorage.head("avatars/42")).thenReturn(new ObjectInfo("image/png", 3L, "v123"));

        assertThat(service.avatarUrl(42L)).isEqualTo("/uploads/avatars/42?v=v123");
    }

    @Test
    void avatarUrl_fallsBackToDefaultWhenUserHasNone() {
        when(objectStorage.head("avatars/42")).thenReturn(null);
        when(objectStorage.head("avatars/default")).thenReturn(new ObjectInfo("image/jpeg", 3L, "v456"));

        assertThat(service.avatarUrl(42L)).isEqualTo("/uploads/avatars/default?v=v456");
    }

    @Test
    void avatarUrl_returnsBareDefaultUrlWhenNothingStored() {
        when(objectStorage.head("avatars/42")).thenReturn(null);
        when(objectStorage.head("avatars/default")).thenReturn(null);

        assertThat(service.avatarUrl(42L)).isEqualTo("/uploads/avatars/default");
    }
}
