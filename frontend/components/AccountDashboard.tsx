"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { useAuth } from "@/components/AuthProvider";

export function AccountDashboard() {
  const router = useRouter();
  const { user, loading, logout } = useAuth();

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, router, user]);

  if (loading) {
    return (
      <section className="account-panel" aria-busy="true">
        <p className="eyebrow">Account</p>
        <div className="skeleton-line skeleton-wide" />
        <div className="skeleton-line" />
      </section>
    );
  }

  if (!user) {
    return <EmptyState title="Login required" message="Sign in to view your account." />;
  }

  return (
    <section className="account-layout">
      <div className="account-copy">
        <p className="eyebrow">Account</p>
        <h1>{user.firstName} {user.lastName}</h1>
        <p>Your account keeps orders, profile details, and session controls close at hand.</p>
      </div>

      <div className="account-stack">
        <div className="account-panel">
          <div className="account-panel-header">
            <div>
              <p className="eyebrow">Profile</p>
              <h2>Personal details</h2>
            </div>
          </div>
          <dl className="profile-details">
            <div>
              <dt>Name</dt>
              <dd>{user.firstName} {user.lastName}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{user.enabled ? "Active" : "Disabled"}</dd>
            </div>
          </dl>
        </div>

        <div className="account-panel account-links-panel">
          <div>
            <p className="eyebrow">Orders</p>
            <h2>Order history</h2>
            <p className="muted-copy">Review your saved order snapshots, payment status, and returns.</p>
          </div>
          <div className="account-actions">
            <LinkButton href="/account/orders">View orders</LinkButton>
            <LinkButton href="/account/returns" variant="secondary">View returns</LinkButton>
            <Button type="button" variant="secondary" onClick={() => void logout()}>
              Logout
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}
