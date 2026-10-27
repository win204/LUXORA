package com.luxora.commerce.aws.s3.api;

import com.luxora.commerce.aws.s3.S3StorageService;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlRequest;
import com.luxora.commerce.aws.s3.dto.PresignedUploadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "AWS S3 Media and File Upload APIs")
public class MediaController {

    private final S3StorageService storageService;

    public MediaController(S3StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/presigned-upload-url")
    @Operation(summary = "Generate a presigned S3 upload URL for direct client file uploads")
    public ResponseEntity<PresignedUploadUrlResponse> getPresignedUploadUrl(
            @Valid @RequestBody PresignedUploadUrlRequest request) {
        PresignedUploadUrlResponse response = storageService.generatePresignedUploadUrl(request);
        return ResponseEntity.ok(response);
    }
}
