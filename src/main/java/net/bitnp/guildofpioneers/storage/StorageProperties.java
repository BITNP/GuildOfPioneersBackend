package net.bitnp.guildofpioneers.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the object storage backend.
 *
 * <p>Bound from the {@code app.storage.*} properties. The {@code type} selects the
 * implementation to inject: {@code s3} (any S3-compatible server such as RustFS,
 * MinIO, or AWS S3) or {@code memory} (an in-memory store for tests and local runs
 * without a storage server).</p>
 *
 * @param type            the storage backend type, either {@code s3} or {@code memory}
 * @param endpoint        the S3 endpoint URL; blank uses the AWS default
 * @param region          the S3 region, required by the client even for RustFS
 * @param accessKey       the S3 access key; blank falls back to the default credentials chain
 * @param secretKey       the S3 secret key
 * @param bucket          the bucket that stores all objects
 * @param pathStyleAccess whether to address the bucket path-style (required by RustFS and MinIO)
 * @param createBucket    whether to create the bucket at startup when it is missing
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String type,
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        boolean pathStyleAccess,
        boolean createBucket
) {
}
