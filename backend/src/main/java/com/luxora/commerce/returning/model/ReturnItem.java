package com.luxora.commerce.returning.model;

import com.luxora.commerce.order.model.OrderItem;
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
@Table(name = "return_items")
public class ReturnItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "requested_quantity", nullable = false)
    private int requestedQuantity;

    @Column(name = "approved_quantity", nullable = false)
    private int approvedQuantity;

    @Column(name = "received_quantity", nullable = false)
    private int receivedQuantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "product_name", nullable = false, length = 180)
    private String productName;

    @Column(nullable = false, length = 80)
    private String sku;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReturnItem() {
    }

    public ReturnItem(OrderItem orderItem, int requestedQuantity, String reason) {
        this.orderItem = orderItem;
        this.requestedQuantity = requestedQuantity;
        this.approvedQuantity = requestedQuantity;
        this.receivedQuantity = 0;
        this.reason = reason;
        this.productName = orderItem.getProductName();
        this.sku = orderItem.getSku();
        this.unitPrice = orderItem.getUnitPrice();
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    void attachTo(ReturnRequest returnRequest) {
        this.returnRequest = returnRequest;
    }

    public void setApprovedQuantity(int approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    public void setReceivedQuantity(int receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public UUID getId() { return id; }
    public ReturnRequest getReturnRequest() { return returnRequest; }
    public OrderItem getOrderItem() { return orderItem; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getApprovedQuantity() { return approvedQuantity; }
    public int getReceivedQuantity() { return receivedQuantity; }
    public String getReason() { return reason; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Instant getCreatedAt() { return createdAt; }
}
