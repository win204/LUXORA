"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { approveAdminReturn, generateAdminReturnShippingLabel, getAdminReturn, markAdminReturnReceived, refundAdminReturn, rejectAdminReturn, updateAdminReturnNote } from "@/lib/adminClient";
import type { MockRefundOutcome, ReturnRequest } from "@/lib/types";

export function AdminReturnDetailsView({ returnId }: { returnId: string }) {
  const [request, setRequest] = useState<ReturnRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [adminNote, setAdminNote] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true); setError("");
      try { const next = await getAdminReturn(returnId); if (active) { setRequest(next); setAdminNote(next.adminNote ?? ""); } }
      catch (caught) { if (active) setError(caught instanceof Error ? caught.message : "Unable to load return."); }
      finally { if (active) setLoading(false); }
    }
    void load();
    return () => { active = false; };
  }, [returnId]);

  async function run(label: string, action: () => Promise<ReturnRequest>, message: string) {
    if (!request) return;
    setBusy(label); setError(""); setSuccess("");
    try { const next = await action(); setRequest(next); setAdminNote(next.adminNote ?? ""); setSuccess(message); }
    catch (caught) { setError(caught instanceof Error ? caught.message : "Return action failed."); }
    finally { setBusy(null); }
  }

  function saveNote() {
    if (!request) return;
    if (adminNote.length > 500) { setError("Operational note cannot exceed 500 characters."); return; }
    void run("note", () => updateAdminReturnNote(request.id, { note: adminNote }), adminNote.trim() ? "Operational note saved." : "Operational note cleared.");
  }

  function approve() {
    if (!request || !window.confirm(`Approve return ${request.id.slice(0, 8)}?`)) return;
    void run("approve", () => approveAdminReturn(request.id, { adminNote: adminNote.trim() || undefined }), "Return approved.");
  }

  function reject() {
    if (!request) return;
    const note = adminNote.trim();
    if (!note) { setError("A rejection reason is required."); return; }
    if (!window.confirm(`Reject return ${request.id.slice(0, 8)}?`)) return;
    void run("reject", () => rejectAdminReturn(request.id, { adminNote: note }), "Return rejected.");
  }

  function receive() {
    if (!request || !window.confirm(`Mark return ${request.id.slice(0, 8)} as received and restock accepted quantities?`)) return;
    void run("receive", () => markAdminReturnReceived(request.id, {}), "Return received and inventory restocked.");
  }

  function generateLabel() {
    if (!request || !window.confirm(`Generate a mock return label for ${request.id.slice(0, 8)}?`)) return;
    void run("label", () => generateAdminReturnShippingLabel(request.id), "Return label generated.");
  }

  function refund(outcome: MockRefundOutcome) {
    if (!request || !window.confirm(`Run mock ${outcome.toLowerCase()} refund for return ${request.id.slice(0, 8)}?`)) return;
    void run(`refund-${outcome}`, () => refundAdminReturn(request.id, { mockOutcome: outcome, reason: adminNote.trim() || "Return refund" }), outcome === "SUCCEEDED" ? "Return refunded." : "Mock refund failed. Return remains received.");
  }

  return (
    <AdminShell>
      {loading ? <div className="state-panel" aria-busy="true">Loading return...</div> : null}
      {error && !request ? <ErrorState title="Return unavailable" message={error} /> : null}
      {!loading && !request && !error ? <EmptyState title="Return not found" message="That return request does not exist." /> : null}
      {request ? (
        <div className="admin-form-stack">
          <div className="admin-order-hero"><div><p className="eyebrow">Return {request.id.slice(0, 8)}</p><h1>{request.status}</h1><p className="muted-copy">Order {request.orderId.slice(0, 8)} - {request.userEmail}</p></div><LinkButton href={`/admin/orders/${request.orderId}`} variant="secondary">Customer order</LinkButton></div>
          {error ? <p className="form-message error" role="alert">{error}</p> : null}
          {success ? <p className="form-message success">{success}</p> : null}
          <section className="admin-panel"><h2>Items</h2><div className="admin-collection compact">{request.items.map((item) => <div className="admin-variant-card" key={item.id}><div><strong>{item.productName}</strong><small>SKU {item.sku}</small></div><div><span>Requested</span><strong>{item.requestedQuantity}</strong></div><div><span>Approved</span><strong>{item.approvedQuantity}</strong></div><div><span>Received</span><strong>{item.receivedQuantity}</strong></div><div><Price amount={String(Number(item.unitPrice) * Math.max(item.receivedQuantity, item.approvedQuantity))} /></div></div>)}</div></section>
          <section className="admin-panel"><h2>Return shipment</h2>{request.shipment ? <div className="admin-grid compact"><div><span>Carrier</span><strong>{request.shipment.carrier}</strong></div><div><span>Tracking</span><strong>{request.shipment.trackingNumber}</strong></div><div><span>Label</span><strong>{request.shipment.mockLabelReference}</strong></div><div><span>Customer shipped</span><strong>{request.shipment.shippedAt ? formatDate(request.shipment.shippedAt) : "Waiting"}</strong></div><div><span>Received</span><strong>{request.shipment.receivedAt ? formatDate(request.shipment.receivedAt) : "Pending"}</strong></div></div> : <p className="muted-copy">{request.status === "APPROVED" ? "Generate a mock label before receiving this return." : "Return shipment details are available after approval."}</p>}</section>
          <section className="admin-panel"><h2>Operational note</h2><label className="field-label field-wide">Note<textarea maxLength={500} rows={3} value={adminNote} onChange={(event) => setAdminNote(event.target.value)} /></label><div className="admin-actions-inline"><Button type="button" variant="secondary" disabled={busy !== null} onClick={saveNote}>{busy === "note" ? "Saving" : "Save note"}</Button><small>{adminNote.length}/500</small></div></section>
          <section className="admin-panel"><h2>Actions</h2><div className="admin-actions-inline">{request.status === "REQUESTED" ? <><Button type="button" disabled={busy !== null} onClick={approve}>{busy === "approve" ? "Approving" : "Approve"}</Button><Button type="button" variant="secondary" disabled={busy !== null} onClick={reject}>{busy === "reject" ? "Rejecting" : "Reject"}</Button></> : null}{request.status === "APPROVED" && !request.shipment ? <Button type="button" disabled={busy !== null} onClick={generateLabel}>{busy === "label" ? "Generating" : "Generate Label"}</Button> : null}{request.status === "APPROVED" && request.shipment ? <Button type="button" disabled={busy !== null} onClick={receive}>{busy === "receive" ? "Receiving" : "Mark Received"}</Button> : null}{request.status === "RECEIVED" ? <><Button type="button" disabled={busy !== null} onClick={() => refund("SUCCEEDED")}>{busy === "refund-SUCCEEDED" ? "Refunding" : "Refund"}</Button><Button type="button" variant="secondary" disabled={busy !== null} onClick={() => refund("FAILED")}>{busy === "refund-FAILED" ? "Processing" : "Mock Failure"}</Button></> : null}</div></section>
          <section className="admin-panel"><h2>History</h2>{request.statusHistory.map((entry) => <div className="admin-list-item" key={entry.id}><span>{entry.fromStatus}{" -> "}{entry.toStatus}</span><small>{formatDate(entry.changedAt)} {entry.changedByEmail ? `- ${entry.changedByEmail}` : ""}</small></div>)}</section>
        </div>
      ) : null}
    </AdminShell>
  );
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }