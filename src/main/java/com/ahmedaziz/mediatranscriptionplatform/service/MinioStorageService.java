package com.ahmedaziz.mediatranscriptionplatform.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error initializing MinIO bucket", e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String objectName = generateObjectName(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded successfully: {}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new IOException("Failed to upload file to MinIO", e);
        }
    }

    public String uploadFile(InputStream inputStream, String filename, String contentType, long size) throws IOException {
        String objectName = generateObjectName(filename);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("File uploaded successfully: {}", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new IOException("Failed to upload file to MinIO", e);
        }
    }

    public InputStream downloadFile(String objectName) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file from MinIO: {}", objectName, e);
            throw new IOException("Failed to download file from MinIO", e);
        }
    }

    public byte[] downloadFileAsBytes(String objectName) throws IOException {
        try (InputStream stream = downloadFile(objectName)) {
            return stream.readAllBytes();
        }
    }

    public void deleteFile(String objectName) throws IOException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("File deleted successfully: {}", objectName);
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}", objectName, e);
            throw new IOException("Failed to delete file from MinIO", e);
        }
    }

    public String getPresignedUrl(String objectName, int expirationMinutes) throws IOException {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}", objectName, e);
            throw new IOException("Failed to generate presigned URL", e);
        }
    }

    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String generateObjectName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public String getBucketName() {
        return bucketName;
    }
}