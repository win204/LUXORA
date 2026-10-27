package com.luxora.commerce.flashsale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateFlashSaleRequest(
    @NotBlank(message = "Flash sale name is required")
    String name,

    String description,

    @NotNull(message = "Start time is required")
    OffsetDateTime startTime,

    @NotNull(message = "End time is required")
    OffsetDateTime endTime,

    String bannerImageUrl,

    @NotEmpty(message = "Flash sale must contain at least one item")
    @Valid
    List<CreateFlashSaleItemRequest> items
) {}
