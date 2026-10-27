package com.luxora.commerce.admin.api;

import com.luxora.commerce.admin.dto.AdminAnalyticsOverviewResponse;
import com.luxora.commerce.admin.dto.RevenueTimelinePoint;
import com.luxora.commerce.admin.dto.TopSellingProductResponse;
import com.luxora.commerce.admin.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Admin Analytics", description = "Endpoints for business intelligence, revenue metrics, and top selling products")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview", description = "Returns financial KPIs, orders by status, and customer stats")
    public AdminAnalyticsOverviewResponse getOverview() {
        return analyticsService.getOverview();
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get revenue timeline", description = "Returns time-series revenue and order volume for 7d, 30d, or 12m")
    public List<RevenueTimelinePoint> getTimeline(@RequestParam(defaultValue = "7d") String period) {
        return analyticsService.getTimeline(period);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Get top selling products", description = "Returns top performing products ranked by quantity sold and revenue")
    public List<TopSellingProductResponse> getTopProducts(@RequestParam(defaultValue = "5") int limit) {
        return analyticsService.getTopSellingProducts(limit);
    }
}
