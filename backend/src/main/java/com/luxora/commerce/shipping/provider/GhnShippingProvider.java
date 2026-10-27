package com.luxora.commerce.shipping.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.shipping.config.GhnProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("ghnShippingProvider")
public class GhnShippingProvider implements ShippingProvider {

    private static final Logger log = LoggerFactory.getLogger(GhnShippingProvider.class);
    private static final String CARRIER_CODE = "GHN";
    private static final String TRACKING_PORTAL_URL = "https://donhang.ghn.vn/?order_code=";

    private final GhnProperties properties;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public GhnShippingProvider(GhnProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        RestClient.Builder builder = restClientBuilder != null ? restClientBuilder : RestClient.builder();
        this.restClient = builder
                .baseUrl(properties.apiUrl())
                .defaultHeader("Token", properties.token())
                .defaultHeader("ShopId", properties.shopId())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GhnShippingProvider(GhnProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public String getCarrierCode() {
        return CARRIER_CODE;
    }

    @Override
    public ShippingOrderResult createShippingOrder(ShippingOrderRequest request) {
        Map<String, Object> body = buildGhnCreateOrderPayload(request);
        try {
            GhnCreateOrderResponse response = restClient.post()
                    .uri("/v2/shipping-order/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GhnCreateOrderResponse.class);

            if (response == null || response.code() != 200 || response.data() == null) {
                String errorMsg = response != null ? response.message() : "Unknown GHN error";
                log.warn("GHN API returned error response: {}", errorMsg);
                throw new BadRequestException("GHN_ORDER_CREATION_FAILED", "GHN creation failed: " + errorMsg);
            }

            String orderCode = response.data().orderCode();
            BigDecimal fee = response.data().totalFee() != null
                    ? BigDecimal.valueOf(response.data().totalFee())
                    : BigDecimal.ZERO;
            Instant expectedDelivery = response.data().expectedDeliveryTime() != null
                    ? response.data().expectedDeliveryTime()
                    : Instant.now().plus(3, ChronoUnit.DAYS);

            return new ShippingOrderResult(
                    CARRIER_CODE,
                    orderCode,
                    buildTrackingUrl(orderCode),
                    fee,
                    expectedDelivery);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Failed to connect to GHN shipping service: {}", ex.getMessage());
            throw new BadRequestException("GHN_SERVICE_UNAVAILABLE", "Failed to communicate with GHN: " + ex.getMessage());
        }
    }

    @Override
    public String buildTrackingUrl(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return TRACKING_PORTAL_URL + trackingNumber.trim();
    }

    private Map<String, Object> buildGhnCreateOrderPayload(ShippingOrderRequest request) {
        String fullAddress = (request.addressLine1() + (request.addressLine2() != null ? " " + request.addressLine2() : "")).trim();
        List<Map<String, Object>> items = request.itemNames().isEmpty()
                ? List.of(Map.of("name", "Standard LUXORA Package", "quantity", 1, "price", 50000))
                : request.itemNames().stream()
                        .map(name -> Map.<String, Object>of(
                                "name", name,
                                "quantity", 1,
                                "price", 100000))
                        .toList();

        return Map.ofEntries(
                Map.entry("payment_type_id", 1), // 1: Sender pays shipping
                Map.entry("note", "LUXORA Order " + request.orderId()),
                Map.entry("required_note", "CHOXEMHANGKHONGTHU"),
                Map.entry("return_phone", properties.clientPhone()),
                Map.entry("return_address", "LUXORA Distribution Center, District 1, HCMC"),
                Map.entry("to_name", request.recipientName()),
                Map.entry("to_phone", request.recipientPhone()),
                Map.entry("to_address", fullAddress),
                Map.entry("to_ward_code", properties.fromWardCode()), // Default ward if unmapped
                Map.entry("to_district_id", properties.fromDistrictId()), // Default district if unmapped
                Map.entry("cod_amount", request.codAmount() != null ? request.codAmount().intValue() : 0),
                Map.entry("content", "LUXORA Luxury Order"),
                Map.entry("weight", request.weightGrams() != null ? request.weightGrams() : 500),
                Map.entry("length", 15),
                Map.entry("width", 15),
                Map.entry("height", 10),
                Map.entry("service_type_id", 2), // Standard e-commerce
                Map.entry("items", items));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GhnCreateOrderResponse(
            int code,
            String message,
            GhnOrderData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GhnOrderData(
            @JsonProperty("order_code") String orderCode,
            @JsonProperty("total_fee") Integer totalFee,
            @JsonProperty("expected_delivery_time") Instant expectedDeliveryTime) {
    }
}
