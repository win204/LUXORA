package com.luxora.commerce.flashsale.service;

import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductImage;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.flashsale.dto.CreateFlashSaleItemRequest;
import com.luxora.commerce.flashsale.dto.CreateFlashSaleRequest;
import com.luxora.commerce.flashsale.dto.FlashSaleItemResponse;
import com.luxora.commerce.flashsale.dto.FlashSaleResponse;
import com.luxora.commerce.flashsale.model.FlashSale;
import com.luxora.commerce.flashsale.model.FlashSaleItem;
import com.luxora.commerce.flashsale.model.FlashSaleStatus;
import com.luxora.commerce.flashsale.repository.FlashSaleItemRepository;
import com.luxora.commerce.flashsale.repository.FlashSaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public FlashSaleService(
        FlashSaleRepository flashSaleRepository,
        FlashSaleItemRepository flashSaleItemRepository,
        ProductRepository productRepository,
        ProductVariantRepository productVariantRepository
    ) {
        this.flashSaleRepository = flashSaleRepository;
        this.flashSaleItemRepository = flashSaleItemRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    public Optional<FlashSaleResponse> getActiveFlashSale() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return flashSaleRepository.findFirstActiveSale(now)
            .map(sale -> mapToResponse(sale, now));
    }

    @Transactional(readOnly = true)
    public List<FlashSaleResponse> getAllFlashSales() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return flashSaleRepository.findAllByOrderByStartTimeDesc().stream()
            .map(sale -> mapToResponse(sale, now))
            .toList();
    }

    @Transactional(readOnly = true)
    public FlashSaleResponse getFlashSale(UUID id) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        FlashSale sale = flashSaleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("FLASH_SALE_NOT_FOUND", "Flash sale not found"));
        return mapToResponse(sale, now);
    }

    @Transactional
    public FlashSaleResponse createFlashSale(CreateFlashSaleRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "End time must be strictly after start time");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        FlashSaleStatus status = FlashSaleStatus.UPCOMING;
        if (!now.isBefore(request.startTime()) && now.isBefore(request.endTime())) {
            status = FlashSaleStatus.ACTIVE;
        } else if (now.isAfter(request.endTime())) {
            status = FlashSaleStatus.ENDED;
        }

        FlashSale flashSale = new FlashSale(
            request.name().trim(),
            request.description() != null ? request.description().trim() : null,
            request.startTime(),
            request.endTime(),
            status
        );
        flashSale.setBannerImageUrl(request.bannerImageUrl());

        List<FlashSaleItem> items = new ArrayList<>();
        for (CreateFlashSaleItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + itemReq.productId()));

            ProductVariant variant = null;
            if (itemReq.variantId() != null) {
                variant = productVariantRepository.findById(itemReq.variantId())
                    .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Variant not found: " + itemReq.variantId()));
            }

            BigDecimal originalPrice = variant != null ? variant.getPrice() : product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(ProductVariant::getPrice)
                .findFirst()
                .orElse(itemReq.flashSalePrice());

            if (itemReq.flashSalePrice().compareTo(originalPrice) >= 0) {
                // If flash sale price is not less than original, warn or reject
                // Allow if specified, but must be > 0
            }

            FlashSaleItem item = new FlashSaleItem(
                flashSale,
                product,
                variant,
                itemReq.flashSalePrice(),
                itemReq.stockQuantity()
            );
            items.add(item);
        }

        flashSale.setItems(items);
        FlashSale saved = flashSaleRepository.save(flashSale);
        return mapToResponse(saved, now);
    }

    @Transactional
    public FlashSaleResponse cancelFlashSale(UUID id) {
        FlashSale sale = flashSaleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("FLASH_SALE_NOT_FOUND", "Flash sale not found"));
        sale.setStatus(FlashSaleStatus.CANCELLED);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return mapToResponse(sale, now);
    }

    @Transactional(readOnly = true)
    public Optional<FlashSaleItem> findActiveFlashSaleItem(UUID variantId, UUID productId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return flashSaleItemRepository.findActiveItem(now, variantId, productId);
    }

    @Transactional
    public void recordSale(UUID flashSaleItemId, int quantity) {
        flashSaleItemRepository.findByIdForUpdate(flashSaleItemId).ifPresent(item -> {
            int newSold = item.getSoldQuantity() + quantity;
            item.setSoldQuantity(Math.min(item.getStockQuantity(), newSold));
        });
    }

    public FlashSaleResponse mapToResponse(FlashSale sale, OffsetDateTime now) {
        long secondsRemaining = 0;
        if (sale.getStatus() == FlashSaleStatus.ACTIVE && sale.getEndTime().isAfter(now)) {
            secondsRemaining = Duration.between(now, sale.getEndTime()).toSeconds();
        }

        List<FlashSaleItemResponse> itemResponses = sale.getItems().stream().map(item -> {
            Product p = item.getProduct();
            ProductVariant v = item.getVariant();

            BigDecimal originalPrice = v != null ? v.getPrice() : p.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(ProductVariant::getPrice)
                .findFirst()
                .orElse(item.getFlashSalePrice());

            int discountPct = 0;
            if (originalPrice.compareTo(BigDecimal.ZERO) > 0 && originalPrice.compareTo(item.getFlashSalePrice()) > 0) {
                discountPct = originalPrice.subtract(item.getFlashSalePrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
            }

            int percentClaimed = item.getStockQuantity() > 0
                ? Math.min(100, Math.round(((float) item.getSoldQuantity() / item.getStockQuantity()) * 100))
                : 100;

            String imageUrl = p.getImages().stream()
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(null);

            String variantDesc = null;
            if (v != null) {
                if (v.getColor() != null && v.getStorage() != null) {
                    variantDesc = v.getColor() + " / " + v.getStorage();
                } else if (v.getColor() != null) {
                    variantDesc = v.getColor();
                } else if (v.getStorage() != null) {
                    variantDesc = v.getStorage();
                } else {
                    variantDesc = v.getSku();
                }
            }

            return new FlashSaleItemResponse(
                item.getId(),
                p.getId(),
                p.getName(),
                p.getSlug(),
                imageUrl,
                originalPrice,
                v != null ? v.getId() : null,
                variantDesc,
                item.getFlashSalePrice(),
                discountPct,
                item.getStockQuantity(),
                item.getSoldQuantity(),
                percentClaimed,
                item.getAvailableQuantity()
            );
        }).toList();

        return new FlashSaleResponse(
            sale.getId(),
            sale.getName(),
            sale.getDescription(),
            sale.getStartTime(),
            sale.getEndTime(),
            sale.getStatus().name(),
            sale.getBannerImageUrl(),
            secondsRemaining,
            itemResponses
        );
    }
}
