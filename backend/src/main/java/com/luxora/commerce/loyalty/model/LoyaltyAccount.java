package com.luxora.commerce.loyalty.model;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_points", nullable = false)
    private int currentPoints = 0;

    @Column(name = "lifetime_points", nullable = false)
    private int lifetimePoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyTier tier = LoyaltyTier.SILVER;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<LoyaltyTransaction> transactions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LoyaltyAccount() {
    }

    public LoyaltyAccount(User user) {
        this.user = user;
        this.currentPoints = 0;
        this.lifetimePoints = 0;
        this.tier = LoyaltyTier.SILVER;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void recalculateTier() {
        this.tier = LoyaltyTier.resolve(this.lifetimePoints);
    }

    public void addTransaction(LoyaltyTransactionType type, int points, String description, UUID orderId) {
        LoyaltyTransaction tx = new LoyaltyTransaction(this, type, points, description, orderId);
        this.transactions.add(tx);
        this.currentPoints += points;
        if (this.currentPoints < 0) {
            this.currentPoints = 0;
        }
        if (points > 0 && (type == LoyaltyTransactionType.ORDER_EARNED || type == LoyaltyTransactionType.ADMIN_ADJUSTED)) {
            this.lifetimePoints += points;
            recalculateTier();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }

    public int getLifetimePoints() {
        return lifetimePoints;
    }

    public void setLifetimePoints(int lifetimePoints) {
        this.lifetimePoints = lifetimePoints;
    }

    public LoyaltyTier getTier() {
        return tier;
    }

    public void setTier(LoyaltyTier tier) {
        this.tier = tier;
    }

    public List<LoyaltyTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<LoyaltyTransaction> transactions) {
        this.transactions = transactions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
