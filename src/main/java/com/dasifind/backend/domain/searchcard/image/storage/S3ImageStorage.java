package com.dasifind.backend.domain.searchcard.image.storage;

import com.dasifind.backend.domain.searchcard.image.config.S3Properties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
public class S3ImageStorage implements ImageStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public S3ImageStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            S3Properties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public void upload(String storageKey, String contentType, byte[] content) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (RuntimeException exception) {
            throw new ImageStorageException("Failed to upload image to S3", exception);
        }
    }

    @Override
    public String createDownloadUrl(String storageKey) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(properties.presignedUrlTtl())
                    .getObjectRequest(objectRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
        } catch (RuntimeException exception) {
            throw new ImageStorageException("Failed to create S3 image download URL", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (RuntimeException exception) {
            throw new ImageStorageException("Failed to delete image from S3", exception);
        }
    }
}
