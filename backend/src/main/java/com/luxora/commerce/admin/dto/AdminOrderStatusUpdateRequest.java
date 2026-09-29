package com.luxora.commerce.admin.dto;

import com.luxora.commerce.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdminOrderStatusUpdateRequest(
        @NotNull OrderStatus status) {
}