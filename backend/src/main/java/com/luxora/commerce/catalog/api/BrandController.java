package com.luxora.commerce.catalog.api;

import com.luxora.commerce.catalog.dto.BrandResponse;
import com.luxora.commerce.catalog.service.CatalogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
@Tag(name = "Brands")
class BrandController {

    private final CatalogQueryService catalogQueryService;

    BrandController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping
    @Operation(summary = "List brands", description = "Returns all active catalog brands.")
    List<BrandResponse> listBrands() {
        return catalogQueryService.findBrands();
    }
}
