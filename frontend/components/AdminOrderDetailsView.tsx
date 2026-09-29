"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { cancelAdminOrder, createAdminShipment, getAdminOrder, refundAndCancelAdminOrder, updateAdminOrderNote, updateAdminOrderStatus, updateAdminShipment } from "@/lib/adminClient";
import type { AdminOrderDetail, MockRefundOutcome, OrderStatus } from "@/lib/types";

const transitionLabels: Partial<Record<OrderStatus, { next: OrderStatus; label: string }>> = {
  PAID: { next: "PROCESSING", label: "Mark as Processing" },
  SHIPPED: { next: "DELIVERED", label: "Mark as Delivered" }
};

const refundableStatuses: OrderStatus[] = ["PAID", "PROCESSING"];

export function AdminOrderDetailsView({ orderId }: { orderId: string }) {
  const [order, setOrder] = useState<AdminOrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [refunding, setRefunding] = useState<MockRefundOutcome | null>(null);
  const [carrier, setCarrier] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");
  const [adminNote, setAdminNote] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const nextOrder = await getAdminOrder(orderId);
        if (active) {
          setOrder(nextOrder);
          setCarrier(nextOrder.shipment?.carrier ?? "");
          setTrackingNumber(nextOrder.shipment?.trackingNumber ?? "");
          setAdminNote(nextOrder.adminNote ?? "");
        }
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : "Unable to load order.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [orderId]);

  async function saveNote() {
    if (!order) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const nextOrder = await updateAdminOrderNote(order.id, { note: adminNote });
      setOrder(nextOrder);
      setAdminNote(nextOrder.adminNote ?? "");
      setSuccess(adminNote.trim() ? "Operational note saved." : "Operational note cleared.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to save the operational note.");
    } finally {
      setSaving(false);
    }
  }
  async function cancelOrder() {
    if (!order) return;
    const confirmed = window.confirm(`Cancel order ${order.id.slice(0, 8)}? Inventory will be restored.`);
    if (!confirmed) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      setOrder(await cancelAdminOrder(order.id));
      setSuccess("Order cancelled.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to cancel order.");
    } finally {
      setSaving(false);
    }
  }

  async function saveShipment(mode: "create" | "update") {
    if (!order) return;
    if (!carrier.trim() || !trackingNumber.trim()) {
      setError("Carrier and tracking number are required.");
      return;
    }
    const confirmed = window.confirm(mode === "create" ? `Mark order ${order.id.slice(0, 8)} as shipped?` : `Update tracking for order ${order.id.slice(0, 8)}?`);
    if (!confirmed) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const payload = { carrier: carrier.trim(), trackingNumber: trackingNumber.trim() };
      const nextOrder = mode === "create" ? await createAdminShipment(order.id, payload) : await updateAdminShipment(order.id, payload);
      setOrder(nextOrder);
      setCarrier(nextOrder.shipment?.carrier ?? "");
      setTrackingNumber(nextOrder.shipment?.trackingNumber ?? "");
      setSuccess(mode === "create" ? "Order marked as shipped." : "Shipment updated.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to save shipment.");
    } finally {
      setSaving(false);
    }
  }

  async function refundAndCancel(mockOutcome: MockRefundOutcome) {
    if (!order) return;
    const confirmed = window.confirm(`Run mock ${mockOutcome.toLowerCase()} refund and cancel order ${order.id.slice(0, 8)}?`);
    if (!confirmed) return;
    setRefunding(mockOutcome);
    setError("");
    setSuccess("");
    try {
      const nextOrder = await refundAndCancelAdminOrder(order.id, {
        mockOutcome,
        reason: "Admin refund and cancel"
      });
      setOrder(nextOrder);
      setSuccess(mockOutcome === "SUCCEEDED" ? "Refund succeeded and order was cancelled." : "Mock refund failed. Order status was unchanged.");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to refund order.");
    } finally {
      setRefunding(null);
    }
  }

  async function transition(status: OrderStatus) {
    if (!order) return;
    const confirmed = window.confirm(`Change order ${order.id.slice(0, 8)} to ${status}?`);
    if (!confirmed) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      setOrder(await updateAdminOrderStatus(order.id, status));
      setSuccess(`Order marked as ${status}.`);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to update order status.");
    } finally {
      setSaving(false);
    }
  }

  const nextAction = order ? transitionLabels[order.status] : undefined;
  const canRefund = order ? refundableStatuses.includes(order.status) : false;
  const busy = saving || refunding !== null;

  return (
    <AdminShell>
      <div className="admin-heading admin-heading-row">
        <div>
          <p className="eyebrow">Orders</p>
          <h1>Order {orderId.slice(0, 8)}</h1>
        </div>
        <LinkButton href="/admin/orders" variant="secondary">Orders</LinkButton>
      </div>

      {loading ? <div className="state-panel" aria-busy="true">Loading order...</div> : null}
      {error ? <ErrorState title="Order request failed" message={error} /> : null}
      {success ? <div className="success-panel">{success}</div> : null}
      {!loading && !error && !order ? <EmptyState title="Order not found" message="This order could not be loaded." /> : null}

      {order ? (
        <div className="admin-form-stack">
          <section className="admin-panel admin-order-hero">
            <div>
              <p className="eyebrow">Current status</p>
              <h2>{order.status}</h2>
              <p>{new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(order.createdAt))}</p>
            </div>
            <div className="payment-actions">
              {nextAction ? <Button type="button" disabled={busy} onClick={() => transition(nextAction.next)}>{saving ? "Updating..." : nextAction.label}</Button> : null}
              {order.status === "PENDING" ? <Button type="button" variant="secondary" disabled={busy} onClick={cancelOrder}>{saving ? "Updating..." : "Cancel Order"}</Button> : null}
              {canRefund ? <Button type="button" variant="secondary" disabled={busy} onClick={() => refundAndCancel("SUCCEEDED")}>{refunding === "SUCCEEDED" ? "Refunding..." : "Refund & Cancel"}</Button> : null}
              {canRefund ? <Button type="button" variant="secondary" disabled={busy} onClick={() => refundAndCancel("FAILED")}>{refunding === "FAILED" ? "Simulating..." : "Simulate Refund Failure"}</Button> : null}
              {!nextAction && order.status !== "PENDING" && !canRefund && order.status !== "PROCESSING" ? <p className="muted-copy">No admin transition is available.</p> : null}
            </div>
          </section>

          <section className="admin-panel">
            <h2>Operational note</h2>
            <label className="field-label">Fulfillment note<textarea maxLength={500} value={adminNote} onChange={(event) => setAdminNote(event.target.value)} placeholder="Add handling, fulfillment, or customer-service context." rows={4} /></label>
            <div className="admin-actions-inline"><small className="muted-copy">{adminNote.length}/500</small><Button type="button" disabled={busy} onClick={saveNote}>{saving ? "Saving..." : "Save Note"}</Button></div>
          </section>

          <section className="admin-panel">
            <h2>Customer and shipping</h2>
            <div className="admin-detail-grid">
              <div><span>Customer</span><strong>{order.userEmail}</strong></div>
              <div><span>Recipient</span><strong>{order.shippingAddress.recipientName}</strong></div>
              <div><span>Phone</span><strong>{order.shippingAddress.phone}</strong></div>
              <div><span>Address</span><strong>{order.shippingAddress.addressLine1}</strong><small>{order.shippingAddress.city}, {order.shippingAddress.province}, {order.shippingAddress.country}</small></div>
            </div>
          </section>

          {(order.status === "PROCESSING" || order.status === "SHIPPED" || order.status === "DELIVERED") ? (
            <section className="admin-panel">
              <h2>Shipment</h2>
              {order.shipment ? (
                <div className="admin-detail-grid">
                  <div><span>Carrier</span><strong>{order.shipment.carrier}</strong></div>
                  <div><span>Tracking</span><strong>{order.shipment.trackingNumber}</strong></div>
                  <div><span>Shipped</span><strong>{new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(order.shipment.shippedAt))}</strong></div>
                  <div><span>Delivered</span><strong>{order.shipment.deliveredAt ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(order.shipment.deliveredAt)) : "Pending"}</strong></div>
                </div>
              ) : <p className="muted-copy">Shipment metadata has not been added yet.</p>}
              {(order.status === "PROCESSING" || order.status === "SHIPPED") ? (
                <div className="admin-inline-form">
                  <label>Carrier<input value={carrier} onChange={(event) => setCarrier(event.target.value)} /></label>
                  <label>Tracking number<input value={trackingNumber} onChange={(event) => setTrackingNumber(event.target.value)} /></label>
                  <Button type="button" disabled={busy} onClick={() => saveShipment(order.status === "PROCESSING" ? "create" : "update")}>{saving ? "Saving..." : order.status === "PROCESSING" ? "Mark as Shipped" : "Update Tracking"}</Button>
                </div>
              ) : null}
            </section>
          ) : null}

          <section className="admin-panel">
            <h2>Items</h2>
            <div className="admin-collection compact">
              {order.items.map((item) => (
                <article className="admin-list-item" key={item.id}>
                  <span><strong>{item.productName}</strong><small>SKU {item.sku} x {item.quantity}</small></span>
                  <Price amount={item.lineTotal} />
                </article>
              ))}
            </div>
          </section>

          <section className="admin-panel">
            <h2>Totals and payment</h2>
            <div className="admin-detail-grid">
              <div><span>Subtotal</span><strong><Price amount={order.subtotal} /></strong></div>
              <div><span>Shipping</span><strong><Price amount={order.shippingFee} /></strong></div>
              <div><span>Tax</span><strong><Price amount={order.tax} /></strong></div>
              <div><span>Discount</span><strong><Price amount={order.discount} /></strong></div>
              <div><span>Grand total</span><strong><Price amount={order.grandTotal} /></strong></div>
              <div><span>Payment</span><strong>{order.latestPayment?.status ?? "NO_ATTEMPT"}</strong><small>{order.latestPayment ? order.latestPayment.provider : "No payment attempt"}</small></div>
              <div><span>Refund</span><strong>{order.latestRefund?.status ?? "NO_ATTEMPT"}</strong><small>{order.latestRefund ? `${order.latestRefund.provider} - ${order.latestRefund.reason ?? "No reason"}` : "No refund attempt"}</small></div>
            </div>
          </section>

          <section className="admin-panel">
            <h2>Status history</h2>
            {order.statusHistory.length === 0 ? <p className="muted-copy">No admin status changes yet.</p> : null}
            <div className="admin-collection compact">
              {order.statusHistory.map((entry) => (
                <article className="admin-list-item" key={entry.id}>
                  <span><strong>{entry.fromStatus} to {entry.toStatus}</strong><small>{entry.changedByEmail ?? "Unknown admin"}</small></span>
                  <span>{new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(entry.changedAt))}</span>
                </article>
              ))}
            </div>
          </section>
        </div>
      ) : null}
    </AdminShell>
  );
}

