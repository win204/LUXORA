package com.luxora.commerce.order.shipment.model;

import com.luxora.commerce.order.model.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, length = 120)
    private String carrier;

    @Column(name = "tracking_number", nullable = false, length = 120)
    private String trackingNumber;

    @Column(name = "shipped_at", nullable = false)
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shipment() {
    }

    public Shipment(Order order, String carrier, String trackingNumber, Instant shippedAt) {
        this.order = order;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void updateTracking(String carrier, String trackingNumber) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }

    public void markDelivered(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
