"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { useAuth } from "@/components/AuthProvider";
import { cancelReturn, getReturn, markReturnShipped, ReturnClientError } from "@/lib/returnClient";
import type { ReturnRequest } from "@/lib/types";

export function AccountReturnDetailsView({ returnId }: { returnId: string }) {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [request, setRequest] = useState<ReturnRequest | null>(null);
  const [busy, setBusy] = useState(false);
  const [dataLoading, setDataLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => { if (!loading && !user) router.replace("/login"); }, [loading, router, user]);
  const load = useCallback(async () => {
    setDataLoading(true); setError("");
    try { setRequest(await getReturn(returnId)); }
    catch (caught) { if (caught instanceof ReturnClientError && caught.status === 401) { router.replace("/login"); return; } setError(caught instanceof Error ? caught.message : "Unable to load return."); }
    finally { setDataLoading(false); }
  }, [returnId, router]);
  useEffect(() => { if (!loading && user) void Promise.resolve().then(load); }, [loading, load, user]);

  async function cancel() {
    if (!request || !window.confirm(`Cancel return ${request.id.slice(0, 8)}?`)) return;
    setBusy(true); setError("");
    try { setRequest(await cancelReturn(request.id)); }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to cancel return."); }
    finally { setBusy(false); }
  }

  async function markShipped() {
    if (!request) return;
    setBusy(true); setError("");
    try { setRequest(await markReturnShipped(request.id)); }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Unable to mark return shipped."); }
    finally { setBusy(false); }
  }

  if (loading || dataLoading) return <div className="state-panel" aria-busy="true">Loading return...</div>;
  if (!user) return <EmptyState title="Login required" message="Sign in to view this return." />;
  if (error && !request) return <ErrorState title="Return unavailable" message={error} />;
  if (!request) return <EmptyState title="Return not found" message="We could not find that return request." />;

  return (
    <section className="account-layout order-history-layout">
      <div className="account-copy"><p className="eyebrow">Return {request.id.slice(0, 8)}</p><h1>{request.status}</h1><p>Order {request.orderId.slice(0, 8)}</p><LinkButton href={`/orders/${request.orderId}`} variant="secondary">View order</LinkButton></div>
      <div className="account-stack">
        {error ? <p className="form-message error">{error}</p> : null}
        <section className="checkout-panel checkout-summary"><p className="eyebrow">Items</p>{request.items.map((item) => <article className="checkout-item" key={item.id}><div><h3>{item.productName}</h3><p>SKU {item.sku}</p><p className="meta-line">Requested {item.requestedQuantity} / Approved {item.approvedQuantity} / Received {item.receivedQuantity}</p><p className="muted-copy">{item.reason}</p></div><Price amount={String(Number(item.unitPrice) * item.approvedQuantity)} /></article>)}</section>
        <section className="checkout-panel checkout-summary"><p className="eyebrow">Return shipment</p>{request.shipment ? <div className="admin-grid compact"><div><span>Carrier</span><strong>{request.shipment.carrier}</strong></div><div><span>Tracking</span><strong>{request.shipment.trackingNumber}</strong></div><div><span>Label</span><strong>{request.shipment.mockLabelReference}</strong></div><div><span>Shipped</span><strong>{request.shipment.shippedAt ? formatDate(request.shipment.shippedAt) : "Waiting"}</strong></div><div><span>Received</span><strong>{request.shipment.receivedAt ? formatDate(request.shipment.receivedAt) : "Pending"}</strong></div></div> : <p className="muted-copy">{request.status === "APPROVED" ? "A return label will appear here after support generates it." : "Return shipping details appear after approval."}</p>}{request.status === "APPROVED" && request.shipment && !request.shipment.shippedAt ? <Button type="button" variant="secondary" disabled={busy} onClick={markShipped}>{busy ? "Saving" : "Mark as Shipped"}</Button> : null}</section>
        <section className="checkout-panel checkout-summary"><p className="eyebrow">Refund</p><h2><Price amount={request.receivedRefundAmount !== "0.00" ? request.receivedRefundAmount : request.estimatedRefund} /></h2><p className="muted-copy">Refund is processed only after returned goods are received.</p>{request.status === "REQUESTED" || request.status === "APPROVED" ? <Button type="button" variant="secondary" disabled={busy} onClick={cancel}>{busy ? "Cancelling" : "Cancel Return"}</Button> : null}</section>
        <section className="checkout-panel checkout-summary"><p className="eyebrow">History</p>{request.statusHistory.map((entry) => <div className="admin-list-item" key={entry.id}><span>{entry.fromStatus}{" -> "}{entry.toStatus}</span><small>{formatDate(entry.changedAt)}</small></div>)}</section>
      </div>
    </section>
  );
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
