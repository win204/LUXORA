package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;

public record RevenueTimelinePoint(
        String date,
        BigDecimal revenue,
        long orderCount) {
}
