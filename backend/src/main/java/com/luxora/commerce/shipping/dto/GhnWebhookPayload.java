package com.luxora.commerce.shipping.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GhnWebhookPayload(
        @JsonAlias({"OrderCode", "order_code", "orderCode"})
        String orderCode,

        @JsonAlias({"Status", "status"})
        String status,

        @JsonAlias({"Time", "time"})
        String time,

        @JsonAlias({"Type", "type"})
        String type,

        @JsonAlias({"Warehouse", "warehouse"})
        String warehouse,

        @JsonAlias({"Reason", "reason"})
        String reason) {
}
