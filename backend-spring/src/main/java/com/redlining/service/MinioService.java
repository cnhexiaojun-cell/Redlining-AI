package com.redlining.service;

import com.redlining.config.AppProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final AppProperties appProperties;

    public MinioService(AppProperties appProperties) {
        this.appProperties = appProperties;
        AppProperties.Minio minio = appProperties.getMinio();
        this.minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    public void ensureBucket() {
        try {
            String bucket = appProperties.getMinio().getBucket();
            if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
            }
            String docsBucket = appProperties.getMinio().getDocumentsBucket();
            if (docsBucket != null && !docsBucket.isBlank() && !minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(docsBucket).build())) {
                minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(docsBucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket", e);
        }
    }

    public String putObject(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(appProperties.getMinio().getBucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to MinIO", e);
        }
    }

    public String getPresignedUrl(String objectKey, int expirySeconds) {
        return getPresignedUrlForBucket(appProperties.getMinio().getBucket(), objectKey, expirySeconds);
    }

    public String getPresignedUrlForBucket(String bucket, String objectKey, int expirySeconds) {
        if (objectKey == null || objectKey.isBlank() || bucket == null || bucket.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned URL", e);
        }
    }

    public String putObjectInBucket(String bucket, String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload to MinIO", e);
        }
    }

    public InputStream getObject(String bucket, String objectKey) {
        try {
            return minioClient.getObject(io.minio.GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object from MinIO", e);
        }
    }
}
