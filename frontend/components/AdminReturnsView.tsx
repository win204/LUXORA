"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Button } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { getAdminReturns } from "@/lib/adminClient";
import type { AdminReturnQuery, PageResponse, ReturnStatus, ReturnSummary } from "@/lib/types";

const pageSize = 10;
const statusViews: Array<ReturnStatus | "ALL"> = ["ALL", "REQUESTED", "APPROVED", "RECEIVED", "REFUNDED", "REJECTED", "CANCELLED"];
const emptyFilters: AdminReturnQuery = { sort: "requestedAt,desc" };

export function AdminReturnsView() {
  const [page, setPage] = useState(0);
  const [draft, setDraft] = useState<AdminReturnQuery>(emptyFilters);
  const [filters, setFilters] = useState<AdminReturnQuery>(emptyFilters);
  const [returns, setReturns] = useState<PageResponse<ReturnSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const next = await getAdminReturns({
          ...filters,
          page,
          size: pageSize,
          dateFrom: filters.dateFrom ? `${filters.dateFrom}T00:00:00.000Z` : undefined,
          dateTo: filters.dateTo ? `${filters.dateTo}T23:59:59.999Z` : undefined
        });
        if (active) setReturns(next);
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : "Unable to load returns.");
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

  function chooseStatus(status: ReturnStatus | "ALL") {
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
      <div className="admin-heading"><p className="eyebrow">Returns</p><h1>Return queue</h1></div>
      <div className="admin-status-tabs" role="tablist" aria-label="Return status views">
        {statusViews.map((status) => <button className={draft.status === status || (!draft.status && status === "ALL") ? "active" : ""} key={status} onClick={() => chooseStatus(status)} role="tab" type="button">{status === "ALL" ? "All" : status}</button>)}
      </div>
      <form className="admin-return-filters" onSubmit={applyFilters}>
        <label className="field-label">Customer email<input value={draft.customerEmail ?? ""} onChange={(event) => setDraft({ ...draft, customerEmail: event.target.value })} placeholder="customer@example.com" /></label>
        <label className="field-label">Order ID<input value={draft.orderId ?? ""} onChange={(event) => setDraft({ ...draft, orderId: event.target.value })} placeholder="Order UUID" /></label>
        <label className="field-label">Tracking number<input value={draft.trackingNumber ?? ""} onChange={(event) => setDraft({ ...draft, trackingNumber: event.target.value })} placeholder="LXR-RMA" /></label>
        <label className="field-label">From<input type="date" value={draft.dateFrom ?? ""} onChange={(event) => setDraft({ ...draft, dateFrom: event.target.value || undefined })} /></label>
        <label className="field-label">To<input type="date" value={draft.dateTo ?? ""} onChange={(event) => setDraft({ ...draft, dateTo: event.target.value || undefined })} /></label>
        <label className="field-label">Sort<select value={draft.sort ?? "requestedAt,desc"} onChange={(event) => setDraft({ ...draft, sort: event.target.value as AdminReturnQuery["sort"] })}><option value="requestedAt,desc">Newest requested</option><option value="requestedAt,asc">Oldest requested</option><option value="updatedAt,desc">Recently updated</option><option value="updatedAt,asc">Least recently updated</option></select></label>
        <div className="admin-filter-actions"><Button type="submit">Apply</Button><Button type="button" variant="secondary" onClick={clearFilters}>Clear</Button></div>
      </form>
      {loading ? <div className="state-panel" aria-busy="true">Loading returns...</div> : null}
      {error ? <ErrorState title="Returns unavailable" message={error} /> : null}
      {!loading && !error && returns?.content.length === 0 ? <EmptyState title="No matching returns" message="Try another status or clear the current filters." /> : null}
      {returns && returns.content.length > 0 ? <>
        <div className="results-bar"><p>{returns.totalElements} return{returns.totalElements === 1 ? "" : "s"}</p></div>
        <div className="admin-table admin-returns-table" role="table" aria-label="Admin returns">
          <div className="admin-table-row admin-table-head" role="row"><span>Return</span><span>Customer</span><span>Status</span><span>Tracking</span><span>Refund</span><span>Requested</span></div>
          {returns.content.map((item) => <div className="admin-table-row" role="row" key={item.id}><span><Link href={`/admin/returns/${item.id}`}><strong>{item.id.slice(0, 8)}</strong></Link><small>{item.orderId.slice(0, 8)}</small></span><span>{item.userEmail}</span><span className={`status-badge status-${item.status.toLowerCase()}`}>{item.status}</span><span>{item.trackingNumber ?? "-"}</span><span><Price amount={item.estimatedRefund} /></span><span>{formatDate(item.requestedAt)}</span></div>)}
        </div>
        <div className="pagination order-history-pagination"><Button type="button" variant="secondary" disabled={page <= 0} onClick={() => setPage(Math.max(0, page - 1))}>Previous</Button><span>Page {returns.page + 1} of {Math.max(returns.totalPages, 1)}</span><Button type="button" variant="secondary" disabled={returns.page + 1 >= returns.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div>
      </> : null}
    </AdminShell>
  );
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }