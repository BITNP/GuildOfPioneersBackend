package net.bitnp.guildofpioneers.storage;

import com.potato.object.ObjectData;
import com.potato.object.ObjectMetadata;
import net.bitnp.guildofpioneers.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link FileController}.
 */
@WebMvcTest(FileController.class)
@Import(SecurityConfig.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void serve_returnsFileBytesWithContentType() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata(
                "42.png", "png", 3L, "md5", "2026-01-01T00:00:00Z", null, "DISK", "avatars/42.png", 0L);
        ObjectData data = new ObjectData(metadata, new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(fileStorageService.get("avatars", "42")).thenReturn(data);

        mockMvc.perform(get("/uploads/avatars/42.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void serve_missingFile_returnsNotFound() throws Exception {
        when(fileStorageService.get("avatars", "missing"))
                .thenThrow(new StoredFileNotFoundException("avatars", "missing"));

        mockMvc.perform(get("/uploads/avatars/missing.png"))
                .andExpect(status().isNotFound());
    }
}
