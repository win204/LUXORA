package com.luxora.commerce.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "product_specifications")
public class ProductSpecification {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "spec_value", nullable = false, length = 500)
    private String value;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected ProductSpecification() {
    }

    ProductSpecification(Product product, String name, String value, int displayOrder) {
        this.product = product;
        this.name = name;
        this.value = value;
        this.displayOrder = displayOrder;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
