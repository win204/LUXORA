package com.luxora.commerce.catalog.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("local")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:luxora-catalog-seed-test;MODE=MSSQLServer;DATABASE_TO_UPPER=false;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
class CatalogSeedApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void productListReturnsSeededProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.slug == 'aerophone-x1')]").exists())
                .andExpect(jsonPath("$.content[?(@.slug == 'studio-one')]").exists())
                .andExpect(jsonPath("$.content[?(@.slug == 'vision-slate')]").exists());
    }

    @Test
    void productDetailReturnsSeededInventoryAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/products/aerophone-x1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AeroPhone X1"))
                .andExpect(jsonPath("$.brand.slug").value("aurora-devices"))
                .andExpect(jsonPath("$.category.slug").value("phones"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.specifications.length()").value(3))
                .andExpect(jsonPath("$.variants.length()").value(3))
                .andExpect(jsonPath("$.variants[?(@.sku == 'AUR-X1-GRF-128' && @.inStock == true)]").exists())
                .andExpect(jsonPath("$.variants[?(@.sku == 'AUR-X1-BLU-512' && @.inStock == false)]").exists());
    }

    @Test
    void categoriesReturnSeededValues() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'phones')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'audio')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'tablets')]").exists());
    }

    @Test
    void brandsReturnSeededValues() throws Exception {
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'aurora-devices')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'atelier-sound')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'lumen-works')]").exists());
    }

    @Test
    void searchFilterReturnsMatchingProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("search", "studio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("studio-one"));
    }

    @Test
    void categoryFilterReturnsMatchingProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("category", "tablets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("vision-slate"));
    }

    @Test
    void brandFilterReturnsMatchingProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("brand", "aurora-devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("aerophone-x1"));
    }

    @Test
    void priceFilterReturnsMatchingProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("minPrice", "700.00")
                        .param("maxPrice", "900.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.slug == 'aerophone-x1')]").exists())
                .andExpect(jsonPath("$.content[?(@.slug == 'vision-slate')]").exists());
    }
}
