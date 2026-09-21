package net.bitnp.guildofpioneers.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultAvatarInitializer}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultAvatarInitializerTest {

    @TempDir
    Path tempDir;

    @Mock
    private ObjectStorage objectStorage;

    @Test
    void run_storesDefaultAvatarIntoObjectStorage() throws IOException {
        Path image = tempDir.resolve("default_avatar.jpg");
        byte[] content = {1, 2, 3, 4};
        Files.write(image, content);
        AtomicReference<String> storedKey = new AtomicReference<>();
        AtomicReference<String> storedContentType = new AtomicReference<>();
        AtomicReference<byte[]> storedContent = new AtomicReference<>();
        AtomicLong storedLength = new AtomicLong();
        doAnswer(invocation -> {
            storedKey.set(invocation.getArgument(0));
            try (InputStream source = invocation.getArgument(1)) {
                storedContent.set(source.readAllBytes());
            }
            storedLength.set(invocation.getArgument(2));
            storedContentType.set(invocation.getArgument(3));
            return null;
        }).when(objectStorage).put(any(), any(), anyLong(), any());

        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(objectStorage, image.toString());

        initializer.run(null);

        assertThat(storedKey.get()).isEqualTo("avatars/" + FileStorageService.DEFAULT_AVATAR_KEY);
        assertThat(storedLength.get()).isEqualTo(content.length);
        assertThat(storedContent.get()).isEqualTo(content);
        assertThat(storedContentType.get()).isEqualTo("image/jpeg");
    }

    @Test
    void run_missingFile_doesNotThrow() throws IOException {
        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(
                objectStorage, tempDir.resolve("missing.jpg").toString());

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
        verify(objectStorage, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void run_storageFailure_doesNotThrow() throws IOException {
        Path image = tempDir.resolve("default_avatar.jpg");
        Files.write(image, new byte[]{1});
        doThrow(new RuntimeException("storage unavailable"))
                .when(objectStorage).put(any(), any(), anyLong(), any());

        DefaultAvatarInitializer initializer = new DefaultAvatarInitializer(objectStorage, image.toString());

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
    }
}
