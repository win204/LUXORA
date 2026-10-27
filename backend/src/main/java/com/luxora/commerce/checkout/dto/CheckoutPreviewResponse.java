package com.luxora.commerce.checkout.dto;

import com.luxora.commerce.promotion.dto.AppliedPromotionResponse;
import com.luxora.commerce.promotion.dto.AvailablePromotionResponse;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        List<CheckoutPreviewItemResponse> items,
        ShippingAddressResponse shippingAddress,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal grandTotal,
        String currency,
        AppliedPromotionResponse promotion,
        Integer loyaltyPointsRedeemed,
        BigDecimal loyaltyDiscount,
        List<AvailablePromotionResponse> availablePromotions) {

    public CheckoutPreviewResponse(
            List<CheckoutPreviewItemResponse> items,
            ShippingAddressResponse shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal grandTotal,
            String currency,
            AppliedPromotionResponse promotion,
            Integer loyaltyPointsRedeemed,
            BigDecimal loyaltyDiscount) {
        this(items, shippingAddress, subtotal, shippingFee, tax, discount, grandTotal, currency, promotion, loyaltyPointsRedeemed, loyaltyDiscount, List.of());
    }

    public CheckoutPreviewResponse(
            List<CheckoutPreviewItemResponse> items,
            ShippingAddressResponse shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal grandTotal,
            String currency,
            AppliedPromotionResponse promotion) {
        this(items, shippingAddress, subtotal, shippingFee, tax, discount, grandTotal, currency, promotion, 0, BigDecimal.ZERO, List.of());
    }
}