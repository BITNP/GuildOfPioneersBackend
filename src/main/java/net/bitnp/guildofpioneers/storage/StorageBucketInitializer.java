package net.bitnp.guildofpioneers.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Ensures the configured S3 bucket exists before anything is written to it.
 *
 * <p>Runs before {@link DefaultAvatarInitializer}. A storage failure is logged but
 * does not abort startup, mirroring how other optional startup imports behave.</p>
 */
@Component
@Order(0)
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class StorageBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageBucketInitializer.class);

    private final S3Client s3Client;
    private final StorageProperties properties;

    /**
     * @param s3Client   the S3 client used to inspect and create the bucket
     * @param properties the bound storage properties
     */
    public StorageBucketInitializer(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.createBucket()) {
            return;
        }
        String bucket = properties.bucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Object storage bucket {} already exists", bucket);
        } catch (S3Exception ex) {
            if (ex.statusCode() != 404) {
                log.error("Failed to check object storage bucket {}", bucket, ex);
                return;
            }
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created object storage bucket {}", bucket);
            } catch (RuntimeException createFailure) {
                log.error("Failed to create object storage bucket {}", bucket, createFailure);
            }
        } catch (RuntimeException ex) {
            log.error("Failed to reach object storage while checking bucket {}", bucket, ex);
        }
    }
}
