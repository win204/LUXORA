package com.luxora.commerce.returning.model;

import com.luxora.commerce.user.model.User;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_status_history")
public class ReturnStatusHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private ReturnRequest returnRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 40)
    private ReturnStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private ReturnStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    protected ReturnStatusHistory() {
    }

    public ReturnStatusHistory(ReturnRequest returnRequest, ReturnStatus fromStatus, ReturnStatus toStatus, User changedByUser) {
        this.returnRequest = returnRequest;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUser = changedByUser;
    }

    @PrePersist
    void prePersist() {
        changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ReturnRequest getReturnRequest() { return returnRequest; }
    public ReturnStatus getFromStatus() { return fromStatus; }
    public ReturnStatus getToStatus() { return toStatus; }
    public Instant getChangedAt() { return changedAt; }
    public User getChangedByUser() { return changedByUser; }
}
