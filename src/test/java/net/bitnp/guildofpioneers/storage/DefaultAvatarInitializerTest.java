package net.bitnp.guildofpioneers.storage;

import com.potato.object.ObjectManager;
import com.potato.object.ObjectStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultAvatarInitializer}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultAvatarInitializerTest {

    @TempDir
    Path tempDir;

    @Mock
    private ObjectManagerRegistry registry;

    @Mock
    private ObjectManager manager;

    @Test
    void run_storesDefaultAvatarIntoVeil() throws IOException {
        Path image = tempDir.resolve("default_avatar.jpg");
        byte[] content = {1, 2, 3, 4};
        Files.write(image, content);
        when(registry.get(ObjectManagerRegistry.NAMESPACE_AVATARS)).thenReturn(manager);
        AtomicReference<String> storedKey = new AtomicReference<>();
        AtomicReference<String> storedFileName = new AtomicReference<>();
        AtomicReference<byte[]> storedContent = new AtomicReference<>();
        doAnswer(invocation -> {
            storedKey.set(invocation.getArgument(0, ObjectStatement.class).key());
            storedFileName.set(invocation.getArgument(1));
            try (InputStream source = invocation.getArgument(2)) {
                storedContent.set(source.readAllBytes());
            }
            return null;
        }).when(manager).update(any(), any(), any());

        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(registry, image.toString());

        initializer.run(null);

        assertThat(storedKey.get()).isEqualTo(FileStorageService.DEFAULT_AVATAR_KEY);
        assertThat(storedFileName.get()).isEqualTo(FileStorageService.DEFAULT_AVATAR_KEY + ".jpg");
        assertThat(storedContent.get()).isEqualTo(content);
    }

    @Test
    void run_missingFile_doesNotThrow() {
        when(registry.get(ObjectManagerRegistry.NAMESPACE_AVATARS)).thenReturn(manager);

        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(
                registry, tempDir.resolve("missing.jpg").toString());

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
        verify(manager, never()).update(any(), any(), any());
    }

    @Test
    void run_unknownNamespace_doesNotThrow() {
        when(registry.get(ObjectManagerRegistry.NAMESPACE_AVATARS)).thenReturn(null);

        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(
                registry, tempDir.resolve("default_avatar.jpg").toString());

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_storageFailure_doesNotThrow() throws IOException {
        Path image = tempDir.resolve("default_avatar.jpg");
        Files.write(image, new byte[]{1});
        when(registry.get(ObjectManagerRegistry.NAMESPACE_AVATARS)).thenReturn(manager);
        doThrow(new RuntimeException("storage unavailable")).when(manager).update(any(), any(), any());

        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(registry, image.toString());

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
    }
}
