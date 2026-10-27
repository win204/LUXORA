package com.luxora.commerce.banner.api;

import com.luxora.commerce.banner.model.Banner;
import com.luxora.commerce.banner.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BannerApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BannerRepository bannerRepository;

    @BeforeEach
    void resetState() {
        bannerRepository.deleteAll();
    }

    @Test
    void getActiveBanners_public_returnsOnlyActiveBannersInOrder() throws Exception {
        Banner b1 = new Banner("Active Low Priority", "Sub 1", "https://example.com/b1.jpg", "/catalog", "Shop Now", 10, true, null, null);
        Banner b2 = new Banner("Active High Priority", "Sub 2", "https://example.com/b2.jpg", "/collections", "Discover", 1, true, null, null);
        Banner b3 = new Banner("Inactive Banner", "Sub 3", "https://example.com/b3.jpg", "/flash-sales", "View", 0, false, null, null);
        bannerRepository.saveAll(java.util.List.of(b1, b2, b3));

        mockMvc.perform(get("/api/v1/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Active High Priority"))
                .andExpect(jsonPath("$[1].title").value("Active Low Priority"));
    }

    @Test
    void adminList_nonAdmin_returnsForbidden() throws Exception {
        String customerToken = registerCustomer("banner-customer@example.com");

        mockMvc.perform(get("/api/v1/admin/banners")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCrudLifecycle_admin_worksCorrectly() throws Exception {
        String adminToken = loginAdmin();

        // 1. Create banner
        var createResult = mockMvc.perform(post("/api/v1/admin/banners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Haute Horlogerie Exhibition",
                                "subtitle", "Masterpieces of Swiss Watchmaking",
                                "imageUrl", "https://example.com/hero.jpg",
                                "linkUrl", "/collections/horlogerie",
                                "ctaText", "Explore Exhibition",
                                "displayOrder", 1,
                                "active", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Haute Horlogerie Exhibition"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andReturn();

        String bannerId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        UUID id = UUID.fromString(bannerId);

        // 2. Get by ID
        mockMvc.perform(get("/api/v1/admin/banners/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Haute Horlogerie Exhibition"));

        // 3. Update banner
        mockMvc.perform(put("/api/v1/admin/banners/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Updated Luxury Showcase",
                                "imageUrl", "https://example.com/hero-v2.jpg",
                                "displayOrder", 5,
                                "active", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Luxury Showcase"))
                .andExpect(jsonPath("$.displayOrder").value(5));

        // 4. Toggle status to inactive
        mockMvc.perform(patch("/api/v1/admin/banners/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(bannerRepository.findById(id).orElseThrow().isActive()).isFalse();

        // 5. Delete banner
        mockMvc.perform(delete("/api/v1/admin/banners/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(bannerRepository.findById(id)).isEmpty();
    }

    private String registerCustomer(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Banner",
                                "lastName", "Shopper"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String loginAdmin() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@luxora.com",
                                "password", "Admin123456!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
