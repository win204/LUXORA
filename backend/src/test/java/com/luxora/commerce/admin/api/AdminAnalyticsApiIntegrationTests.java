package com.luxora.commerce.admin.api;

import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.repository.ProductRepository;
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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminAnalyticsApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("delete from loyalty_transactions");
        jdbcTemplate.update("delete from loyalty_accounts");
        jdbcTemplate.update("delete from return_status_history");
        jdbcTemplate.update("delete from refunds");
        jdbcTemplate.update("delete from return_items");
        jdbcTemplate.update("delete from returns");
        jdbcTemplate.update("delete from order_status_history");
        jdbcTemplate.update("delete from payments");
        jdbcTemplate.update("delete from shipments");
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
    }

    @Test
    void getOverview_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_nonAdmin_returnsForbidden() throws Exception {
        AuthTokens customer = registerUser("analytics-customer@example.com");

        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOverview_admin_calculatesRevenueExcludingCancelled() throws Exception {
        String adminToken = loginAdmin();
        AuthTokens customer = registerUser("analytics-shopper@example.com");
        UUID userId = UUID.fromString(customer.userId());

        // Insert DELIVERED order: $300.00
        UUID order1Id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into orders (id, user_id, status, recipient_name, phone, address_line1, city, province, country, postal_code, subtotal, shipping_fee, tax, discount, grand_total, currency, customer_email, created_at, updated_at)
                values (?, ?, 'DELIVERED', 'Shopper 1', '0901234567', '123 St', 'HCM', 'HCM', 'VN', '700000', 300.00, 0.00, 0.00, 0.00, 300.00, 'USD', 'analytics-shopper@example.com', ?, ?)
                """, order1Id, userId, Instant.now(), Instant.now());

        // Insert PAID order: $200.00
        UUID order2Id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into orders (id, user_id, status, recipient_name, phone, address_line1, city, province, country, postal_code, subtotal, shipping_fee, tax, discount, grand_total, currency, customer_email, created_at, updated_at)
                values (?, ?, 'PAID', 'Shopper 2', '0901234567', '123 St', 'HCM', 'HCM', 'VN', '700000', 200.00, 0.00, 0.00, 0.00, 200.00, 'USD', 'analytics-shopper@example.com', ?, ?)
                """, order2Id, userId, Instant.now(), Instant.now());

        // Insert CANCELLED order: $500.00 (MUST NOT count towards total revenue)
        UUID order3Id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into orders (id, user_id, status, recipient_name, phone, address_line1, city, province, country, postal_code, subtotal, shipping_fee, tax, discount, grand_total, currency, customer_email, created_at, updated_at)
                values (?, ?, 'CANCELLED', 'Shopper 3', '0901234567', '123 St', 'HCM', 'HCM', 'VN', '700000', 500.00, 0.00, 0.00, 0.00, 500.00, 'USD', 'analytics-shopper@example.com', ?, ?)
                """, order3Id, userId, Instant.now(), Instant.now());

        // Overview endpoint:
        // Expected totalRevenue = 300 + 200 = 500.00
        // Expected completedOrders = 1 (DELIVERED)
        // Expected totalOrders = 3
        // Expected cancelledOrders = 1
        // Expected AOV = 500.00 / 2 = 250.00
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(500.0))
                .andExpect(jsonPath("$.todayRevenue").value(500.0))
                .andExpect(jsonPath("$.thisMonthRevenue").value(500.0))
                .andExpect(jsonPath("$.averageOrderValue").value(250.0))
                .andExpect(jsonPath("$.totalOrders").value(3))
                .andExpect(jsonPath("$.completedOrders").value(1))
                .andExpect(jsonPath("$.cancelledOrders").value(1))
                .andExpect(jsonPath("$.ordersByStatus.DELIVERED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.PAID").value(1))
                .andExpect(jsonPath("$.ordersByStatus.CANCELLED").value(1));
    }

    @Test
    void getTimeline_7d_returnsContiguousDaysWithZeroFilling() throws Exception {
        String adminToken = loginAdmin();
        AuthTokens customer = registerUser("analytics-timeline@example.com");
        UUID userId = UUID.fromString(customer.userId());

        // Order today
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into orders (id, user_id, status, recipient_name, phone, address_line1, city, province, country, postal_code, subtotal, shipping_fee, tax, discount, grand_total, currency, customer_email, created_at, updated_at)
                values (?, ?, 'DELIVERED', 'Timeline User', '0901234567', '123 St', 'HCM', 'HCM', 'VN', '700000', 150.00, 0.00, 0.00, 0.00, 150.00, 'USD', 'analytics-timeline@example.com', ?, ?)
                """, orderId, userId, Instant.now(), Instant.now());

        var result = mockMvc.perform(get("/api/v1/admin/analytics/timeline?period=7d")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andReturn();

        JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());
        // Last point is today, should have revenue 150.00 and orderCount 1
        JsonNode todayPoint = array.get(6);
        assertThat(todayPoint.get("revenue").asDouble()).isEqualTo(150.0);
        assertThat(todayPoint.get("orderCount").asInt()).isEqualTo(1);
    }

    @Test
    void getTopProducts_ranksBestSellersAccurately() throws Exception {
        String adminToken = loginAdmin();
        AuthTokens customer = registerUser("analytics-topproducts@example.com");
        UUID userId = UUID.fromString(customer.userId());

        Product product1 = productRepository.findBySlug("aerophone-x1")
                .orElseGet(() -> productRepository.findAll().get(0));

        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into orders (id, user_id, status, recipient_name, phone, address_line1, city, province, country, postal_code, subtotal, shipping_fee, tax, discount, grand_total, currency, customer_email, created_at, updated_at)
                values (?, ?, 'DELIVERED', 'Top Buyer', '0901234567', '123 St', 'HCM', 'HCM', 'VN', '700000', 1798.00, 0.00, 0.00, 0.00, 1798.00, 'USD', 'analytics-topproducts@example.com', ?, ?)
                """, orderId, userId, Instant.now(), Instant.now());

        jdbcTemplate.update("""
                insert into order_items (id, order_id, product_id, variant_id, product_slug, product_name, sku, unit_price, quantity, line_total, created_at)
                values (?, ?, ?, ?, ?, ?, 'SKU-TOP-1', 899.00, 2, 1798.00, ?)
                """, UUID.randomUUID(), orderId, product1.getId(), UUID.randomUUID(), product1.getSlug(), product1.getName(), Instant.now());

        mockMvc.perform(get("/api/v1/admin/analytics/top-products?limit=5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(product1.getId().toString()))
                .andExpect(jsonPath("$[0].productName").value(product1.getName()))
                .andExpect(jsonPath("$[0].totalQuantitySold").value(2))
                .andExpect(jsonPath("$[0].totalRevenue").value(1798.0));
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Analytics",
                                "lastName", "Tester"))))
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
