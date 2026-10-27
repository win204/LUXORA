package com.luxora.commerce.loyalty.service;

import com.luxora.commerce.loyalty.dto.LoyaltyAccountResponse;
import com.luxora.commerce.loyalty.dto.LoyaltyTransactionResponse;
import com.luxora.commerce.loyalty.model.LoyaltyAccount;
import com.luxora.commerce.loyalty.model.LoyaltyTier;
import com.luxora.commerce.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface LoyaltyService {

    LoyaltyAccountResponse getOrCreateAccount(UUID userId);

    LoyaltyAccount getAccountEntity(UUID userId);

    int calculatePointsToAward(BigDecimal grandTotal, LoyaltyTier tier);

    void awardPointsOnDelivery(Order order);

    BigDecimal calculateLoyaltyDiscount(UUID userId, Integer pointsToRedeem, BigDecimal subtotal);

    void redeemPointsForOrder(UUID userId, int points, Order order, BigDecimal discount);

    void restorePointsForOrder(Order order);

    Page<LoyaltyTransactionResponse> getTransactions(UUID userId, Pageable pageable);

    LoyaltyAccountResponse adjustPoints(UUID userId, int points, String reason);
}
