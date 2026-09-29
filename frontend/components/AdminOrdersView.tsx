"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Button } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { getAdminOrders } from "@/lib/adminClient";
import type { AdminOrderQuery, AdminOrderSummary, OrderStatus, PageResponse } from "@/lib/types";

const pageSize = 10;
const statusViews: Array<OrderStatus | "ALL"> = ["ALL", "PENDING", "PAID", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"];
const emptyFilters: AdminOrderQuery = { sort: "createdAt,desc" };

export function AdminOrdersView() {
  const [page, setPage] = useState(0);
  const [draft, setDraft] = useState<AdminOrderQuery>(emptyFilters);
  const [filters, setFilters] = useState<AdminOrderQuery>(emptyFilters);
  const [orders, setOrders] = useState<PageResponse<AdminOrderSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const nextOrders = await getAdminOrders({
          ...filters,
          page,
          size: pageSize,
          dateFrom: filters.dateFrom ? `${filters.dateFrom}T00:00:00.000Z` : undefined,
          dateTo: filters.dateTo ? `${filters.dateTo}T23:59:59.999Z` : undefined
        });
        if (active) setOrders(nextOrders);
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : "Unable to load orders.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [filters, page]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setFilters({
      ...draft,
      orderId: draft.orderId?.trim() || undefined,
      customerEmail: draft.customerEmail?.trim() || undefined,
      trackingNumber: draft.trackingNumber?.trim() || undefined
    });
  }

  function chooseStatus(status: OrderStatus | "ALL") {
    const next = { ...draft, status: status === "ALL" ? undefined : status };
    setDraft(next);
    setFilters(next);
    setPage(0);
  }

  function clearFilters() {
    setDraft(emptyFilters);
    setFilters(emptyFilters);
    setPage(0);
  }

  return (
    <AdminShell>
      <div className="admin-heading"><p className="eyebrow">Orders</p><h1>Order queue</h1></div>
      <div className="admin-status-tabs" role="tablist" aria-label="Order status views">
        {statusViews.map((status) => <button className={draft.status === status || (!draft.status && status === "ALL") ? "active" : ""} key={status} onClick={() => chooseStatus(status)} role="tab" type="button">{status === "ALL" ? "All" : status}</button>)}
      </div>
      <form className="admin-return-filters" onSubmit={applyFilters}>
        <label className="field-label">Customer email<input value={draft.customerEmail ?? ""} onChange={(event) => setDraft({ ...draft, customerEmail: event.target.value })} placeholder="customer@example.com" /></label>
        <label className="field-label">Order ID<input value={draft.orderId ?? ""} onChange={(event) => setDraft({ ...draft, orderId: event.target.value })} placeholder="Order UUID" /></label>
        <label className="field-label">Tracking number<input value={draft.trackingNumber ?? ""} onChange={(event) => setDraft({ ...draft, trackingNumber: event.target.value })} placeholder="Carrier tracking" /></label>
        <label className="field-label">From<input type="date" value={draft.dateFrom ?? ""} onChange={(event) => setDraft({ ...draft, dateFrom: event.target.value || undefined })} /></label>
        <label className="field-label">To<input type="date" value={draft.dateTo ?? ""} onChange={(event) => setDraft({ ...draft, dateTo: event.target.value || undefined })} /></label>
        <label className="field-label">Sort<select value={draft.sort ?? "createdAt,desc"} onChange={(event) => setDraft({ ...draft, sort: event.target.value as AdminOrderQuery["sort"] })}><option value="createdAt,desc">Newest orders</option><option value="createdAt,asc">Oldest orders</option><option value="updatedAt,desc">Recently updated</option><option value="updatedAt,asc">Least recently updated</option></select></label>
        <div className="admin-filter-actions"><Button type="submit">Apply</Button><Button type="button" variant="secondary" onClick={clearFilters}>Clear</Button></div>
      </form>
      {loading ? <div className="state-panel" aria-busy="true">Loading orders...</div> : null}
      {error ? <ErrorState title="Orders unavailable" message={error} /> : null}
      {!loading && !error && orders?.content.length === 0 ? <EmptyState title="No matching orders" message="Try another status or clear the current filters." /> : null}
      {orders && orders.content.length > 0 ? <>
        <div className="results-bar"><p>{orders.totalElements} order{orders.totalElements === 1 ? "" : "s"}</p></div>
        <div className="admin-table admin-orders-queue-table" role="table" aria-label="Admin orders">
          <div className="admin-table-row admin-table-head" role="row"><span>Order</span><span>Customer</span><span>Status</span><span>Tracking</span><span>Items</span><span>Total</span><span>Updated</span></div>
          {orders.content.map((order) => <div className="admin-table-row" role="row" key={order.id}><span><Link href={`/admin/orders/${order.id}`}><strong>{order.id.slice(0, 8)}</strong></Link><small>{order.id}</small></span><span>{order.userEmail}</span><span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status}</span><span>{order.trackingNumber ?? "-"}</span><span>{order.totalItems}</span><span><Price amount={order.grandTotal} /></span><span>{formatDate(order.updatedAt)}</span></div>)}
        </div>
        <div className="pagination order-history-pagination"><Button type="button" variant="secondary" disabled={page <= 0} onClick={() => setPage(Math.max(0, page - 1))}>Previous</Button><span>Page {orders.page + 1} of {Math.max(orders.totalPages, 1)}</span><Button type="button" variant="secondary" disabled={orders.page + 1 >= orders.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div>
      </> : null}
    </AdminShell>
  );
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }