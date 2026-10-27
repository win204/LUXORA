"use client";

import { useEffect, useState, useCallback } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { useAuth } from "@/components/AuthProvider";
import {
  getAdminFlashSales,
  createAdminFlashSale,
  cancelAdminFlashSale
} from "@/lib/flashSaleClient";
import { getProducts } from "@/lib/api";
import type { FlashSale, ProductListItem } from "@/lib/types";

export function AdminFlashSalesView() {
  const { accessToken } = useAuth();
  const [sales, setSales] = useState<FlashSale[]>([]);
  const [products, setProducts] = useState<ProductListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  // Create form state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [bannerImageUrl, setBannerImageUrl] = useState("");
  const [selectedProductId, setSelectedProductId] = useState("");
  const [flashPrice, setFlashPrice] = useState("");
  const [stockQuantity, setStockQuantity] = useState("10");
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setError("");
    try {
      const [salesData, productsData] = await Promise.all([
        getAdminFlashSales(accessToken || ""),
        getProducts({ size: "50" })
      ]);
      setSales(salesData);
      setProducts(productsData.content);
      if (productsData.content.length > 0) {
        setSelectedProductId((prev) => prev || productsData.content[0].id);
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Failed to load flash sales.");
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    let active = true;

    async function initialLoad() {
      try {
        const [salesData, productsData] = await Promise.all([
          getAdminFlashSales(accessToken || ""),
          getProducts({ size: "50" })
        ]);
        if (active) {
          setSales(salesData);
          setProducts(productsData.content);
          if (productsData.content.length > 0) {
            setSelectedProductId(productsData.content[0].id);
          }
        }
      } catch (caught) {
        if (active) {
          setError(caught instanceof Error ? caught.message : "Failed to load flash sales.");
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    void initialLoad();
    return () => {
      active = false;
    };
  }, [accessToken]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!accessToken || !selectedProductId || !flashPrice || !startTime || !endTime) return;
    setSubmitting(true);
    setError("");
    setActionSuccess("");

    try {
      const payload = {
        name,
        description: description || undefined,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        bannerImageUrl: bannerImageUrl || undefined,
        items: [
          {
            productId: selectedProductId,
            flashSalePrice: parseFloat(flashPrice),
            stockQuantity: parseInt(stockQuantity, 10) || 10
          }
        ]
      };

      await createAdminFlashSale(payload, accessToken);
      setActionSuccess("Flash sale campaign created successfully!");
      setShowCreateModal(false);
      setName("");
      setDescription("");
      setStartTime("");
      setEndTime("");
      setBannerImageUrl("");
      setFlashPrice("");
      await loadData();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Failed to create flash sale.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(id: string) {
    if (!accessToken) return;
    if (!confirm("Are you sure you want to cancel this flash sale campaign?")) return;
    try {
      await cancelAdminFlashSale(id, accessToken);
      setActionSuccess("Campaign cancelled successfully.");
      await loadData();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Failed to cancel flash sale.");
    }
  }

  return (
    <AdminShell>
      <div className="admin-heading admin-dashboard-heading">
        <div>
          <p className="eyebrow">Marketing & Promotions</p>
          <h1>Flash Sales Campaigns</h1>
        </div>
        <button
          type="button"
          className="button"
          onClick={() => setShowCreateModal(true)}
        >
          + New Flash Sale
        </button>
      </div>

      {actionSuccess ? <div className="success-panel" style={{ marginBottom: 16 }}>{actionSuccess}</div> : null}
      {error ? <ErrorState title="Operation failed" message={error} /> : null}
      {loading ? <div className="state-panel" aria-busy="true">Loading campaigns...</div> : null}

      {!loading && sales.length === 0 ? (
        <div className="empty-panel">
          No flash sale campaigns created yet. Click &apos;+ New Flash Sale&apos; to launch one.
        </div>
      ) : null}

      <div className="admin-collection">
        {sales.map((sale) => (
          <article key={sale.id} className="admin-panel-box" style={{ marginBottom: 16 }}>
            <div className="panel-box-header">
              <div>
                <span className={`status-pill status-${sale.status.toLowerCase()}`}>
                  {sale.status}
                </span>
                <h2 style={{ marginTop: 8 }}>{sale.name}</h2>
                {sale.description ? <p className="muted-copy">{sale.description}</p> : null}
              </div>

              {sale.status === "ACTIVE" || sale.status === "UPCOMING" ? (
                <button
                  type="button"
                  className="button secondary"
                  style={{ color: "#991b1b" }}
                  onClick={() => void handleCancel(sale.id)}
                >
                  Cancel Sale
                </button>
              ) : null}
            </div>

            <div className="flash-sale-meta-row" style={{ display: "flex", gap: 24, fontSize: "0.85rem", color: "var(--muted)" }}>
              <span><strong>Starts:</strong> {new Date(sale.startTime).toLocaleString()}</span>
              <span><strong>Ends:</strong> {new Date(sale.endTime).toLocaleString()}</span>
              {sale.status === "ACTIVE" ? (
                <span style={{ color: "#a9792b", fontWeight: 700 }}>
                  ⏳ {Math.floor(sale.timeRemainingSeconds / 3600)}h {Math.floor((sale.timeRemainingSeconds % 3600) / 60)}m remaining
                </span>
              ) : null}
            </div>

            <div className="flash-sale-items-preview" style={{ marginTop: 14 }}>
              <p className="eyebrow" style={{ marginBottom: 8 }}>Campaign Products ({sale.items.length})</p>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12 }}>
                {sale.items.map((it) => (
                  <div
                    key={it.id}
                    style={{
                      border: "1px solid var(--line)",
                      borderRadius: 8,
                      padding: 12,
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      background: "var(--surface-soft)"
                    }}
                  >
                    <div>
                      <strong>{it.productName}</strong>
                      {it.variantName ? <p style={{ margin: "2px 0", fontSize: "0.75rem", color: "var(--muted)" }}>{it.variantName}</p> : null}
                      <p style={{ margin: "4px 0 0", fontSize: "0.78rem" }}>
                        Sold: <strong>{it.soldQuantity}</strong> / {it.stockQuantity} ({it.percentClaimed}%)
                      </p>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <Price amount={String(it.flashSalePrice)} />
                      <span style={{ display: "block", fontSize: "0.75rem", color: "var(--muted)", textDecoration: "line-through" }}>
                        ${it.originalPrice.toFixed(2)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </article>
        ))}
      </div>

      {showCreateModal ? (
        <div className="modal-backdrop" style={{
          position: "fixed",
          inset: 0,
          background: "rgba(0,0,0,0.5)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 100
        }}>
          <div className="modal-card" style={{
            background: "var(--surface)",
            border: "1px solid var(--line)",
            borderRadius: 12,
            padding: 24,
            maxWidth: 520,
            width: "90%",
            maxHeight: "90vh",
            overflowY: "auto"
          }}>
            <h2>Create Flash Sale Campaign</h2>
            <form onSubmit={(e) => void handleCreate(e)} style={{ display: "flex", flexDirection: "column", gap: 14, marginTop: 16 }}>
              <div>
                <label className="field-label">Campaign Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. VIP Midnight Flash Deal"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                />
              </div>

              <div>
                <label className="field-label">Description</label>
                <input
                  type="text"
                  placeholder="Short promotion tagline"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                />
              </div>

              <div>
                <label className="field-label">Banner Image URL</label>
                <input
                  type="text"
                  placeholder="https://..."
                  value={bannerImageUrl}
                  onChange={(e) => setBannerImageUrl(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <div>
                  <label className="field-label">Start Time *</label>
                  <input
                    type="datetime-local"
                    required
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                  />
                </div>
                <div>
                  <label className="field-label">End Time *</label>
                  <input
                    type="datetime-local"
                    required
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                  />
                </div>
              </div>

              <div>
                <label className="field-label">Featured Product *</label>
                <select
                  required
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.slug})
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <div>
                  <label className="field-label">Flash Sale Price ($) *</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    placeholder="e.g. 199.00"
                    value={flashPrice}
                    onChange={(e) => setFlashPrice(e.target.value)}
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                  />
                </div>
                <div>
                  <label className="field-label">Allocated Quota *</label>
                  <input
                    type="number"
                    required
                    min="1"
                    value={stockQuantity}
                    onChange={(e) => setStockQuantity(e.target.value)}
                    style={{ width: "100%", padding: "8px 12px", borderRadius: 6, border: "1px solid var(--line)" }}
                  />
                </div>
              </div>

              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 10 }}>
                <button
                  type="button"
                  className="button secondary"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="button"
                  disabled={submitting}
                >
                  {submitting ? "Launching..." : "Launch Flash Sale"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </AdminShell>
  );
}
