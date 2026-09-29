package com.luxora.commerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Catalog category")
public record CategoryResponse(UUID id, String name, String slug) {
}
