package com.luxora.commerce.aws.s3.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUploadUrlRequest(
        @NotBlank(message = "Filename is required")
        String filename,

        @NotBlank(message = "Content type is required")
        String contentType,

        String folder
) {}
