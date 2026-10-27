package com.luxora.commerce.aws.s3.dto;

import java.time.Instant;

public record PresignedUploadUrlResponse(
        String uploadUrl,
        String fileKey,
        String publicUrl,
        Instant expiresAt
) {}
