package com.luxora.commerce.catalog.api;

import com.luxora.commerce.catalog.dto.BrandResponse;
import com.luxora.commerce.catalog.dto.CategoryResponse;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.catalog.dto.ProductDetailResponse;
import com.luxora.commerce.catalog.dto.ProductImageResponse;
import com.luxora.commerce.catalog.dto.ProductListResponse;
import com.luxora.commerce.catalog.dto.ProductSpecificationResponse;
import com.luxora.commerce.catalog.dto.ProductVariantResponse;
import com.luxora.commerce.catalog.service.CatalogQueryService;
import com.luxora.commerce.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProductController.class, CategoryController.class, BrandController.class})
class CatalogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogQueryService catalogQueryService;

    @Test
    void productListReturnsPage() throws Exception {
        when(catalogQueryService.findProducts(0, 20, "newest", null, null, null, null, null))
                .thenReturn(new PageResponse<>(List.of(productListResponse()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("luxora-phone"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void productListAcceptsPagination() throws Exception {
        when(catalogQueryService.findProducts(2, 10, "newest", null, null, null, null, null))
                .thenReturn(new PageResponse<>(List.of(), 2, 10, 42, 5));

        mockMvc.perform(get("/api/v1/products").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void productListAcceptsSearch() throws Exception {
        when(catalogQueryService.findProducts(0, 20, "newest", "phone", null, null, null, null))
                .thenReturn(new PageResponse<>(List.of(productListResponse()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products").param("search", "phone"))
                .andExpect(status().isOk());
    }

    @Test
    void productListAcceptsCategoryFilter() throws Exception {
        when(catalogQueryService.findProducts(0, 20, "newest", null, "phones", null, null, null))
                .thenReturn(new PageResponse<>(List.of(productListResponse()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products").param("category", "phones"))
                .andExpect(status().isOk());
    }

    @Test
    void productListAcceptsBrandFilter() throws Exception {
        when(catalogQueryService.findProducts(0, 20, "newest", null, null, "luxora", null, null))
                .thenReturn(new PageResponse<>(List.of(productListResponse()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products").param("brand", "luxora"))
                .andExpect(status().isOk());
    }

    @Test
    void productListAcceptsPriceFilter() throws Exception {
        when(catalogQueryService.findProducts(
                        eq(0),
                        eq(20),
                        eq("newest"),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(new BigDecimal("100.00")),
                        eq(new BigDecimal("900.00"))))
                .thenReturn(new PageResponse<>(List.of(productListResponse()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/products")
                        .param("minPrice", "100.00")
                        .param("maxPrice", "900.00"))
                .andExpect(status().isOk());
    }

    @Test
    void productListRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void productDetailReturnsProduct() throws Exception {
        when(catalogQueryService.findProductBySlug("luxora-phone")).thenReturn(productDetailResponse());

        mockMvc.perform(get("/api/v1/products/luxora-phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("luxora-phone"))
                .andExpect(jsonPath("$.variants[0].sku").value("LUX-PHONE-BLK-256"))
                .andExpect(jsonPath("$.variants[0].inStock").value(true));
    }

    @Test
    void unknownSlugReturnsConsistentError() throws Exception {
        when(catalogQueryService.findProductBySlug("missing"))
                .thenThrow(new NotFoundException("PRODUCT_NOT_FOUND", "Product not found"));

        mockMvc.perform(get("/api/v1/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/products/missing"));
    }

    @Test
    void categoriesReturnsList() throws Exception {
        when(catalogQueryService.findCategories()).thenReturn(List.of(categoryResponse()));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("phones"));
    }

    @Test
    void brandsReturnsList() throws Exception {
        when(catalogQueryService.findBrands()).thenReturn(List.of(brandResponse()));

        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("luxora"));
    }

    @Test
    void productListPassesSortParameter() throws Exception {
        when(catalogQueryService.findProducts(0, 20, "priceAsc", null, null, null, null, null))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/products").param("sort", "priceAsc"))
                .andExpect(status().isOk());

        verify(catalogQueryService).findProducts(0, 20, "priceAsc", null, null, null, null, null);
    }

    private ProductListResponse productListResponse() {
        return new ProductListResponse(
                UUID.randomUUID(),
                "LUXORA Phone",
                "luxora-phone",
                "Minimal flagship",
                brandResponse(),
                categoryResponse(),
                new BigDecimal("799.00"),
                true);
    }

    private ProductDetailResponse productDetailResponse() {
        return new ProductDetailResponse(
                UUID.randomUUID(),
                "LUXORA Phone",
                "luxora-phone",
                "Minimal flagship",
                "A polished commerce placeholder product.",
                brandResponse(),
                categoryResponse(),
                List.of(new ProductImageResponse(UUID.randomUUID(), "https://example.com/phone.jpg", "Phone", 1)),
                List.of(new ProductSpecificationResponse(UUID.randomUUID(), "Display", "6.3 inch", 1)),
                List.of(new ProductVariantResponse(
                        UUID.randomUUID(),
                        "LUX-PHONE-BLK-256",
                        "Black",
                        "256GB",
                        new BigDecimal("799.00"),
                        true)));
    }

    private BrandResponse brandResponse() {
        return new BrandResponse(UUID.randomUUID(), "LUXORA", "luxora");
    }

    private CategoryResponse categoryResponse() {
        return new CategoryResponse(UUID.randomUUID(), "Phones", "phones");
    }
}
