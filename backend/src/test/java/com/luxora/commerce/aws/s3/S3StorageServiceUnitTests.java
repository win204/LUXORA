package com.luxora.commerce.aws.s3;

import com.luxora.commerce.aws.config.AwsProperties;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlRequest;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlResponse;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceUnitTests {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private AwsProperties properties;
    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new AwsProperties();
        properties.setRegion("ap-southeast-1");
        properties.getS3().setBucket("test-bucket");
        properties.getS3().setPresignedUrlExpiration(Duration.ofMinutes(15));
        storageService = new S3StorageService(s3Client, s3Presigner, properties);
    }

    @Test
    void generatePresignedUploadUrlSuccess() throws Exception {
        when(presignedPutObjectRequest.url()).thenReturn(URI.create("https://test-bucket.s3.amazonaws.com/upload?sig=123").toURL());
        when(presignedPutObjectRequest.expiration()).thenReturn(Instant.now().plusSeconds(900));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);

        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("iphone.png", "image/png", "products");
        PresignedUploadUrlResponse response = storageService.generatePresignedUploadUrl(request);

        assertThat(response.uploadUrl()).contains("https://test-bucket.s3.amazonaws.com/upload");
        assertThat(response.fileKey()).startsWith("products/").endsWith(".png");
        assertThat(response.publicUrl()).contains("test-bucket.s3.ap-southeast-1.amazonaws.com/products/");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void uploadBytesSuccess() {
        byte[] content = "test content".getBytes();
        String publicUrl = storageService.uploadBytes("documents/test.txt", content, "text/plain");

        assertThat(publicUrl).contains("test-bucket.s3.ap-southeast-1.amazonaws.com/documents/test.txt");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void deleteFileSuccess() {
        storageService.deleteFile("documents/test.txt");
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
