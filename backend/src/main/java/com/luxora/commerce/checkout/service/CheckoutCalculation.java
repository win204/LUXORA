package com.luxora.commerce.checkout.service;

import com.luxora.commerce.checkout.dto.CheckoutPreviewItemResponse;
import com.luxora.commerce.checkout.dto.CheckoutPreviewResponse;
import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.promotion.dto.AvailablePromotionResponse;
import com.luxora.commerce.promotion.service.PromotionService;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutCalculation(
        List<CheckoutCalculationItem> items,
        ShippingAddressResponse shippingAddress,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal grandTotal,
        String currency,
        PromotionService.AppliedPromotion promotion,
        Integer loyaltyPointsRedeemed,
        BigDecimal loyaltyDiscount,
        List<AvailablePromotionResponse> availablePromotions) {

    public CheckoutCalculation(
            List<CheckoutCalculationItem> items,
            ShippingAddressResponse shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal grandTotal,
            String currency,
            PromotionService.AppliedPromotion promotion,
            Integer loyaltyPointsRedeemed,
            BigDecimal loyaltyDiscount) {
        this(items, shippingAddress, subtotal, shippingFee, tax, discount, grandTotal, currency, promotion, loyaltyPointsRedeemed, loyaltyDiscount, List.of());
    }

    public CheckoutCalculation(
            List<CheckoutCalculationItem> items,
            ShippingAddressResponse shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal grandTotal,
            String currency,
            PromotionService.AppliedPromotion promotion) {
        this(items, shippingAddress, subtotal, shippingFee, tax, discount, grandTotal, currency, promotion, 0, BigDecimal.ZERO, List.of());
    }

    public CheckoutPreviewResponse toPreviewResponse() {
        return new CheckoutPreviewResponse(
                items.stream().map(i -> new CheckoutPreviewItemResponse(
                        i.productId(),
                        i.variantId(),
                        i.slug(),
                        i.name(),
                        i.sku(),
                        i.color(),
                        i.storage(),
                        i.imageUrl(),
                        i.unitPrice(),
                        i.quantity(),
                        i.lineTotal())).toList(),
                shippingAddress,
                subtotal,
                shippingFee,
                tax,
                discount,
                grandTotal,
                currency,
                promotion != null ? promotion.response() : null,
                loyaltyPointsRedeemed != null ? loyaltyPointsRedeemed : 0,
                loyaltyDiscount != null ? loyaltyDiscount : BigDecimal.ZERO,
                availablePromotions != null ? availablePromotions : List.of()
        );
    }
}