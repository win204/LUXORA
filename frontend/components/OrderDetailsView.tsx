"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { ReturnRequestPanel } from "@/components/ReturnRequestPanel";
import { useAuth } from "@/components/AuthProvider";
import { cancelOrder, createMockPayment, createVnPayPayment, getLatestPayment, getOrder, getStoredGuestToken, OrderClientError } from "@/lib/orderClient";
import type { MockPaymentOutcome, Order, Payment } from "@/lib/types";

type PayingState = MockPaymentOutcome | "VNPAY" | null;

export function OrderDetailsView({ orderId }: { orderId: string }) {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);
  const [payment, setPayment] = useState<Payment | null>(null);
  const [orderLoading, setOrderLoading] = useState(true);
  const [paying, setPaying] = useState<PayingState>(null);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState("");
  const [paymentMessage, setPaymentMessage] = useState("");

  const hasGuestToken = Boolean(
    getStoredGuestToken(orderId) ||
    (typeof window !== "undefined" && new URLSearchParams(window.location.search).get("token"))
  );

  useEffect(() => {
    if (!loading && !user && !hasGuestToken) {
      router.replace("/login");
    }
  }, [hasGuestToken, loading, router, user]);

  const loadOrder = useCallback(async () => {
    setOrderLoading(true);
    setError("");
    try {
      const searchParams = typeof window !== "undefined" ? new URLSearchParams(window.location.search) : null;
      const token = searchParams?.get("token") ?? undefined;
      const vnpResponseCode = searchParams?.get("vnp_ResponseCode");
      if (vnpResponseCode) {
        if (vnpResponseCode === "00") {
          setPaymentMessage("Thanh toán VNPay thành công! Đơn hàng đã được xác nhận thanh toán.");
        } else {
          setError(`Thanh toán VNPay không thành công hoặc đã bị hủy (Mã lỗi: ${vnpResponseCode}).`);
        }
      }
      const loaded = await getOrder(orderId, token);
      setOrder(loaded);
      try {
        setPayment(await getLatestPayment(orderId, token));
      } catch (caught) {
        if (caught instanceof OrderClientError && caught.status === 404) {
          setPayment(null);
        } else {
          throw caught;
        }
      }
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401 && user) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to load order.");
    } finally {
      setOrderLoading(false);
    }
  }, [orderId, router, user]);

  useEffect(() => {
    if (loading) {
      return;
    }
    if (!user && !hasGuestToken) {
      return;
    }

    void Promise.resolve().then(loadOrder);
  }, [hasGuestToken, loading, loadOrder, user]);

  async function handleCancel() {
    if (!order) return;
    const confirmed = window.confirm(`Cancel order ${order.id.slice(0, 8)}? Inventory will be restored.`);
    if (!confirmed) return;
    setCancelling(true);
    setError("");
    setPaymentMessage("");
    try {
      const token = (typeof window !== "undefined" ? new URLSearchParams(window.location.search).get("token") : null) ?? undefined;
      setOrder(await cancelOrder(order.id, token));
      setPaymentMessage("Order cancelled.");
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401 && user) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to cancel order.");
    } finally {
      setCancelling(false);
    }
  }

  async function handleMockPayment(mockOutcome: MockPaymentOutcome) {
    setPaying(mockOutcome);
    setError("");
    setPaymentMessage("");
    try {
      const token = (typeof window !== "undefined" ? new URLSearchParams(window.location.search).get("token") : null) ?? undefined;
      const nextPayment = await createMockPayment(orderId, mockOutcome, token);
      setPayment(nextPayment);
      const updatedOrder = await getOrder(orderId, token);
      setOrder(updatedOrder);
      setPaymentMessage(nextPayment.status === "SUCCEEDED" ? "Payment succeeded." : "Payment failed. You can try again.");
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401 && user) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to process payment.");
    } finally {
      setPaying(null);
    }
  }

  async function handleVnPayPayment() {
    setPaying("VNPAY");
    setError("");
    setPaymentMessage("");
    try {
      const token = (typeof window !== "undefined" ? new URLSearchParams(window.location.search).get("token") : null) ?? undefined;
      const nextPayment = await createVnPayPayment(orderId, undefined, token);
      setPayment(nextPayment);
      if (nextPayment.paymentUrl) {
        setPaymentMessage("Đang chuyển hướng sang cổng thanh toán VNPay Sandbox...");
        window.location.href = nextPayment.paymentUrl;
        return;
      }
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401 && user) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Không thể khởi tạo thanh toán VNPay.");
      setPaying(null);
    }
  }

  if (loading || orderLoading) {
    return <div className="state-panel">Loading order...</div>;
  }

  if (!user && !hasGuestToken) {
    return <EmptyState title="Login required" message="Sign in or use your order access link to view this order." />;
  }

  if (error && !order) {
    return <ErrorState message={error} />;
  }

  if (!order) {
    return <EmptyState title="Order not found" message="We could not find that order." />;
  }

  return (
    <section className="order-detail-layout">
      <div className="checkout-copy">
        <p className="eyebrow">Order confirmed</p>
        <h1>Order {order.id.slice(0, 8)}</h1>
        <p>Status: <strong>{order.status}</strong></p>
        {order.customerEmail ? <p className="meta-line">Customer: {order.customerEmail}</p> : null}
        <p>Created {new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(order.createdAt))}</p>
      </div>

      <div className="checkout-stack">
        {error ? <p className="form-message error" role="alert">{error}</p> : null}
        <section className="checkout-panel checkout-summary" aria-label="Order items">
          <p className="eyebrow">Items</p>
          {order.items.map((item) => (
            <article className="checkout-item" key={item.id}>
              <div>
                <h3>{item.productName}</h3>
                <p>{item.variantName ?? item.sku}</p>
                <p className="meta-line">SKU {item.sku} x {item.quantity}</p>
              </div>
              <Price amount={item.lineTotal} />
            </article>
          ))}
        </section>

        {order.status === "PENDING" ? (
          <section className="checkout-panel checkout-summary" aria-label="Cancellation">
            <p className="eyebrow">Cancellation</p>
            <h2>Need to cancel?</h2>
            <p className="muted-copy">Pending orders can be cancelled before payment. Reserved inventory will be restored.</p>
            <Button type="button" variant="secondary" disabled={cancelling || paying !== null} onClick={handleCancel}>
              {cancelling ? "Cancelling" : "Cancel Order"}
            </Button>
          </section>
        ) : null}

        <PaymentSection
          order={order}
          payment={payment}
          paying={paying}
          paymentMessage={paymentMessage}
          onPayMock={handleMockPayment}
          onPayVnPay={handleVnPayPayment}
        />

        {user ? <ReturnRequestPanel order={order} /> : null}
        {order.shipment ? (
          <section className="checkout-panel" aria-label="Shipment details" style={{ marginTop: "1rem" }}>
            <p className="eyebrow">Vận chuyển</p>
            <h2>Thông tin kiện hàng</h2>
            <div style={{ marginTop: "0.75rem", display: "grid", gap: "0.5rem" }}>
              <div><strong>Đơn vị vận chuyển:</strong> {order.shipment.carrier}</div>
              <div>
                <strong>Mã vận đơn:</strong>{" "}
                {order.shipment.trackingUrl ? (
                  <a
                    href={order.shipment.trackingUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ color: "var(--accent, #6366f1)", textDecoration: "underline", fontWeight: 600 }}
                  >
                    {order.shipment.trackingNumber} ↗ (Tra cứu hành trình GHN)
                  </a>
                ) : (
                  <span>{order.shipment.trackingNumber}</span>
                )}
              </div>
              <div>
                <strong>Ngày gửi:</strong> {new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(order.shipment.shippedAt))}
              </div>
              {order.shipment.deliveredAt ? (
                <div>
                  <strong>Đã giao lúc:</strong> {new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(order.shipment.deliveredAt))}
                </div>
              ) : null}
            </div>
          </section>
        ) : null}


        <aside className="checkout-panel checkout-summary" aria-label="Order totals">
          <p className="eyebrow">Summary</p>
          <h2>Total</h2>
          <div className="checkout-totals">
            <div><span>Subtotal</span><Price amount={order.subtotal} /></div>
            <div><span>Shipping</span><Price amount={order.shippingFee} /></div>
            <div><span>Tax</span><Price amount={order.tax} /></div>
            <div><span>Discount</span><Price amount={order.discount} /></div>
            <div className="grand-total"><span>Grand total</span><Price amount={order.grandTotal} /></div>
          </div>
          <div className="shipping-snapshot">
            <h3>Shipping</h3>
            <p>{order.shippingAddress.recipientName}</p>
            <p>{order.shippingAddress.phone}</p>
            <p>{order.shippingAddress.addressLine1}</p>
            {order.shippingAddress.addressLine2 ? <p>{order.shippingAddress.addressLine2}</p> : null}
            <p>{order.shippingAddress.city}, {order.shippingAddress.province}</p>
            <p>{order.shippingAddress.country} {order.shippingAddress.postalCode ?? ""}</p>
          </div>
        </aside>
      </div>
    </section>
  );
}

function PaymentSection({
  order,
  payment,
  paying,
  paymentMessage,
  onPayMock,
  onPayVnPay
}: {
  order: Order;
  payment: Payment | null;
  paying: PayingState;
  paymentMessage: string;
  onPayMock: (mockOutcome: MockPaymentOutcome) => void;
  onPayVnPay: () => void;
}) {
  const paymentLocked = order.status !== "PENDING" || paying !== null;

  return (
    <section className="checkout-panel checkout-summary" aria-label="Payment">
      <p className="eyebrow">Payment</p>
      <h2>{order.status === "PAID" ? "Paid" : "Payment method"}</h2>
      {payment ? (
        <div className="payment-status">
          <div><span>Latest attempt</span><strong>{payment.status}</strong></div>
          <div><span>Provider</span><strong>{payment.provider}</strong></div>
          <div><span>Amount</span><Price amount={payment.amount} /></div>
        </div>
      ) : (
        <p className="muted-copy">No payment attempts yet.</p>
      )}
      {paymentMessage ? <p className={payment?.status === "FAILED" ? "form-message error" : "form-message success"}>{paymentMessage}</p> : null}
      {order.status === "PENDING" ? (
        <div className="payment-actions">
          <Button type="button" disabled={paymentLocked} onClick={onPayVnPay}>
            {paying === "VNPAY" ? "Chuyển sang VNPay..." : "Thanh toán VNPay Sandbox"}
          </Button>
          <Button type="button" variant="secondary" disabled={paymentLocked} onClick={() => onPayMock("SUCCEEDED")}>
            {paying === "SUCCEEDED" ? "Processing" : "Mock Provider (Success)"}
          </Button>
          <Button type="button" variant="secondary" disabled={paymentLocked} onClick={() => onPayMock("FAILED")}>
            {paying === "FAILED" ? "Processing" : "Mock Provider (Fail)"}
          </Button>
        </div>
      ) : (
        <p className="muted-copy">This order no longer accepts payment attempts.</p>
      )}
    </section>
  );
}
