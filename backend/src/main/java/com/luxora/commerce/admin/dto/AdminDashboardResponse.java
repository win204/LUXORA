package com.luxora.commerce.admin.dto;

public record AdminDashboardResponse(
        long totalProducts,
        long totalVariants,
        long totalOrders,
        long pendingOrders,
        long paidOrders) {
}