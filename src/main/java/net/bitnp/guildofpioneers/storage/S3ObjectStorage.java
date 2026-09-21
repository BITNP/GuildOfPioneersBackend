package net.bitnp.guildofpioneers.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ObjectStorage} backed by an S3-compatible server.
 *
 * <p>Works with RustFS, MinIO, and AWS S3; the endpoint and path-style addressing
 * come from {@link StorageProperties}. Absent objects are reported by HTTP 404 from
 * the server and translated into {@link StoredFileNotFoundException} (for reads) or
 * {@code null} (for metadata lookups) instead of leaking SDK exceptions.</p>
 */
public class S3ObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);

    private final S3Client s3Client;
    private final String bucket;

    /**
     * @param s3Client the injected S3 client
     * @param bucket   the bucket that stores all objects
     */
    public S3ObjectStorage(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void put(String objectKey, InputStream content, long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
    }

    @Override
    public StoredObject get(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            GetObjectResponse metadata = response.response();
            return new StoredObject(response, metadata.contentType(), lengthOf(metadata.contentLength()));
        } catch (S3Exception ex) {
            if (isNotFound(ex)) {
                throw new StoredFileNotFoundException(objectKey);
            }
            throw ex;
        }
    }

    @Override
    public ObjectInfo head(String objectKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        try {
            HeadObjectResponse response = s3Client.headObject(request);
            return new ObjectInfo(response.contentType(), lengthOf(response.contentLength()), versionOf(response.eTag()));
        } catch (S3Exception ex) {
            if (isNotFound(ex)) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            log.warn("Failed to delete object {} from bucket {}", objectKey, bucket, ex);
        }
    }

    private static boolean isNotFound(S3Exception ex) {
        return ex.statusCode() == 404;
    }

    private static long lengthOf(Long contentLength) {
        return contentLength == null ? 0L : contentLength;
    }

    private static String versionOf(String eTag) {
        if (eTag == null) {
            return null;
        }
        return eTag.replace("\"", "");
    }
}
