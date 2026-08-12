package net.bitnp.guildofpioneers.storage;

import com.potato.VeilConfiguration;
import com.potato.database.DatabaseManager;
import com.potato.database.DatabaseType;
import com.potato.storage.DiskFileManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Initializes the Veil file-management library once for the application.
 *
 * <p>The {@link VeilConfiguration} is a process-wide singleton, so initialization is
 * idempotent: an existing instance is reused instead of failing on re-init.</p>
 */
@Configuration
public class VeilConfig {

    /**
     * Initializes the process-wide Veil configuration, failing fast if it cannot be
     * set up.
     *
     * @param dataSource the pooled datasource used for metadata persistence
     * @param uploadDir  the root directory that stored files are resolved against
     * @return the initialized {@link VeilConfiguration}
     */
    @Bean
    public VeilConfiguration veilConfiguration(
            DataSource dataSource,
            @Value("${app.upload-dir:./uploads}") String uploadDir
    ) {
        VeilConfiguration configuration = getOrInit(dataSource, uploadDir);
        if (configuration == null) {
            throw new IllegalStateException("Failed to initialize Veil configuration");
        }
        return configuration;
    }

    /**
     * Builds the metadata {@link DatabaseManager} used by all namespaces.
     *
     * @param configuration the initialized Veil configuration
     * @param databaseType  the metadata database engine, POSTGRES by default
     * @return a new {@link DatabaseManager}
     */
    @Bean
    public DatabaseManager veilDatabaseManager(
            VeilConfiguration configuration,
            @Value("${app.veil.database-type:POSTGRES}") String databaseType
    ) {
        return DatabaseManager.builder()
                .dataSource(configuration.getDataSource())
                .databaseType(DatabaseType.valueOf(databaseType.toUpperCase(Locale.ROOT)))
                .build();
    }

    private static VeilConfiguration getOrInit(DataSource dataSource, String uploadDir) {
        try {
            return VeilConfiguration.getInstance();
        } catch (IllegalStateException notInitialized) {
            return VeilConfiguration.init(
                    dataSource,
                    new DiskFileManager(Paths.get(uploadDir)),
                    null
            );
        }
    }
}
