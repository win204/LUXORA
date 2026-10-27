"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { Price } from "@/components/Price";
import { useAuth } from "@/components/AuthProvider";
import { CheckoutClientError, previewCheckout } from "@/lib/checkoutClient";
import { createOrder, OrderClientError } from "@/lib/orderClient";
import { fetchMyLoyaltyAccount } from "@/lib/loyaltyClient";
import { fetchAvailablePromotions } from "@/lib/promotionClient";
import type { CheckoutAddress, CheckoutPreview, LoyaltyAccount, AvailablePromotion } from "@/lib/types";

const initialAddress: CheckoutAddress = {
  email: "",
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
  const { user, accessToken, loading } = useAuth();
  const [address, setAddress] = useState<CheckoutAddress>(initialAddress);
  const [preview, setPreview] = useState<CheckoutPreview | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [error, setError] = useState("");
  const [promotionCode, setPromotionCode] = useState("");
  const [autoApplyBestVoucher, setAutoApplyBestVoucher] = useState(false);
  const [availablePromotions, setAvailablePromotions] = useState<AvailablePromotion[]>([]);
  const [loyaltyAccount, setLoyaltyAccount] = useState<LoyaltyAccount | null>(null);
  const [useLoyalty, setUseLoyalty] = useState(false);
  const [redeemPoints, setRedeemPoints] = useState(0);

  useEffect(() => {
    fetchAvailablePromotions(0)
      .then((promos) => setAvailablePromotions(promos))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!user || !accessToken) return;
    fetchMyLoyaltyAccount(accessToken)
      .then((acc) => setLoyaltyAccount(acc))
      .catch(() => setLoyaltyAccount(null));
  }, [user, accessToken]);

  if (loading) {
    return <div className="state-panel">Preparing checkout...</div>;
  }

  if (!user) {
    return (
      <section className="checkout-layout">
        <div className="state-panel" style={{ maxWidth: "620px", margin: "40px auto", textAlign: "center", padding: "52px 36px" }}>
          <p className="eyebrow" style={{ color: "var(--gold)" }}>Đặc quyền hội viên LUXORA Maison</p>
          <h1 style={{ fontFamily: "var(--font-display), serif", fontSize: "2.4rem", margin: "14px 0 16px", color: "var(--foreground)" }}>
            Bắt buộc đăng nhập để mua hàng
          </h1>
          <p style={{ color: "var(--muted)", lineHeight: 1.75, marginBottom: "32px", fontSize: "0.96rem" }}>
            Để bảo lưu quyền lợi bảo hành chính hãng 24 tháng, tích lũy điểm thưởng VIP và đảm bảo tính bảo mật cho giao dịch, quý khách vui lòng đăng nhập tài khoản trước khi tiến hành thanh toán.
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "14px", alignItems: "center" }}>
            <LinkButton href="/login?redirect=/checkout" variant="primary" style={{ width: "100%", maxWidth: "340px", textAlign: "center" }}>
              Đăng nhập tài khoản
            </LinkButton>
            <LinkButton href="/register?redirect=/checkout" variant="secondary" style={{ width: "100%", maxWidth: "340px", textAlign: "center" }}>
              Đăng ký thành viên mới
            </LinkButton>
          </div>
        </div>
      </section>
    );
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!user) {
      setError("Quý khách vui lòng đăng nhập để tiến hành đặt hàng.");
      return;
    }
    setSubmitting(true);
    setError("");
    setPreview(null);

    try {
      const nextPreview = await previewCheckout(
        normalizedAddress(),
        autoApplyBestVoucher ? undefined : promotionCode,
        useLoyalty ? redeemPoints : 0,
        autoApplyBestVoucher
      );
      setPreview(nextPreview);
      if (nextPreview.availablePromotions && nextPreview.availablePromotions.length > 0) {
        setAvailablePromotions(nextPreview.availablePromotions);
      }
      if (autoApplyBestVoucher && nextPreview.promotion) {
        setPromotionCode(nextPreview.promotion.code);
      }
    } catch (caught) {
      if (caught instanceof CheckoutClientError && caught.status === 401 && user) {
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
      const order = await createOrder({
        ...normalizedAddress(),
        promotionCode: preview?.promotion?.code ?? (promotionCode.trim() || undefined),
        redeemPoints: useLoyalty && redeemPoints > 0 ? redeemPoints : undefined
      });
      router.push(`/orders/${order.id}`);
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401 && user) {
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
      email: user ? (user.email ?? address.email?.trim()) : address.email?.trim(),
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
        <p>Preview confirms your current cart, stock, and prices from LUXORA before an order is created.</p>
      </div>

      <div className="checkout-stack">
        <form className="checkout-panel checkout-form" onSubmit={handleSubmit}>
          <div>
            <p className="eyebrow">Shipping & Customer</p>
            <h2>Delivery details</h2>
          </div>

          <p className="meta-line">Đặt hàng với tài khoản thành viên: <strong>{user.email}</strong></p>

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

          <div className="vouchers-checkout-box">
            <div className="vouchers-header">
              <div className="vouchers-title-group">
                <h3>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                  </svg>
                  Ưu đãi & Voucher LUXORA
                </h3>
                <p>Mã giảm giá độc quyền dành riêng cho bạn</p>
              </div>
              <label className="auto-apply-toggle">
                <input
                  type="checkbox"
                  checked={autoApplyBestVoucher}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setAutoApplyBestVoucher(checked);
                    if (checked) {
                      setPromotionCode("");
                    }
                    setPreview(null);
                  }}
                />
                <span>Tự động chọn ưu đãi tốt nhất</span>
              </label>
            </div>

            {!autoApplyBestVoucher ? (
              <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                <input
                  value={promotionCode}
                  maxLength={64}
                  placeholder="Nhập mã voucher (vd: LUXORA10, WELCOME50)"
                  onChange={(event) => {
                    setPromotionCode(event.target.value.toUpperCase());
                    setPreview(null);
                  }}
                  style={{ flex: 1, padding: "8px 12px", border: "1px solid var(--line)", borderRadius: "6px" }}
                />
                {promotionCode ? (
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => {
                      setPromotionCode("");
                      setPreview(null);
                    }}
                  >
                    Xóa
                  </Button>
                ) : null}
              </div>
            ) : (
              <div style={{ fontSize: "0.82rem", color: "#166534", background: "#f0fdf4", padding: "8px 12px", borderRadius: "6px" }}>
                ✨ Hệ thống LUXORA sẽ tự động tính toán và áp dụng voucher có mức giảm giá cao nhất khi bạn duyệt đơn.
              </div>
            )}

            {availablePromotions.length > 0 ? (
              <div className="voucher-cards-list">
                {availablePromotions.map((promo) => {
                  const isSelected = promotionCode.toUpperCase() === promo.code.toUpperCase();
                  return (
                    <div
                      key={promo.code}
                      className={`voucher-item-card ${promo.isBestDeal ? "is-best-deal" : ""} ${isSelected ? "is-selected" : ""} ${!promo.eligible ? "is-ineligible" : ""}`}
                    >
                      <div className="voucher-card-left">
                        <div className="voucher-card-badges">
                          <span className="voucher-code-badge">{promo.code}</span>
                          {promo.isBestDeal ? <span className="voucher-badge-best">TỐT NHẤT ★</span> : null}
                        </div>
                        <div className="voucher-card-desc">
                          {promo.description || (promo.type === "PERCENTAGE" ? `Giảm ${promo.value}% giá trị đơn` : `Giảm $${promo.value} cho đơn hàng`)}
                        </div>
                        <div className={`voucher-card-sub ${!promo.eligible && promo.amountNeeded && promo.amountNeeded > 0 ? "need-more" : ""}`}>
                          {promo.minimumOrderAmount && promo.minimumOrderAmount > 0
                            ? promo.eligible
                              ? `Đơn tối thiểu $${promo.minimumOrderAmount}`
                              : `Cần mua thêm $${promo.amountNeeded?.toFixed(2)} để dùng (Tối thiểu $${promo.minimumOrderAmount})`
                            : "Áp dụng cho mọi giá trị đơn"}
                        </div>
                      </div>

                      <div className="voucher-card-right">
                        {promo.potentialDiscount && promo.potentialDiscount > 0 ? (
                          <span className="voucher-savings-est">Giảm -${promo.potentialDiscount.toFixed(2)}</span>
                        ) : null}
                        {isSelected ? (
                          <span className="voucher-applied-pill">✓ Đã áp dụng</span>
                        ) : (
                          <button
                            type="button"
                            className="voucher-apply-btn"
                            disabled={!promo.eligible}
                            onClick={() => {
                              setAutoApplyBestVoucher(false);
                              setPromotionCode(promo.code);
                              setPreview(null);
                            }}
                          >
                            Áp dụng
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : null}
          </div>

          {user && loyaltyAccount && loyaltyAccount.currentPoints > 0 ? (
            <div className="loyalty-checkout-box">
              <label className="loyalty-toggle-label">
                <input
                  type="checkbox"
                  checked={useLoyalty}
                  onChange={(e) => {
                    const next = e.target.checked;
                    setUseLoyalty(next);
                    if (next && redeemPoints === 0) {
                      setRedeemPoints(Math.min(loyaltyAccount.currentPoints, 500));
                    }
                    setPreview(null);
                  }}
                />
                <span>
                  <strong>Redeem LUXORA Points</strong> ({loyaltyAccount.currentPoints.toLocaleString()} PTS available = ${loyaltyAccount.dollarValue.toFixed(2)})
                </span>
              </label>

              {useLoyalty ? (
                <div className="loyalty-inputs-container">
                  <div className="loyalty-number-row">
                    <label>
                      <span>Points to redeem:</span>
                      <input
                        type="number"
                        min={0}
                        max={loyaltyAccount.currentPoints}
                        step={50}
                        value={redeemPoints}
                        onChange={(e) => {
                          const val = Math.max(0, Math.min(loyaltyAccount.currentPoints, parseInt(e.target.value || "0", 10)));
                          setRedeemPoints(val);
                          setPreview(null);
                        }}
                      />
                    </label>
                    <span className="points-conversion-hint">≈ ${(redeemPoints / 100).toFixed(2)} USD discount</span>
                  </div>
                  <div className="loyalty-quick-chips">
                    <button
                      type="button"
                      className="loyalty-chip"
                      onClick={() => {
                        setRedeemPoints(Math.min(loyaltyAccount.currentPoints, 100));
                        setPreview(null);
                      }}
                    >
                      100 PTS ($1)
                    </button>
                    {loyaltyAccount.currentPoints >= 500 ? (
                      <button
                        type="button"
                        className="loyalty-chip"
                        onClick={() => {
                          setRedeemPoints(Math.min(loyaltyAccount.currentPoints, 500));
                          setPreview(null);
                        }}
                      >
                        500 PTS ($5)
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="loyalty-chip"
                      onClick={() => {
                        setRedeemPoints(loyaltyAccount.currentPoints);
                        setPreview(null);
                      }}
                    >
                      Max Available
                    </button>
                  </div>
                  <small className="loyalty-sub-hint">100 points = $1.00 USD. Capped at 50% of eligible order subtotal.</small>
                </div>
              ) : null}
            </div>
          ) : null}

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
        {preview.promotion && preview.promotion.discountAmount !== "0.00" ? (
          <div style={{ display: "flex", justifyContent: "space-between", color: "#166534", fontWeight: 600 }}>
            <span>Voucher ({preview.promotion.code})</span>
            <Price amount={`-${preview.promotion.discountAmount}`} />
          </div>
        ) : null}
        {preview.loyaltyPointsRedeemed && preview.loyaltyPointsRedeemed > 0 ? (
          <div className="loyalty-discount-row">
            <span>LUXORA Rewards ({preview.loyaltyPointsRedeemed} pts)</span>
            <Price amount={`-${preview.loyaltyDiscount ?? "0.00"}`} />
          </div>
        ) : null}
        <div className="grand-total"><span>Grand total</span><Price amount={preview.grandTotal} /></div>
      </div>
      <Button type="button" disabled={placingOrder} onClick={onPlaceOrder}>
        {placingOrder ? "Placing order" : "Place order"}
      </Button>
      <p className="muted-copy">This creates a pending order; payment is handled in the next step.</p>
      <LinkButton href="/cart" variant="ghost">Back to cart</LinkButton>
    </div>
  );
}