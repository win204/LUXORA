package com.luxora.commerce.catalog.api;

import com.luxora.commerce.catalog.dto.CategoryResponse;
import com.luxora.commerce.catalog.service.CatalogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
class CategoryController {

    private final CatalogQueryService catalogQueryService;

    CategoryController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping
    @Operation(summary = "List categories", description = "Returns all active catalog categories.")
    List<CategoryResponse> listCategories() {
        return catalogQueryService.findCategories();
    }
}
