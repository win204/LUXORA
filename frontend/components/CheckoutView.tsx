"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { Price } from "@/components/Price";
import { useAuth } from "@/components/AuthProvider";
import { CheckoutClientError, previewCheckout } from "@/lib/checkoutClient";
import { createOrder, OrderClientError } from "@/lib/orderClient";
import type { CheckoutAddress, CheckoutPreview } from "@/lib/types";

const initialAddress: CheckoutAddress = {
  recipientName: "",
  phone: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  province: "",
  country: "Vietnam",
  postalCode: ""
};

export function CheckoutView() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [address, setAddress] = useState<CheckoutAddress>(initialAddress);
  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [error, setError] = useState("");
  const [promotionCode, setPromotionCode] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, router, user]);

  if (loading) {
    return <div className="state-panel">Preparing checkout...</div>;
  }

  if (!user) {
    return <EmptyState title="Login required" message="Sign in to review checkout for your saved cart." />;
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    setPreview(null);

    try {
      setPreview(await previewCheckout(normalizedAddress(), promotionCode));
    } catch (caught) {
      if (caught instanceof CheckoutClientError && caught.status === 401) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to preview checkout.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handlePlaceOrder() {
    setPlacingOrder(true);
    setError("");

    try {
      const order = await createOrder(normalizedAddress());
      router.push(`/orders/${order.id}`);
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to place order.");
    } finally {
      setPlacingOrder(false);
    }
  }

  function normalizedAddress() {
    return {
      recipientName: address.recipientName.trim(),
      phone: address.phone.trim(),
      addressLine1: address.addressLine1.trim(),
      addressLine2: address.addressLine2?.trim(),
      city: address.city.trim(),
      province: address.province.trim(),
      country: address.country.trim(),
      postalCode: address.postalCode?.trim()
    } satisfies CheckoutAddress;
  }

  function updateField(field: keyof CheckoutAddress, value: string) {
    setAddress((current) => ({ ...current, [field]: value }));
    setPreview(null);
  }

  return (
    <section className="checkout-layout">
      <div className="checkout-copy">
        <p className="eyebrow">Checkout</p>
        <h1>Review your order</h1>
        <p>Preview confirms your current cart, stock, and prices from LUXORA before an order is created. Payment is not enabled yet.</p>
      </div>

      <div className="checkout-stack">
        <form className="checkout-panel checkout-form" onSubmit={handleSubmit}>
          <div>
            <p className="eyebrow">Shipping</p>
            <h2>Delivery details</h2>
          </div>
          <label>
            <span>Recipient name</span>
            <input value={address.recipientName} maxLength={160} required onChange={(event) => updateField("recipientName", event.target.value)} />
          </label>
          <label>
            <span>Phone</span>
            <input value={address.phone} maxLength={32} required inputMode="tel" autoComplete="tel" onChange={(event) => updateField("phone", event.target.value)} />
          </label>
          <label>
            <span>Address line 1</span>
            <input value={address.addressLine1} maxLength={240} required autoComplete="address-line1" onChange={(event) => updateField("addressLine1", event.target.value)} />
          </label>
          <label>
            <span>Address line 2</span>
            <input value={address.addressLine2 ?? ""} maxLength={240} autoComplete="address-line2" onChange={(event) => updateField("addressLine2", event.target.value)} />
          </label>
          <div className="checkout-field-grid">
            <label>
              <span>City</span>
              <input value={address.city} maxLength={120} required autoComplete="address-level2" onChange={(event) => updateField("city", event.target.value)} />
            </label>
            <label>
              <span>Province</span>
              <input value={address.province} maxLength={120} required autoComplete="address-level1" onChange={(event) => updateField("province", event.target.value)} />
            </label>
          </div>
          <div className="checkout-field-grid">
            <label>
              <span>Country</span>
              <input value={address.country} maxLength={120} required autoComplete="country-name" onChange={(event) => updateField("country", event.target.value)} />
            </label>
            <label>
              <span>Postal code</span>
              <input value={address.postalCode ?? ""} maxLength={24} autoComplete="postal-code" onChange={(event) => updateField("postalCode", event.target.value)} />
            </label>
          </div>

          <label><span>Promotion code</span><input value={promotionCode} maxLength={64} placeholder="LUXORA10" onChange={(event) => { setPromotionCode(event.target.value.toUpperCase()); setPreview(null); }} /></label>
          {promotionCode ? <Button type="button" variant="ghost" onClick={() => { setPromotionCode(""); setPreview(null); }}>Clear promotion</Button> : null}

          {error ? <p className="form-message error" role="alert">{error}</p> : null}
          <Button type="submit" disabled={submitting || placingOrder}>
            {submitting ? "Reviewing" : "Review order"}
          </Button>
        </form>

        <aside className="checkout-panel" aria-label="Order preview">
          <p className="eyebrow">Preview</p>
          <h2>Order summary</h2>
          {preview ? (
            <CheckoutSummary preview={preview} placingOrder={placingOrder} onPlaceOrder={handlePlaceOrder} />
          ) : (
            <p className="muted-copy">Enter a shipping address to calculate a server-authoritative preview.</p>
          )}
        </aside>
      </div>
    </section>
  );
}

function CheckoutSummary({ preview, placingOrder, onPlaceOrder }: { preview: CheckoutPreview; placingOrder: boolean; onPlaceOrder: () => void }) {
  return (
    <div className="checkout-summary">
      <div className="checkout-items">
        {preview.items.map((item) => (
          <article className="checkout-item" key={item.variantId}>
            <div>
              <h3>{item.name}</h3>
              <p>{item.color ?? "Standard"} {item.storage ? `/ ${item.storage}` : ""}</p>
              <p className="meta-line">SKU {item.sku} x {item.quantity}</p>
            </div>
            <Price amount={item.lineTotal} />
          </article>
        ))}
      </div>
      <div className="checkout-totals">
        <div><span>Subtotal</span><Price amount={preview.subtotal} /></div>
        <div><span>Shipping</span><Price amount={preview.shippingFee} /></div>
        <div><span>Tax</span><Price amount={preview.tax} /></div>
        {preview.promotion ? <div><span>{preview.promotion.code}</span><Price amount={preview.promotion.discountAmount} /></div> : null}
        <div><span>Discount</span><Price amount={preview.discount} /></div>
        <div className="grand-total"><span>Grand total</span><Price amount={preview.grandTotal} /></div>
      </div>
      <Button type="button" disabled={placingOrder} onClick={onPlaceOrder}>
        {placingOrder ? "Placing order" : "Place order"}
      </Button>
      <p className="muted-copy">Payment is not available yet; this creates a pending order only.</p>
      <LinkButton href="/cart" variant="ghost">Back to cart</LinkButton>
    </div>
  );
}