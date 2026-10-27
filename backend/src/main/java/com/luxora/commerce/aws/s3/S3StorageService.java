package com.luxora.commerce.aws.s3;

import com.luxora.commerce.aws.config.AwsProperties;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlRequest;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlResponse;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.awsProperties = awsProperties;
    }

    public PresignedUploadUrlResponse generatePresignedUploadUrl(PresignedUploadUrlRequest request) {
        String bucket = awsProperties.getS3().getBucket();
        String folder = (request.folder() != null && !request.folder().isBlank())
                ? sanitizeFolder(request.folder())
                : "uploads";

        String extension = extractExtension(request.filename());
        String fileKey = folder + "/" + UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(awsProperties.getS3().getPresignedUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedRequest.url().toString();
        Instant expiresAt = presignedRequest.expiration();

        String publicUrl = resolvePublicUrl(bucket, fileKey);

        log.info("Generated presigned upload URL for key: {}, expiresAt: {}", fileKey, expiresAt);
        return new PresignedUploadUrlResponse(uploadUrl, fileKey, publicUrl, expiresAt);
    }

    public String uploadBytes(String key, byte[] content, String contentType) {
        String bucket = awsProperties.getS3().getBucket();
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(content));
        String publicUrl = resolvePublicUrl(bucket, key);
        log.info("Successfully uploaded object to S3: {}/{}", bucket, key);
        return publicUrl;
    }

    public void deleteFile(String key) {
        String bucket = awsProperties.getS3().getBucket();
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.info("Successfully deleted object from S3: {}/{}", bucket, key);
    }

    public String resolvePublicUrl(String bucket, String key) {
        if (awsProperties.getEndpoint() != null && !awsProperties.getEndpoint().isBlank()) {
            return awsProperties.getEndpoint() + "/" + bucket + "/" + key;
        }
        return "https://" + bucket + ".s3." + awsProperties.getRegion() + ".amazonaws.com/" + key;
    }

    private String sanitizeFolder(String folder) {
        String cleaned = folder.trim().replaceAll("^/+|/+$", "");
        return cleaned.replaceAll("[^a-zA-Z0-9-_/]", "");
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
