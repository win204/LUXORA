package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminAnalyticsOverviewResponse(
        BigDecimal totalRevenue,
        BigDecimal todayRevenue,
        BigDecimal thisMonthRevenue,
        BigDecimal averageOrderValue,
        long totalOrders,
        long completedOrders,
        long pendingOrders,
        long processingOrders,
        long shippedOrders,
        long cancelledOrders,
        long totalCustomers,
        long newCustomersThisMonth,
        Map<String, Long> ordersByStatus,
        Map<String, Long> customersByTier) {
}
