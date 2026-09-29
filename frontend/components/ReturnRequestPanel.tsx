"use client";

import { useState } from "react";
import { Button, LinkButton } from "@/components/Button";
import { Price } from "@/components/Price";
import { createReturn, ReturnClientError } from "@/lib/returnClient";
import type { Order, ReturnRequest } from "@/lib/types";

export function ReturnRequestPanel({ order }: { order: Order }) {
  const [quantities, setQuantities] = useState<Record<string, number>>(() => Object.fromEntries(order.items.map((item) => [item.id, 0])));
  const [reasons, setReasons] = useState<Record<string, string>>(() => Object.fromEntries(order.items.map((item) => [item.id, ""]))) ;
  const [customerNote, setCustomerNote] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [createdReturn, setCreatedReturn] = useState<ReturnRequest | null>(null);

  const eligible = order.status === "DELIVERED" && Boolean(order.shipment?.deliveredAt);
  if (!eligible) {
    return null;
  }

  async function submitReturn() {
    const items = order.items
      .map((item) => ({ orderItemId: item.id, quantity: quantities[item.id] ?? 0, reason: (reasons[item.id] ?? "").trim() }))
      .filter((item) => item.quantity > 0);
    if (items.length === 0) {
      setError("Select at least one item quantity to return.");
      return;
    }
    if (items.some((item) => !item.reason)) {
      setError("Add a reason for every returned item.");
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      setCreatedReturn(await createReturn(order.id, { customerNote: customerNote.trim() || undefined, items }));
    } catch (caught) {
      setError(caught instanceof ReturnClientError || caught instanceof Error ? caught.message : "Unable to request return.");
    } finally {
      setSubmitting(false);
    }
  }

  if (createdReturn) {
    return (
      <section className="checkout-panel checkout-summary" aria-label="Return request">
        <p className="eyebrow">Return</p>
        <h2>Return requested</h2>
        <p className="muted-copy">Your request is waiting for admin review.</p>
        <LinkButton href={`/account/returns/${createdReturn.id}`}>View return</LinkButton>
      </section>
    );
  }

  return (
    <section className="checkout-panel checkout-summary" aria-label="Return request">
      <p className="eyebrow">Return</p>
      <h2>Request a return</h2>
      <p className="muted-copy">Delivered items can be requested for return within the configured return window.</p>
      {error ? <p className="form-message error" role="alert">{error}</p> : null}
      <div className="admin-collection compact">
        {order.items.map((item) => (
          <div className="admin-variant-card" key={item.id}>
            <div>
              <strong>{item.productName}</strong>
              <small>SKU {item.sku}</small>
            </div>
            <div><span className="muted-copy">Purchased</span><strong>{item.quantity}</strong></div>
            <div><span className="muted-copy">Unit</span><Price amount={item.unitPrice} /></div>
            <label className="field-label">Qty
              <input min="0" max={item.quantity} type="number" value={quantities[item.id] ?? 0} onChange={(event) => setQuantities((current) => ({ ...current, [item.id]: Math.min(item.quantity, Math.max(0, Number(event.target.value))) }))} />
            </label>
            <label className="field-label">Reason
              <input value={reasons[item.id] ?? ""} onChange={(event) => setReasons((current) => ({ ...current, [item.id]: event.target.value }))} />
            </label>
          </div>
        ))}
      </div>
      <label className="field-label field-wide">Note
        <textarea rows={3} value={customerNote} onChange={(event) => setCustomerNote(event.target.value)} />
      </label>
      <Button type="button" disabled={submitting} onClick={submitReturn}>{submitting ? "Requesting" : "Request Return"}</Button>
    </section>
  );
}
