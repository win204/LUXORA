package com.luxora.commerce.loyalty.model;

public enum LoyaltyTier {
    SILVER(0, 1.0, false, 500),
    GOLD(500, 1.25, false, 2000),
    PLATINUM(2000, 1.5, true, Integer.MAX_VALUE);

    private final int minLifetimePoints;
    private final double pointMultiplier;
    private final boolean freeShipping;
    private final int nextTierPoints;

    LoyaltyTier(int minLifetimePoints, double pointMultiplier, boolean freeShipping, int nextTierPoints) {
        this.minLifetimePoints = minLifetimePoints;
        this.pointMultiplier = pointMultiplier;
        this.freeShipping = freeShipping;
        this.nextTierPoints = nextTierPoints;
    }

    public static LoyaltyTier resolve(int lifetimePoints) {
        if (lifetimePoints >= PLATINUM.minLifetimePoints) {
            return PLATINUM;
        } else if (lifetimePoints >= GOLD.minLifetimePoints) {
            return GOLD;
        } else {
            return SILVER;
        }
    }

    public int getMinLifetimePoints() {
        return minLifetimePoints;
    }

    public double getPointMultiplier() {
        return pointMultiplier;
    }

    public boolean isFreeShipping() {
        return freeShipping;
    }

    public int getNextTierPoints() {
        return nextTierPoints;
    }
}
