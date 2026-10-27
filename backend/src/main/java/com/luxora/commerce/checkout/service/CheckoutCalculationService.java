package com.luxora.commerce.checkout.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.cart.service.CartService;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductImage;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.checkout.dto.CheckoutPreviewRequest;
import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.flashsale.model.FlashSaleItem;
import com.luxora.commerce.flashsale.service.FlashSaleService;
import com.luxora.commerce.loyalty.service.LoyaltyService;
import com.luxora.commerce.promotion.service.PromotionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckoutCalculationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String CURRENCY = "USD";

    private final CartService cartService;
    private final ProductVariantRepository variantRepository;
    private final PromotionService promotionService;
    private final LoyaltyService loyaltyService;
    private final FlashSaleService flashSaleService;

    public CheckoutCalculationService(
            CartService cartService,
            ProductVariantRepository variantRepository,
            PromotionService promotionService,
            LoyaltyService loyaltyService,
            FlashSaleService flashSaleService) {
        this.cartService = cartService;
        this.variantRepository = variantRepository;
        this.promotionService = promotionService;
        this.loyaltyService = loyaltyService;
        this.flashSaleService = flashSaleService;
    }

    public CheckoutCalculation calculate(UUID userId, CheckoutPreviewRequest request, boolean lockInventory) {
        Map<UUID, Integer> quantities = cartService.getAuthenticatedCartQuantities(userId);
        return calculateWithQuantities(userId, quantities, request, lockInventory);
    }

    public CheckoutCalculation calculate(
            AuthenticatedUser user,
            String cartId,
            CheckoutPreviewRequest request,
            boolean lockInventory) {
        Map<UUID, Integer> quantities;
        UUID userId = null;
        if (user != null) {
            userId = user.id();
            quantities = cartService.getAuthenticatedCartQuantities(user.id());
        } else {
            quantities = cartService.getAnonymousCartQuantities(cartId);
        }
        return calculateWithQuantities(userId, quantities, request, lockInventory);
    }

    private CheckoutCalculation calculateWithQuantities(
            UUID userId,
            Map<UUID, Integer> quantities,
            CheckoutPreviewRequest request,
            boolean lockInventory) {
        if (quantities == null || quantities.isEmpty()) {
            throw new BadRequestException("EMPTY_CART", "Cart is empty");
        }
        List<CheckoutCalculationItem> items = quantities.entrySet().stream()
                .map(e -> item(e.getKey(), e.getValue(), lockInventory))
                .sorted(Comparator.comparing(CheckoutCalculationItem::name).thenComparing(CheckoutCalculationItem::sku))
                .toList();
        BigDecimal subtotal = items.stream().map(CheckoutCalculationItem::lineTotal).reduce(ZERO, BigDecimal::add);
        String promoCode = request.promotionCode();
        if ((promoCode == null || promoCode.isBlank()) && Boolean.TRUE.equals(request.autoApplyBestVoucher())) {
            promoCode = promotionService.findBestAvailablePromotionCode(subtotal);
        }
        PromotionService.AppliedPromotion promotion = promotionService.apply(promoCode, subtotal, lockInventory);
        java.util.List<com.luxora.commerce.promotion.dto.AvailablePromotionResponse> availablePromotions = promotionService.getAvailablePromotions(subtotal);

        BigDecimal subtotalAfterPromo = subtotal.subtract(promotion.discountAmount());
        BigDecimal loyaltyDiscount = ZERO;
        int pointsRedeemed = 0;

        if (userId != null && request.redeemPoints() != null && request.redeemPoints() > 0) {
            loyaltyDiscount = loyaltyService.calculateLoyaltyDiscount(userId, request.redeemPoints(), subtotalAfterPromo);
            pointsRedeemed = loyaltyDiscount.multiply(new BigDecimal("100")).intValue();
        }

        BigDecimal grandTotal = subtotalAfterPromo.subtract(loyaltyDiscount);
        if (grandTotal.compareTo(ZERO) < 0) {
            grandTotal = ZERO;
        }

        return new CheckoutCalculation(
                items,
                address(request),
                subtotal,
                ZERO,
                ZERO,
                promotion.discountAmount().add(loyaltyDiscount),
                grandTotal,
                CURRENCY,
                promotion,
                pointsRedeemed,
                loyaltyDiscount,
                availablePromotions);
    }

    private CheckoutCalculationItem item(UUID id, int quantity, boolean lock) {
        if (quantity <= 0) {
            throw new BadRequestException("INVALID_QUANTITY", "Quantity must be greater than zero");
        }
        ProductVariant v = (lock ? variantRepository.findWithProductByIdForUpdate(id) : variantRepository.findWithProductById(id))
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found"));
        Product p = v.getProduct();
        if (!v.isActive() || !p.isActive()) {
            throw new BadRequestException("VARIANT_UNAVAILABLE", "Product variant is unavailable");
        }
        if (v.getInventoryItem().getQuantityAvailable() < quantity) {
            throw new BadRequestException("INSUFFICIENT_STOCK", "Not enough stock for selected variant");
        }
        String image = p.getImages().stream().min(Comparator.comparingInt(ProductImage::getDisplayOrder)).map(ProductImage::getUrl).orElse(null);

        BigDecimal effectiveUnitPrice = v.getPrice();
        UUID flashSaleItemId = null;
        if (flashSaleService != null) {
            Optional<FlashSaleItem> activeFlashSale = flashSaleService.findActiveFlashSaleItem(v.getId(), p.getId());
            if (activeFlashSale.isPresent()) {
                FlashSaleItem fsi = activeFlashSale.get();
                if (fsi.isAvailable()) {
                    effectiveUnitPrice = fsi.getFlashSalePrice();
                    flashSaleItemId = fsi.getId();
                }
            }
        }

        return new CheckoutCalculationItem(
                v,
                p.getId(),
                v.getId(),
                p.getSlug(),
                p.getName(),
                v.getSku(),
                v.getColor(),
                v.getStorage(),
                image,
                effectiveUnitPrice,
                quantity,
                effectiveUnitPrice.multiply(BigDecimal.valueOf(quantity)),
                flashSaleItemId);
    }

    private ShippingAddressResponse address(CheckoutPreviewRequest r) {
        return new ShippingAddressResponse(
                r.recipientName().trim(),
                r.phone().trim(),
                r.addressLine1().trim(),
                trim(r.addressLine2()),
                r.city().trim(),
                r.province().trim(),
                r.country().trim(),
                trim(r.postalCode()));
    }

    private String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}