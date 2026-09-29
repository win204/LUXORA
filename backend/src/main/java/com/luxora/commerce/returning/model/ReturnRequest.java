package com.luxora.commerce.returning.model;

import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "returns")
public class ReturnRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ReturnShipment shipment;

    protected ReturnRequest() {
    }

    public ReturnRequest(Order order, User user, String customerNote) {
        this.order = order;
        this.user = user;
        this.customerNote = customerNote;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        requestedAt = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addItem(ReturnItem item) {
        items.add(item);
        item.attachTo(this);
    }

    public void attachShipment(ReturnShipment shipment) {
        this.shipment = shipment;
    }

    public void approve(String adminNote) {
        status = ReturnStatus.APPROVED;
        this.adminNote = adminNote;
        approvedAt = Instant.now();
    }

    public void reject(String adminNote) {
        status = ReturnStatus.REJECTED;
        this.adminNote = adminNote;
        rejectedAt = Instant.now();
    }

    public void cancel() {
        status = ReturnStatus.CANCELLED;
        cancelledAt = Instant.now();
    }

    public void markReceived(String adminNote) {
        status = ReturnStatus.RECEIVED;
        this.adminNote = adminNote;
        receivedAt = Instant.now();
    }

    public void markRefunded() {
        status = ReturnStatus.REFUNDED;
        refundedAt = Instant.now();
    }

    public void updateAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public UUID getId() { return id; }
    public Order getOrder() { return order; }
    public User getUser() { return user; }
    public ReturnStatus getStatus() { return status; }
    public String getCustomerNote() { return customerNote; }
    public String getAdminNote() { return adminNote; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ReturnItem> getItems() { return Collections.unmodifiableList(items); }
    public ReturnShipment getShipment() { return shipment; }
}
