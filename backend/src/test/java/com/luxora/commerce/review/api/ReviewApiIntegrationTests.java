package com.luxora.commerce.review.api;

import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.review.dto.ReviewRequest;
import com.luxora.commerce.review.dto.UpdateReviewStatusRequest;
import com.luxora.commerce.review.model.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ReviewApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product testProduct;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("delete from review_images");
        jdbcTemplate.update("delete from reviews");

        testProduct = productRepository.findBySlug("aerophone-x1")
                .orElseGet(() -> productRepository.findAll().stream().findFirst().orElseThrow());
    }

    @Test
    void submitReview_unauthenticated_returnsUnauthorized() throws Exception {
        ReviewRequest request = new ReviewRequest(5, "Great phone", "Loved the camera and screen quality.", List.of());

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitReview_authenticated_success() throws Exception {
        AuthTokens tokens = registerUser("reviewer-success@example.com");
        ReviewRequest request = new ReviewRequest(5, "Incredible device", "Battery life is unmatched.",
                List.of("https://s3.amazonaws.com/luxora-media/reviews/photo1.jpg"));

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Incredible device"))
                .andExpect(jsonPath("$.comment").value("Battery life is unmatched."))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.verifiedPurchase").value(false))
                .andExpect(jsonPath("$.imageUrls.length()").value(1))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://s3.amazonaws.com/luxora-media/reviews/photo1.jpg"));

        // Verify review is returned in public product reviews endpoint
        mockMvc.perform(get("/api/v1/products/{slug}/reviews", testProduct.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void submitReview_invalidRating_returnsBadRequest() throws Exception {
        AuthTokens tokens = registerUser("reviewer-invalid-rating@example.com");
        ReviewRequest request = new ReviewRequest(6, "Over the top", "Should fail validation", List.of());

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReview_blankComment_returnsBadRequest() throws Exception {
        AuthTokens tokens = registerUser("reviewer-blank-comment@example.com");
        ReviewRequest request = new ReviewRequest(4, "Title", "   ", List.of());

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReview_duplicateUserReview_updatesExistingReview() throws Exception {
        AuthTokens tokens = registerUser("reviewer-duplicate@example.com");
        ReviewRequest initialRequest = new ReviewRequest(4, "Good", "Initial comment", List.of());

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(initialRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));

        ReviewRequest updatedRequest = new ReviewRequest(5, "Excellent after update", "Updated review comment",
                List.of("https://s3.amazonaws.com/luxora-media/reviews/new_photo.jpg"));

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updatedRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Excellent after update"))
                .andExpect(jsonPath("$.comment").value("Updated review comment"))
                .andExpect(jsonPath("$.imageUrls.length()").value(1));

        // Ensure database still has only 1 review
        Integer count = jdbcTemplate.queryForObject("select count(*) from reviews where user_id = ?",
                Integer.class, UUID.fromString(tokens.userId()));
        assertThat(count).isEqualTo(1);
    }

    @Test
    void submitReview_verifiedPurchase_markedVerifiedWhenDeliveredOrderExists() throws Exception {
        AuthTokens tokens = registerUser("reviewer-verified@example.com");
        UUID userId = UUID.fromString(tokens.userId());

        // Create a delivered order with this product
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        jdbcTemplate.update("""
                insert into orders (id, user_id, status, recipient_name, phone, address_line1, city, province, country, postal_code, subtotal, shipping_fee, tax, discount, grand_total, currency, customer_email, created_at, updated_at)
                values (?, ?, 'DELIVERED', 'Test User', '0901234567', '123 St', 'HCM', 'HCM', 'VN', '700000', 100.00, 10.00, 5.00, 0.00, 115.00, 'USD', 'reviewer-verified@example.com', ?, ?)
                """, orderId, userId, Instant.now(), Instant.now());

        jdbcTemplate.update("""
                insert into order_items (id, order_id, product_id, variant_id, product_slug, product_name, sku, unit_price, quantity, line_total, created_at)
                values (?, ?, ?, ?, ?, ?, 'TEST-SKU', 100.00, 1, 100.00, ?)
                """, orderItemId, orderId, testProduct.getId(), variantId, testProduct.getSlug(), testProduct.getName(), Instant.now());

        ReviewRequest request = new ReviewRequest(5, "Verified buyer review", "Really great product!", List.of());

        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verifiedPurchase").value(true));
    }

    @Test
    void getProductReviewSummary_calculatesAverageAndDistribution() throws Exception {
        AuthTokens user1 = registerUser("reviewer-summary-1@example.com");
        AuthTokens user2 = registerUser("reviewer-summary-2@example.com");

        // Submit 5 stars
        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + user1.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(5, "Best ever", "Loved it", List.of()))))
                .andExpect(status().isCreated());

        // Submit 4 stars
        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + user2.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(4, "Pretty good", "Nice phone", List.of()))))
                .andExpect(status().isCreated());

        // Verify summary
        mockMvc.perform(get("/api/v1/products/{slug}/reviews/summary", testProduct.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.ratingDistribution.5").value(1))
                .andExpect(jsonPath("$.ratingDistribution.4").value(1))
                .andExpect(jsonPath("$.ratingDistribution.3").value(0))
                .andExpect(jsonPath("$.ratingDistribution.2").value(0))
                .andExpect(jsonPath("$.ratingDistribution.1").value(0));
    }

    @Test
    void getMyReview_returnsUserReviewWhenExists() throws Exception {
        AuthTokens tokens = registerUser("reviewer-my-review@example.com");

        // Initially no review -> 204 No Content
        mockMvc.perform(get("/api/v1/products/{id}/reviews/me", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNoContent());

        // Submit review
        mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(4, "My personal opinion", "Works fine", List.of()))))
                .andExpect(status().isCreated());

        // Now returns review
        mockMvc.perform(get("/api/v1/products/{id}/reviews/me", testProduct.getId())
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.title").value("My personal opinion"));
    }

    @Test
    void adminReviewModeration_canApproveOrRejectReview() throws Exception {
        AuthTokens customer = registerUser("reviewer-moderation@example.com");
        String adminToken = loginAdmin();

        // Customer posts review
        var result = mockMvc.perform(post("/api/v1/products/{id}/reviews", testProduct.getId())
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewRequest(1, "Spam review", "Contains inappropriate links", List.of()))))
                .andExpect(status().isCreated())
                .andReturn();

        String reviewId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Admin lists reviews
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(reviewId));

        // Admin rejects review
        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/status", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateReviewStatusRequest(ReviewStatus.REJECTED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Public review list should no longer show rejected review
        mockMvc.perform(get("/api/v1/products/{slug}/reviews", testProduct.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // Admin deletes review
        mockMvc.perform(delete("/api/v1/admin/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Admin list should now be empty
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void adminReviewModeration_nonAdmin_returnsForbidden() throws Exception {
        AuthTokens customer = registerUser("reviewer-non-admin@example.com");

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Reviewer",
                                "lastName", "Customer"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(node.get("accessToken").asText(), node.get("user").get("id").asText());
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

    private record AuthTokens(String accessToken, String userId) {
    }
}
