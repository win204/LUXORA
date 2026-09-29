"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { useAuth } from "@/components/AuthProvider";
import { getOrders, OrderClientError } from "@/lib/orderClient";
import type { OrderSummary, PageResponse } from "@/lib/types";

const pageSize = 6;

export function AccountOrdersView() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [page, setPage] = useState(0);
  const [orders, setOrders] = useState<PageResponse<OrderSummary> | null>(null);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, router, user]);

  const loadOrders = useCallback(async () => {
    setOrdersLoading(true);
    setError("");
    try {
      setOrders(await getOrders(page, pageSize));
    } catch (caught) {
      if (caught instanceof OrderClientError && caught.status === 401) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to load orders.");
    } finally {
      setOrdersLoading(false);
    }
  }, [page, router]);

  useEffect(() => {
    if (loading || !user) {
      return;
    }

    void Promise.resolve().then(loadOrders);
  }, [loading, loadOrders, user]);

  if (loading || ordersLoading) {
    return <div className="state-panel" aria-busy="true">Loading orders...</div>;
  }

  if (!user) {
    return <EmptyState title="Login required" message="Sign in to view your order history." />;
  }

  if (error) {
    return <ErrorState title="Orders unavailable" message={error} />;
  }

  if (!orders || orders.content.length === 0) {
    return (
      <section className="account-layout">
        <div className="account-copy">
          <p className="eyebrow">Orders</p>
          <h1>Order history</h1>
          <p>Your confirmed orders will appear here.</p>
        </div>
        <EmptyState title="No orders yet" message="When you place your first LUXORA order, it will be saved here." />
      </section>
    );
  }

  return (
    <section className="account-layout order-history-layout">
      <div className="account-copy">
        <p className="eyebrow">Orders</p>
        <h1>Order history</h1>
        <p>{orders.totalElements} order{orders.totalElements === 1 ? "" : "s"} saved for {user.firstName}.</p>
      </div>

      <div className="account-stack">
        <div className="order-history-list">
          {orders.content.map((order) => (
            <article className="order-history-card" key={order.id}>
              <div className="order-history-main">
                <div>
                  <p className="eyebrow">Order {order.id.slice(0, 8)}</p>
                  <h2>{formatDate(order.createdAt)}</h2>
                  <p className="muted-copy">{previewText(order)}</p>
                </div>
                <span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status}</span>
              </div>
              <div className="order-history-meta">
                <span>{order.totalItems} item{order.totalItems === 1 ? "" : "s"}</span>
                <Price amount={order.grandTotal} />
              </div>
              <LinkButton href={`/orders/${order.id}`} variant="secondary">View order</LinkButton>
            </article>
          ))}
        </div>

        <div className="pagination order-history-pagination" aria-label="Order history pagination">
          <Button type="button" variant="secondary" disabled={page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>
            Previous
          </Button>
          <span>Page {orders.page + 1} of {Math.max(orders.totalPages, 1)}</span>
          <Button type="button" variant="secondary" disabled={orders.page + 1 >= orders.totalPages} onClick={() => setPage((current) => current + 1)}>
            Next
          </Button>
        </div>
      </div>
    </section>
  );
}

function previewText(order: OrderSummary) {
  if (order.itemPreview.length === 0) {
    return "Order snapshot";
  }

  return order.itemPreview
    .map((item) => `${item.productName} x ${item.quantity}`)
    .join(", ");
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}