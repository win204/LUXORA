package com.luxora.commerce.returning.model;

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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "return_shipments")
public class ReturnShipment {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false, unique = true)
    private ReturnRequest returnRequest;

    @Column(name = "carrier", nullable = false, length = 80)
    private String carrier;

    @Column(name = "tracking_number", nullable = false, length = 120, unique = true)
    private String trackingNumber;

    @Column(name = "mock_label_reference", nullable = false, length = 180)
    private String mockLabelReference;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReturnShipment() {
    }

    public ReturnShipment(ReturnRequest returnRequest, String carrier, String trackingNumber, String mockLabelReference) {
        this.returnRequest = returnRequest;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.mockLabelReference = mockLabelReference;
    }

    @PrePersist
    void prePersist() {
        Instant now = timestamp();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = timestamp();
    }

    public void markShipped() {
        if (shippedAt == null) {
            shippedAt = timestamp();
        }
    }

    public void markReceived() {
        if (receivedAt == null) {
            receivedAt = timestamp();
        }
    }

    public UUID getId() { return id; }
    public ReturnRequest getReturnRequest() { return returnRequest; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getMockLabelReference() { return mockLabelReference; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static Instant timestamp() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}