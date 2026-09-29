package com.luxora.commerce.catalog.api;

import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.catalog.dto.ProductDetailResponse;
import com.luxora.commerce.catalog.dto.ProductListResponse;
import com.luxora.commerce.catalog.service.CatalogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products")
class ProductController {

    private final CatalogQueryService catalogQueryService;

    ProductController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping
    @Operation(summary = "List products", description = "Search and filter active products with pagination.")
    PageResponse<ProductListResponse> listProducts(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Allowed values: newest, priceAsc, priceDesc, nameAsc")
            @RequestParam(defaultValue = "newest") @Pattern(regexp = "newest|priceAsc|priceDesc|nameAsc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) @PositiveOrZero BigDecimal minPrice,
            @RequestParam(required = false) @PositiveOrZero BigDecimal maxPrice) {
        return catalogQueryService.findProducts(page, size, sort, search, category, brand, minPrice, maxPrice);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product detail", description = "Returns product details, imagery, specifications, variants, and stock availability.")
    ProductDetailResponse getProduct(@PathVariable String slug) {
        return catalogQueryService.findProductBySlug(slug);
    }
}
