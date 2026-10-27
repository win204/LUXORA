"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { fetchMyLoyaltyAccount, fetchMyLoyaltyTransactions } from "@/lib/loyaltyClient";
import type { LoyaltyAccount, LoyaltyTransaction } from "@/lib/types";

export function LoyaltyRewardsSection() {
  const { user, accessToken, refreshAuth } = useAuth();
  const [account, setAccount] = useState<LoyaltyAccount | null>(null);
  const [transactions, setTransactions] = useState<LoyaltyTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadLoyalty() {
      if (!user) return;
      setLoading(true);
      setError(null);

      try {
        let token = accessToken;
        if (!token) {
          const refreshed = await refreshAuth();
          token = refreshed?.accessToken ?? null;
        }
        if (!token) throw new Error("Authentication token unavailable.");

        const [acc, txPage] = await Promise.all([
          fetchMyLoyaltyAccount(token),
          fetchMyLoyaltyTransactions(token, 0, 5)
        ]);

        if (!cancelled) {
          setAccount(acc);
          setTransactions(txPage.content);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Failed to load loyalty rewards.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadLoyalty();

    return () => {
      cancelled = true;
    };
  }, [user, accessToken, refreshAuth]);

  if (loading) {
    return (
      <div className="account-panel loyalty-panel" aria-busy="true">
        <p className="eyebrow">Rewards</p>
        <div className="skeleton-line skeleton-wide" />
        <div className="skeleton-line" />
      </div>
    );
  }

  if (error || !account) {
    return null; // Gracefully degrade if loyalty service has minor issue
  }

  const tierColors: Record<string, { bg: string; text: string; border: string; glow: string }> = {
    SILVER: {
      bg: "linear-gradient(135deg, #e0e2e6 0%, #c4c7cc 50%, #b0b4ba 100%)",
      text: "#1a1f26",
      border: "#b0b4ba",
      glow: "rgba(192, 197, 204, 0.35)"
    },
    GOLD: {
      bg: "linear-gradient(135deg, #f7d57f 0%, #e5b94c 50%, #c59728 100%)",
      text: "#332200",
      border: "#d4a433",
      glow: "rgba(229, 185, 76, 0.4)"
    },
    PLATINUM: {
      bg: "linear-gradient(135deg, #2b323c 0%, #1a1e24 50%, #0d1116 100%)",
      text: "#f0f3f6",
      border: "#4b5563",
      glow: "rgba(100, 116, 139, 0.5)"
    }
  };

  const style = tierColors[account.tier] ?? tierColors.SILVER;
  const isPlatinum = account.tier === "PLATINUM";

  const progressPercent = isPlatinum
    ? 100
    : Math.min(100, Math.round(((account.nextTierPoints - account.pointsToNextTier) / account.nextTierPoints) * 100));

  return (
    <div className="account-panel loyalty-card-container">
      <div className="account-panel-header">
        <div>
          <p className="eyebrow">LUXORA Privilege</p>
          <h2>Rewards & Tier Membership</h2>
        </div>
      </div>

      <div
        className="loyalty-hero-card"
        style={{
          background: style.bg,
          color: style.text,
          border: `1px solid ${style.border}`,
          boxShadow: `0 18px 45px ${style.glow}`
        }}
      >
        <div className="loyalty-card-top">
          <div className="loyalty-brand">
            <span className="brand-icon">✦</span>
            <strong>LUXORA REWARDS</strong>
          </div>
          <span className="tier-pill" style={{ borderColor: style.border }}>
            {account.tier} MEMBER
          </span>
        </div>

        <div className="loyalty-card-center">
          <div className="points-display">
            <span className="points-label">AVAILABLE POINTS</span>
            <div className="points-number">
              {account.currentPoints.toLocaleString()} <span className="pts-suffix">PTS</span>
            </div>
            <p className="dollar-worth">≈ ${account.dollarValue.toFixed(2)} USD value off your next order</p>
          </div>
          <div className="tier-benefit-badges">
            <div className="benefit-badge">
              <strong>{account.pointMultiplier}x</strong>
              <span>Points Multiplier</span>
            </div>
            {account.freeShipping ? (
              <div className="benefit-badge highlight">
                <strong>FREE</strong>
                <span>Standard Shipping</span>
              </div>
            ) : null}
          </div>
        </div>

        <div className="loyalty-card-bottom">
          <div className="tier-progress-info">
            <span>
              {isPlatinum ? (
                "Exclusive VIP tier unlocked"
              ) : (
                <>
                  Earn <strong>{account.pointsToNextTier}</strong> more points to reach{" "}
                  <strong>{account.tier === "SILVER" ? "GOLD" : "PLATINUM"}</strong>
                </>
              )}
            </span>
            <span className="lifetime-count">{account.lifetimePoints} Lifetime PTS</span>
          </div>
          <div className="loyalty-progress-track">
            <div
              className="loyalty-progress-fill"
              style={{
                width: `${progressPercent}%`,
                background: isPlatinum ? "#f0f3f6" : "#171717"
              }}
            />
          </div>
        </div>
      </div>

      <div className="loyalty-perks-grid">
        <div className={`perk-box ${account.tier === "SILVER" ? "active" : ""}`}>
          <div className="perk-tier-title">SILVER</div>
          <p className="perk-desc">1.0x points for every $1 spent</p>
          <span className="perk-status">Entry Level</span>
        </div>
        <div className={`perk-box ${account.tier === "GOLD" ? "active" : ""}`}>
          <div className="perk-tier-title">GOLD</div>
          <p className="perk-desc">1.25x points multiplier + exclusive offers</p>
          <span className="perk-status">500+ Lifetime Points</span>
        </div>
        <div className={`perk-box ${account.tier === "PLATINUM" ? "active" : ""}`}>
          <div className="perk-tier-title">PLATINUM</div>
          <p className="perk-desc">1.5x points multiplier + FREE standard delivery</p>
          <span className="perk-status">2,000+ Lifetime Points</span>
        </div>
      </div>

      {transactions.length > 0 ? (
        <div className="loyalty-transactions-section">
          <h3>Recent Points Activity</h3>
          <ul className="loyalty-tx-list">
            {transactions.map((tx) => {
              const isPositive = tx.points > 0;
              return (
                <li key={tx.id} className="loyalty-tx-item">
                  <div className="tx-details">
                    <span className="tx-description">{tx.description || tx.type.replace(/_/g, " ")}</span>
                    <span className="tx-date">{new Date(tx.createdAt).toLocaleDateString()}</span>
                  </div>
                  <div className={`tx-points ${isPositive ? "positive" : "negative"}`}>
                    {isPositive ? `+${tx.points}` : tx.points} PTS
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
