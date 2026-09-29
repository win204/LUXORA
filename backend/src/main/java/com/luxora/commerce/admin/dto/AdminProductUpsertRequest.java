package com.luxora.commerce.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminProductUpsertRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 220) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase words separated by hyphens") String slug,
        @Size(max = 240) String subtitle,
        @NotBlank @Size(max = 4000) String description,
        @NotNull UUID brandId,
        @NotNull UUID categoryId,
        boolean active) {
}