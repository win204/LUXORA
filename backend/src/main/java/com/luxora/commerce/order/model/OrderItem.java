package com.luxora.commerce.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "product_slug", nullable = false, length = 220)
    private String productSlug;

    @Column(name = "product_name", nullable = false, length = 180)
    private String productName;

    @Column(name = "variant_name", length = 180)
    private String variantName;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(length = 80)
    private String color;

    @Column(length = 80)
    private String storage;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItem() {
    }

    public OrderItem(UUID productId, UUID variantId, String productSlug, String productName, String variantName,
            String sku, String color, String storage, String imageUrl, BigDecimal unitPrice, int quantity,
            BigDecimal lineTotal) {
        this.productId = productId;
        this.variantId = variantId;
        this.productSlug = productSlug;
        this.productName = productName;
        this.variantName = variantName;
        this.sku = sku;
        this.color = color;
        this.storage = storage;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public String getProductSlug() {
        return productSlug;
    }

    public String getProductName() {
        return productName;
    }

    public String getVariantName() {
        return variantName;
    }

    public String getSku() {
        return sku;
    }

    public String getColor() {
        return color;
    }

    public String getStorage() {
        return storage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}