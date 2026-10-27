package com.luxora.commerce.shipping.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.order.shipment.dto.ShipmentResponse;
import com.luxora.commerce.shipping.config.GhnProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

class GhnShippingProviderUnitTests {

    private GhnProperties properties;
    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private GhnShippingProvider provider;
    private MockShippingProvider mockShippingProvider;

    @BeforeEach
    void setUp() {
        properties = new GhnProperties(
                "https://dev-online-gateway.ghn.vn/shiip/public-api",
                "test-ghn-token",
                "test-shop-id",
                1442,
                "20101",
                "0901234567",
                "test-secret-webhook");

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.apiUrl())
                .defaultHeader("Token", properties.token())
                .defaultHeader("ShopId", properties.shopId());

        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        provider = new GhnShippingProvider(properties, restClient);
        mockShippingProvider = new MockShippingProvider();
    }

    @Test
    @DisplayName("GhnShippingProvider returns carrier code GHN and generates valid tracking URL")
    void ghnCarrierCodeAndTrackingUrl() {
        assertThat(provider.getCarrierCode()).isEqualTo("GHN");
        assertThat(provider.buildTrackingUrl("L598M9P6")).isEqualTo("https://donhang.ghn.vn/?order_code=L598M9P6");
        assertThat(provider.buildTrackingUrl(null)).isNull();
        assertThat(provider.buildTrackingUrl("  ")).isNull();

        assertThat(ShipmentResponse.resolveTrackingUrl("GHN", "L598M9P6")).isEqualTo("https://donhang.ghn.vn/?order_code=L598M9P6");
        assertThat(ShipmentResponse.resolveTrackingUrl("GiaoHangNhanh", "L598M9P6")).isEqualTo("https://donhang.ghn.vn/?order_code=L598M9P6");
        assertThat(ShipmentResponse.resolveTrackingUrl("DHL", "DHL123")).isNull();
    }

    @Test
    @DisplayName("MockShippingProvider creates mock tracking number and valid result")
    void mockShippingProviderSuccess() {
        assertThat(mockShippingProvider.getCarrierCode()).isEqualTo("MOCK");
        ShippingOrderRequest request = new ShippingOrderRequest(
                UUID.randomUUID(),
                "John Doe",
                "0901234567",
                "123 Nguyen Hue",
                null,
                "District 1",
                "Ho Chi Minh",
                "Vietnam",
                "70000",
                500,
                BigDecimal.ZERO,
                List.of("Product A"));

        ShippingOrderResult result = mockShippingProvider.createShippingOrder(request);
        assertThat(result.carrier()).isEqualTo("MOCK");
        assertThat(result.trackingNumber()).startsWith("MOCK-");
        assertThat(result.expectedDeliveryDate()).isNotNull();
    }

    @Test
    @DisplayName("GhnShippingProvider successfully creates shipping order when GHN responds 200 OK")
    void ghnCreateShippingOrderSuccess() {
        String ghnResponseJson = """
                {
                    "code": 200,
                    "message": "Success",
                    "data": {
                        "order_code": "GHN-TEST-12345",
                        "total_fee": 28000,
                        "expected_delivery_time": "2026-10-25T17:00:00Z"
                    }
                }
                """;

        mockServer.expect(MockRestRequestMatchers.requestTo("https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("Token", "test-ghn-token"))
                .andExpect(MockRestRequestMatchers.header("ShopId", "test-shop-id"))
                .andRespond(MockRestResponseCreators.withSuccess(ghnResponseJson, MediaType.APPLICATION_JSON));

        ShippingOrderRequest request = new ShippingOrderRequest(
                UUID.randomUUID(),
                "Nguyen Van A",
                "0987654321",
                "456 Le Loi",
                null,
                "Ben Nghe",
                "Ho Chi Minh",
                "Vietnam",
                "70000",
                500,
                BigDecimal.ZERO,
                List.of("AeroPhone X1"));

        ShippingOrderResult result = provider.createShippingOrder(request);

        mockServer.verify();
        assertThat(result.carrier()).isEqualTo("GHN");
        assertThat(result.trackingNumber()).isEqualTo("GHN-TEST-12345");
        assertThat(result.trackingUrl()).isEqualTo("https://donhang.ghn.vn/?order_code=GHN-TEST-12345");
        assertThat(result.fee()).isEqualByComparingTo(BigDecimal.valueOf(28000));
    }

    @Test
    @DisplayName("GhnShippingProvider throws BadRequestException when GHN returns API error")
    void ghnCreateShippingOrderFailure() {
        String errorJson = """
                {
                    "code": 400,
                    "message": "To ward code is invalid",
                    "data": null
                }
                """;

        mockServer.expect(MockRestRequestMatchers.requestTo("https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(errorJson, MediaType.APPLICATION_JSON));

        ShippingOrderRequest request = new ShippingOrderRequest(
                UUID.randomUUID(),
                "Nguyen Van A",
                "0987654321",
                "456 Le Loi",
                null,
                "Ben Nghe",
                "Ho Chi Minh",
                "Vietnam",
                "70000",
                500,
                BigDecimal.ZERO,
                List.of());

        assertThatThrownBy(() -> provider.createShippingOrder(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("To ward code is invalid");

        mockServer.verify();
    }
}
