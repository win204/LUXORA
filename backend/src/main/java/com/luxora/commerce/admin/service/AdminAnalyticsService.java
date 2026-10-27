package com.luxora.commerce.admin.service;

import com.luxora.commerce.admin.dto.AdminAnalyticsOverviewResponse;
import com.luxora.commerce.admin.dto.RevenueTimelinePoint;
import com.luxora.commerce.admin.dto.TopSellingProductResponse;
import com.luxora.commerce.loyalty.repository.LoyaltyAccountRepository;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.repository.OrderItemRepository;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminAnalyticsService {

    private static final List<OrderStatus> REVENUE_STATUSES = List.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;

    public AdminAnalyticsService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            LoyaltyAccountRepository loyaltyAccountRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
    }

    public AdminAnalyticsOverviewResponse getOverview() {
        BigDecimal totalRevenue = orderRepository.sumGrandTotalByStatusIn(REVENUE_STATUSES);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal todayRevenue = orderRepository.sumGrandTotalByStatusInAndCreatedAtAfter(REVENUE_STATUSES, todayStart);
        if (todayRevenue == null) {
            todayRevenue = BigDecimal.ZERO;
        }

        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal thisMonthRevenue = orderRepository.sumGrandTotalByStatusInAndCreatedAtAfter(REVENUE_STATUSES, monthStart);
        if (thisMonthRevenue == null) {
            thisMonthRevenue = BigDecimal.ZERO;
        }

        long qualifyingOrders = orderRepository.countByStatusIn(REVENUE_STATUSES);
        BigDecimal averageOrderValue = qualifyingOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(qualifyingOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalOrders = orderRepository.count();
        long completedOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long shippedOrders = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        long totalCustomers = userRepository.count();
        long newCustomersThisMonth = userRepository.countByCreatedAtAfter(monthStart);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            ordersByStatus.put(st.name(), 0L);
        }
        for (Object[] row : orderRepository.countOrdersByStatusGrouped()) {
            if (row[0] != null) {
                OrderStatus st = (OrderStatus) row[0];
                long count = ((Number) row[1]).longValue();
                ordersByStatus.put(st.name(), count);
            }
        }

        Map<String, Long> customersByTier = new LinkedHashMap<>();
        customersByTier.put("SILVER", 0L);
        customersByTier.put("GOLD", 0L);
        customersByTier.put("PLATINUM", 0L);
        for (Object[] row : loyaltyAccountRepository.countByTierGrouped()) {
            if (row[0] != null) {
                String tier = row[0].toString();
                long count = ((Number) row[1]).longValue();
                customersByTier.put(tier, count);
            }
        }

        return new AdminAnalyticsOverviewResponse(
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                todayRevenue.setScale(2, RoundingMode.HALF_UP),
                thisMonthRevenue.setScale(2, RoundingMode.HALF_UP),
                averageOrderValue.setScale(2, RoundingMode.HALF_UP),
                totalOrders,
                completedOrders,
                pendingOrders,
                processingOrders,
                shippedOrders,
                cancelledOrders,
                totalCustomers,
                newCustomersThisMonth,
                ordersByStatus,
                customersByTier
        );
    }

    public List<RevenueTimelinePoint> getTimeline(String period) {
        String mode = (period != null) ? period.trim().toLowerCase() : "7d";

        if ("12m".equals(mode)) {
            return buildMonthlyTimeline(12);
        } else if ("30d".equals(mode)) {
            return buildDailyTimeline(30);
        } else {
            return buildDailyTimeline(7);
        }
    }

    private List<RevenueTimelinePoint> buildDailyTimeline(int days) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDay = today.minusDays(days - 1);
        Instant since = startDay.atStartOfDay(ZoneOffset.UTC).toInstant();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, RevenueTimelinePoint> pointsMap = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            String dateStr = startDay.plusDays(i).format(dtf);
            pointsMap.put(dateStr, new RevenueTimelinePoint(dateStr, BigDecimal.ZERO, 0));
        }

        List<Object[]> rawOrders = orderRepository.findTimelineOrders(REVENUE_STATUSES, since);
        for (Object[] row : rawOrders) {
            Instant createdAt = (Instant) row[0];
            BigDecimal grandTotal = (BigDecimal) row[1];
            if (createdAt == null || grandTotal == null) continue;

            String dateStr = createdAt.atZone(ZoneOffset.UTC).toLocalDate().format(dtf);
            RevenueTimelinePoint existing = pointsMap.get(dateStr);
            if (existing != null) {
                pointsMap.put(dateStr, new RevenueTimelinePoint(
                        dateStr,
                        existing.revenue().add(grandTotal).setScale(2, RoundingMode.HALF_UP),
                        existing.orderCount() + 1
                ));
            }
        }

        return new ArrayList<>(pointsMap.values());
    }

    private List<RevenueTimelinePoint> buildMonthlyTimeline(int months) {
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        YearMonth startMonth = current.minusMonths(months - 1);
        Instant since = startMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, RevenueTimelinePoint> pointsMap = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            String monthStr = startMonth.plusMonths(i).format(ymFmt);
            pointsMap.put(monthStr, new RevenueTimelinePoint(monthStr, BigDecimal.ZERO, 0));
        }

        List<Object[]> rawOrders = orderRepository.findTimelineOrders(REVENUE_STATUSES, since);
        for (Object[] row : rawOrders) {
            Instant createdAt = (Instant) row[0];
            BigDecimal grandTotal = (BigDecimal) row[1];
            if (createdAt == null || grandTotal == null) continue;

            String monthStr = YearMonth.from(createdAt.atZone(ZoneOffset.UTC)).format(ymFmt);
            RevenueTimelinePoint existing = pointsMap.get(monthStr);
            if (existing != null) {
                pointsMap.put(monthStr, new RevenueTimelinePoint(
                        monthStr,
                        existing.revenue().add(grandTotal).setScale(2, RoundingMode.HALF_UP),
                        existing.orderCount() + 1
                ));
            }
        }

        return new ArrayList<>(pointsMap.values());
    }

    public List<TopSellingProductResponse> getTopSellingProducts(int limit) {
        int safeLimit = Math.min(50, Math.max(1, limit));
        List<Object[]> rows = orderItemRepository.findTopSellingProducts(REVENUE_STATUSES, PageRequest.of(0, safeLimit));

        List<TopSellingProductResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            UUID productId = (UUID) row[0];
            String productName = (String) row[1];
            String productSlug = (String) row[2];
            String imageUrl = (String) row[3];
            long totalQuantity = ((Number) row[4]).longValue();
            BigDecimal totalRevenue = ((BigDecimal) row[5]).setScale(2, RoundingMode.HALF_UP);

            result.add(new TopSellingProductResponse(
                    productId,
                    productName,
                    productSlug,
                    imageUrl,
                    totalQuantity,
                    totalRevenue
            ));
        }
        return result;
    }
}
