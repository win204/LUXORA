"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AdminShell } from "@/components/AdminShell";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import {
  getAdminAnalyticsOverview,
  getAdminRevenueTimeline,
  getAdminTopSellingProducts
} from "@/lib/adminClient";
import type {
  AdminAnalyticsOverview,
  RevenueTimelinePoint,
  TopSellingProduct
} from "@/lib/types";

export function AdminDashboardView() {
  const [overview, setOverview] = useState<AdminAnalyticsOverview | null>(null);
  const [timeline, setTimeline] = useState<RevenueTimelinePoint[]>([]);
  const [topProducts, setTopProducts] = useState<TopSellingProduct[]>([]);
  const [period, setPeriod] = useState<"7d" | "30d" | "12m">("7d");
  const [loading, setLoading] = useState(true);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [error, setError] = useState("");
  const [hoveredPoint, setHoveredPoint] = useState<RevenueTimelinePoint | null>(null);

  useEffect(() => {
    let active = true;

    async function loadInitial() {
      setLoading(true);
      setError("");
      try {
        const [ovData, tlData, tpData] = await Promise.all([
          getAdminAnalyticsOverview(),
          getAdminRevenueTimeline("7d"),
          getAdminTopSellingProducts(5)
        ]);
        if (active) {
          setOverview(ovData);
          setTimeline(tlData);
          setTopProducts(tpData);
        }
      } catch (caught) {
        if (active) {
          setError(caught instanceof Error ? caught.message : "Unable to load business intelligence data.");
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadInitial();

    function handleAuthUpdate() {
      if (active) {
        void loadInitial();
      }
    }

    window.addEventListener("luxora-auth-updated", handleAuthUpdate);
    return () => {
      active = false;
      window.removeEventListener("luxora-auth-updated", handleAuthUpdate);
    };
  }, []);

  async function handlePeriodChange(nextPeriod: "7d" | "30d" | "12m") {
    if (nextPeriod === period) return;
    setPeriod(nextPeriod);
    setTimelineLoading(true);
    setHoveredPoint(null);
    try {
      const data = await getAdminRevenueTimeline(nextPeriod);
      setTimeline(data);
    } catch {
      // Keep existing timeline on error
    } finally {
      setTimelineLoading(false);
    }
  }

  // Calculate completion rate
  const nonCancelledOrders = overview ? overview.totalOrders - overview.cancelledOrders : 0;
  const completionRate = nonCancelledOrders > 0
    ? Math.round((overview!.completedOrders / nonCancelledOrders) * 100)
    : 0;

  return (
    <AdminShell>
      <div className="admin-heading admin-dashboard-heading">
        <div>
          <p className="eyebrow">Executive Intelligence</p>
          <h1>Analytics & Revenue Dashboard</h1>
        </div>
        <div className="period-selector">
          <button
            type="button"
            className={`period-btn ${period === "7d" ? "active" : ""}`}
            onClick={() => void handlePeriodChange("7d")}
          >
            7 Days
          </button>
          <button
            type="button"
            className={`period-btn ${period === "30d" ? "active" : ""}`}
            onClick={() => void handlePeriodChange("30d")}
          >
            30 Days
          </button>
          <button
            type="button"
            className={`period-btn ${period === "12m" ? "active" : ""}`}
            onClick={() => void handlePeriodChange("12m")}
          >
            12 Months
          </button>
        </div>
      </div>

      {loading ? <div className="state-panel" aria-busy="true">Loading financial metrics...</div> : null}
      {error ? <ErrorState title="Analytics unavailable" message={error} /> : null}

      {overview ? (
        <div className="analytics-content">
          {/* Top Primary KPI Row */}
          <div className="admin-kpi-grid">
            <article className="kpi-card highlight-revenue">
              <span className="kpi-label">TOTAL GROSS REVENUE</span>
              <strong className="kpi-value">${overview.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
              <p className="kpi-hint">From all non-cancelled paid & completed orders</p>
            </article>

            <article className="kpi-card">
              <span className="kpi-label">TODAY REVENUE</span>
              <strong className="kpi-value">${overview.todayRevenue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
              <p className="kpi-hint">Current calendar day (UTC)</p>
            </article>

            <article className="kpi-card">
              <span className="kpi-label">THIS MONTH</span>
              <strong className="kpi-value">${overview.thisMonthRevenue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
              <p className="kpi-hint">Month-to-date sales volume</p>
            </article>

            <article className="kpi-card">
              <span className="kpi-label">AVERAGE ORDER VALUE (AOV)</span>
              <strong className="kpi-value">${overview.averageOrderValue.toFixed(2)}</strong>
              <p className="kpi-hint">Revenue per qualifying order</p>
            </article>

            <article className="kpi-card">
              <span className="kpi-label">ORDER COMPLETION RATE</span>
              <strong className="kpi-value">{completionRate}%</strong>
              <p className="kpi-hint">{overview.completedOrders} delivered of {nonCancelledOrders} active</p>
            </article>

            <article className="kpi-card">
              <span className="kpi-label">REGISTERED CUSTOMERS</span>
              <strong className="kpi-value">{overview.totalCustomers.toLocaleString()}</strong>
              <p className="kpi-hint">+{overview.newCustomersThisMonth} new accounts this month</p>
            </article>
          </div>

          {/* Time Series Interactive Chart */}
          <div className="analytics-chart-panel">
            <div className="chart-header">
              <div>
                <p className="eyebrow">Trends</p>
                <h2>Revenue & Order Trajectory ({period === "7d" ? "Past 7 Days" : period === "30d" ? "Past 30 Days" : "Past 12 Months"})</h2>
              </div>
              {hoveredPoint ? (
                <div className="chart-hover-pill">
                  <span><strong>{hoveredPoint.date}:</strong> ${hoveredPoint.revenue.toFixed(2)} ({hoveredPoint.orderCount} orders)</span>
                </div>
              ) : null}
            </div>

            {timelineLoading ? (
              <div className="chart-loading-overlay">Updating timeline...</div>
            ) : null}

            <TimelineChart
              points={timeline}
              onHover={setHoveredPoint}
              hoveredDate={hoveredPoint?.date ?? null}
            />
          </div>

          {/* Order Status Pipeline */}
          <div className="analytics-pipeline-panel">
            <div className="pipeline-header">
              <p className="eyebrow">Fulfillment</p>
              <h2>Order Status Distribution</h2>
            </div>
            <div className="pipeline-track">
              {buildStatusSegment(overview.ordersByStatus, overview.totalOrders)}
            </div>
            <div className="status-chips-legend">
              <StatusLegendItem label="Delivered" count={overview.completedOrders} total={overview.totalOrders} color="#166534" />
              <StatusLegendItem label="Shipped" count={overview.shippedOrders} total={overview.totalOrders} color="#0f766e" />
              <StatusLegendItem label="Processing" count={overview.processingOrders} total={overview.totalOrders} color="#1d4ed8" />
              <StatusLegendItem label="Paid" count={overview.ordersByStatus?.["PAID"] ?? 0} total={overview.totalOrders} color="#ca8a04" />
              <StatusLegendItem label="Pending" count={overview.pendingOrders} total={overview.totalOrders} color="#d97706" />
              <StatusLegendItem label="Cancelled" count={overview.cancelledOrders} total={overview.totalOrders} color="#991b1b" />
            </div>
          </div>

          {/* Lower Two-Column Section */}
          <div className="analytics-lower-grid">
            {/* Top Selling Products */}
            <div className="admin-panel-box">
              <div className="panel-box-header">
                <div>
                  <p className="eyebrow">Performance</p>
                  <h2>Top Best-Selling Products</h2>
                </div>
                <Link href="/admin/products" className="panel-link">Manage catalog</Link>
              </div>

              {topProducts.length === 0 ? (
                <p className="muted-copy">No product sales recorded yet.</p>
              ) : (
                <div className="top-products-list">
                  {topProducts.map((p, idx) => (
                    <article className="top-product-item" key={p.productId}>
                      <span className="rank-badge">#{idx + 1}</span>
                      <div className="product-info">
                        <Link href={`/products/${p.productSlug}`} className="product-title-link">
                          {p.productName}
                        </Link>
                        <span className="product-slug-label">{p.productSlug}</span>
                      </div>
                      <div className="product-sales-metrics">
                        <span className="sold-count-badge">{p.totalQuantitySold} units sold</span>
                        <Price amount={String(p.totalRevenue)} />
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>

            {/* Customer & Loyalty Tier Insights */}
            <div className="admin-panel-box">
              <div className="panel-box-header">
                <div>
                  <p className="eyebrow">Retention</p>
                  <h2>Loyalty Tier Membership</h2>
                </div>
                <span className="badge-luxury">LUXORA Privilege</span>
              </div>

              <div className="tier-breakdown-list">
                <TierBar
                  tierName="PLATINUM"
                  count={overview.customersByTier?.["PLATINUM"] ?? 0}
                  total={overview.totalCustomers}
                  perk="1.5x points + Free standard delivery"
                  color="linear-gradient(135deg, #374151 0%, #111827 100%)"
                />
                <TierBar
                  tierName="GOLD"
                  count={overview.customersByTier?.["GOLD"] ?? 0}
                  total={overview.totalCustomers}
                  perk="1.25x points multiplier (500+ PTS)"
                  color="linear-gradient(135deg, #f59e0b 0%, #d97706 100%)"
                />
                <TierBar
                  tierName="SILVER"
                  count={overview.customersByTier?.["SILVER"] ?? 0}
                  total={overview.totalCustomers}
                  perk="1.0x points baseline member"
                  color="linear-gradient(135deg, #9ca3af 0%, #6b7280 100%)"
                />
              </div>

              <div className="quick-nav-actions">
                <Link href="/admin/orders" className="quick-action-button">
                  View Orders Queue →
                </Link>
                <Link href="/admin/returns" className="quick-action-button secondary">
                  Inspect Returns (RMA) →
                </Link>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </AdminShell>
  );
}

function TimelineChart({
  points,
  onHover,
  hoveredDate
}: {
  points: RevenueTimelinePoint[];
  onHover: (point: RevenueTimelinePoint | null) => void;
  hoveredDate: string | null;
}) {
  if (points.length === 0) {
    return <div className="chart-empty">No timeline data available for this range.</div>;
  }

  const maxRevenue = Math.max(...points.map((p) => p.revenue), 10);
  const chartHeight = 220;
  const chartWidth = 900;
  const paddingX = 40;
  const paddingY = 24;

  const innerWidth = chartWidth - paddingX * 2;
  const innerHeight = chartHeight - paddingY * 2;

  // Build coordinate pairs
  const coords = points.map((p, idx) => {
    const x = paddingX + (idx / Math.max(1, points.length - 1)) * innerWidth;
    const y = chartHeight - paddingY - (p.revenue / maxRevenue) * innerHeight;
    return { x, y, point: p };
  });

  // Build SVG path
  const pathD = coords.reduce((acc, c, idx) => {
    return idx === 0 ? `M ${c.x} ${c.y}` : `${acc} L ${c.x} ${c.y}`;
  }, "");

  // Area path closing down to bottom
  const areaD = `${pathD} L ${coords[coords.length - 1].x} ${chartHeight - paddingY} L ${coords[0].x} ${chartHeight - paddingY} Z`;

  return (
    <div className="timeline-svg-container">
      <svg
        viewBox={`0 0 ${chartWidth} ${chartHeight}`}
        className="timeline-svg"
        preserveAspectRatio="none"
      >
        <defs>
          <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#145c52" stopOpacity="0.45" />
            <stop offset="100%" stopColor="#145c52" stopOpacity="0.02" />
          </linearGradient>
        </defs>

        {/* Grid lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((pct) => {
          const y = paddingY + pct * innerHeight;
          const val = maxRevenue * (1 - pct);
          return (
            <g key={pct}>
              <line
                x1={paddingX}
                y1={y}
                x2={chartWidth - paddingX}
                y2={y}
                stroke="#e2e8f0"
                strokeDasharray="4 4"
              />
              <text x={paddingX - 8} y={y + 4} textAnchor="end" fontSize="10" fill="#94a3b8">
                ${Math.round(val)}
              </text>
            </g>
          );
        })}

        {/* Shaded Area */}
        <path d={areaD} fill="url(#revenueGradient)" />

        {/* Main Line */}
        <path
          d={pathD}
          fill="none"
          stroke="#145c52"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Interactive Data Dots */}
        {coords.map((c) => {
          const isHovered = c.point.date === hoveredDate;
          return (
            <g
              key={c.point.date}
              onMouseEnter={() => onHover(c.point)}
              onMouseLeave={() => onHover(null)}
              style={{ cursor: "pointer" }}
            >
              <circle
                cx={c.x}
                cy={c.y}
                r={isHovered ? 7 : 4}
                fill={isHovered ? "#a9792b" : "#145c52"}
                stroke="#ffffff"
                strokeWidth={isHovered ? 3 : 2}
                className="chart-dot"
              />
              {/* Invisible larger target for easy hovering */}
              <circle cx={c.x} cy={c.y} r={16} fill="transparent" />
            </g>
          );
        })}
      </svg>

      {/* X Axis Labels */}
      <div className="timeline-x-labels">
        {points.map((p, idx) => {
          // Show fewer labels if 30 points to avoid overlap
          const showLabel = points.length <= 12 || idx % Math.ceil(points.length / 8) === 0 || idx === points.length - 1;
          return (
            <span
              key={p.date}
              className={`x-label ${p.date === hoveredDate ? "highlight" : ""}`}
              style={{ visibility: showLabel ? "visible" : "hidden" }}
            >
              {formatDateLabel(p.date)}
            </span>
          );
        })}
      </div>
    </div>
  );
}

function formatDateLabel(dateStr: string) {
  if (dateStr.length === 7) {
    // yyyy-MM
    return dateStr;
  }
  // yyyy-MM-dd -> MM/dd
  const parts = dateStr.split("-");
  return parts.length === 3 ? `${parts[1]}/${parts[2]}` : dateStr;
}

function buildStatusSegment(ordersByStatus: Record<string, number> | undefined, total: number) {
  if (total === 0 || !ordersByStatus) {
    return <div className="empty-track-placeholder">No orders recorded</div>;
  }

  const colors: Record<string, string> = {
    DELIVERED: "#166534",
    SHIPPED: "#0f766e",
    PROCESSING: "#1d4ed8",
    PAID: "#ca8a04",
    PENDING: "#d97706",
    CANCELLED: "#991b1b"
  };

  return (
    <div className="segmented-bar">
      {Object.entries(ordersByStatus ?? {}).map(([status, count]) => {
        if (count === 0) return null;
        const widthPct = Math.max(1, Math.round((count / total) * 100));
        return (
          <div
            key={status}
            className="segment"
            style={{ width: `${widthPct}%`, background: colors[status] ?? "#64748b" }}
            title={`${status}: ${count} orders (${widthPct}%)`}
          />
        );
      })}
    </div>
  );
}

function StatusLegendItem({
  label,
  count,
  total,
  color
}: {
  label: string;
  count: number;
  total: number;
  color: string;
}) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0;
  return (
    <div className="legend-chip">
      <span className="legend-color-dot" style={{ background: color }} />
      <span className="legend-name">{label}</span>
      <strong>{count}</strong>
      <span className="legend-pct">({pct}%)</span>
    </div>
  );
}

function TierBar({
  tierName,
  count,
  total,
  perk,
  color
}: {
  tierName: string;
  count: number;
  total: number;
  perk: string;
  color: string;
}) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0;
  return (
    <div className="tier-bar-row">
      <div className="tier-bar-meta">
        <span className="tier-title-label">{tierName}</span>
        <span className="tier-count-num"><strong>{count}</strong> customers ({pct}%)</span>
      </div>
      <div className="tier-track">
        <div className="tier-fill" style={{ width: `${Math.max(2, pct)}%`, background: color }} />
      </div>
      <span className="tier-perk-hint">{perk}</span>
    </div>
  );
}