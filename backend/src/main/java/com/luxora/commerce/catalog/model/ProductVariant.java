package com.luxora.commerce.catalog.model;

import com.luxora.commerce.inventory.model.InventoryItem;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(length = 80)
    private String color;

    @Column(length = 80)
    private String storage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @OneToOne(mappedBy = "variant", fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    private InventoryItem inventoryItem;

    protected ProductVariant() {
    }

    public ProductVariant(String sku, String color, String storage, BigDecimal price, int quantityAvailable) {
        this.sku = sku;
        this.color = color;
        this.storage = storage;
        this.price = price;
        this.inventoryItem = new InventoryItem(this, quantityAvailable);
    }

    void assignProduct(Product product) {
        this.product = product;
    }

    public void update(String sku, String color, String storage, BigDecimal price, boolean active) {
        this.sku = sku;
        this.color = color;
        this.storage = storage;
        this.price = price;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
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

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }
}
