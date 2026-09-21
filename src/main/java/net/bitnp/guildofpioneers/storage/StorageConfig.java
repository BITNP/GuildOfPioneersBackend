package net.bitnp.guildofpioneers.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * Wires the S3-compatible {@link ObjectStorage} implementation.
 *
 * <p>Selects the backend through {@code app.storage.type}: {@code s3} (default)
 * builds an {@link S3Client} pointed at {@link StorageProperties#endpoint()}, while
 * {@code memory} injects {@link InMemoryObjectStorage}. Because the storage port is
 * injected, the rest of the application is unaware of which backend is active.</p>
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    /**
     * Builds the S3 client used by the default backend. Credentials are only set when
     * configured, so an empty key pair falls back to the AWS default credentials chain.
     *
     * @param properties the bound storage properties
     * @return the configured S3 client
     */
    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
    public S3Client s3Client(StorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(StringUtils.hasText(properties.region()) ? properties.region() : "us-east-1"))
                .forcePathStyle(properties.pathStyleAccess());
        if (StringUtils.hasText(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        if (StringUtils.hasText(properties.accessKey()) && StringUtils.hasText(properties.secretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
    public ObjectStorage s3ObjectStorage(S3Client s3Client, StorageProperties properties) {
        return new S3ObjectStorage(s3Client, properties.bucket());
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "memory")
    public ObjectStorage inMemoryObjectStorage() {
        return new InMemoryObjectStorage();
    }
}
