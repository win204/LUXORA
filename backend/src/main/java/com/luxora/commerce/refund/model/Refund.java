package com.luxora.commerce.refund.model;

import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.returning.model.ReturnRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id")
    private ReturnRequest returnRequest;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_reference", nullable = false, unique = true, length = 120)
    private String providerReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Refund() {
    }

    public Refund(Order order, Payment payment, String provider, String providerReference, BigDecimal amount, String currency, String reason) {
        this(order, payment, null, provider, providerReference, amount, currency, reason);
    }

    public Refund(Order order, Payment payment, ReturnRequest returnRequest, String provider, String providerReference, BigDecimal amount, String currency, String reason) {
        this.order = order;
        this.payment = payment;
        this.returnRequest = returnRequest;
        this.provider = provider;
        this.providerReference = providerReference;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
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

    public void markSucceeded() { status = RefundStatus.SUCCEEDED; }
    public void markFailed() { status = RefundStatus.FAILED; }

    public UUID getId() { return id; }
    public Order getOrder() { return order; }
    public Payment getPayment() { return payment; }
    public ReturnRequest getReturnRequest() { return returnRequest; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public RefundStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
