"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorState } from "@/components/ErrorState";
import { getAdminDashboard } from "@/lib/adminClient";
import type { AdminDashboard } from "@/lib/types";

export function AdminDashboardView() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const next = await getAdminDashboard();
        if (active) setDashboard(next);
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : "Unable to load admin dashboard.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, []);

  return (
    <AdminShell>
      <div className="admin-heading">
        <p className="eyebrow">Operations</p>
        <h1>Admin dashboard</h1>
      </div>
      {loading ? <div className="state-panel" aria-busy="true">Loading dashboard...</div> : null}
      {error ? <ErrorState title="Dashboard unavailable" message={error} /> : null}
      {dashboard ? (
        <div className="admin-card-grid">
          <Metric label="Products" value={dashboard.totalProducts} />
          <Metric label="Variants" value={dashboard.totalVariants} />
          <Metric label="Orders" value={dashboard.totalOrders} />
          <Metric label="Pending" value={dashboard.pendingOrders} />
          <Metric label="Paid" value={dashboard.paidOrders} />
        </div>
      ) : null}
    </AdminShell>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <article className="admin-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}