package com.luxora.commerce.inventory.model;

import com.luxora.commerce.catalog.model.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    private ProductVariant variant;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    protected InventoryItem() {
    }

    public InventoryItem(ProductVariant variant, int quantityAvailable) {
        this.variant = variant;
        this.quantityAvailable = quantityAvailable;
    }

    public UUID getId() {
        return id;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public boolean isAvailable() {
        return quantityAvailable > 0;
    }

    public void updateQuantity(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public void decrement(int quantity) {
        this.quantityAvailable -= quantity;
    }

    public void restock(int quantity) {
        this.quantityAvailable += quantity;
    }
}