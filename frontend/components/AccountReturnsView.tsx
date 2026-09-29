"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { useAuth } from "@/components/AuthProvider";
import { getReturns, ReturnClientError } from "@/lib/returnClient";
import type { PageResponse, ReturnSummary } from "@/lib/types";

const pageSize = 6;

export function AccountReturnsView() {
  const router = useRouter();
  const { user, loading } = useAuth();
  const [page, setPage] = useState(0);
  const [returns, setReturns] = useState<PageResponse<ReturnSummary> | null>(null);
  const [returnsLoading, setReturnsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => { if (!loading && !user) router.replace("/login"); }, [loading, router, user]);

  const loadReturns = useCallback(async () => {
    setReturnsLoading(true);
    setError("");
    try {
      setReturns(await getReturns(page, pageSize));
    } catch (caught) {
      if (caught instanceof ReturnClientError && caught.status === 401) { router.replace("/login"); return; }
      setError(caught instanceof Error ? caught.message : "Unable to load returns.");
    } finally {
      setReturnsLoading(false);
    }
  }, [page, router]);

  useEffect(() => { if (!loading && user) void Promise.resolve().then(loadReturns); }, [loading, loadReturns, user]);

  if (loading || returnsLoading) return <div className="state-panel" aria-busy="true">Loading returns...</div>;
  if (!user) return <EmptyState title="Login required" message="Sign in to view your returns." />;
  if (error) return <ErrorState title="Returns unavailable" message={error} />;
  if (!returns || returns.content.length === 0) return <EmptyState title="No returns" message="Return requests for delivered orders will appear here." />;

  return (
    <section className="account-layout order-history-layout">
      <div className="account-copy"><p className="eyebrow">Returns</p><h1>Return history</h1><p>{returns.totalElements} request{returns.totalElements === 1 ? "" : "s"} saved.</p></div>
      <div className="account-stack">
        <div className="order-history-list">
          {returns.content.map((item) => (
            <article className="order-history-card" key={item.id}>
              <div className="order-history-main"><div><p className="eyebrow">Return {item.id.slice(0, 8)}</p><h2>{formatDate(item.requestedAt)}</h2><p className="muted-copy">Order {item.orderId.slice(0, 8)}</p></div><span className={`status-badge status-${item.status.toLowerCase()}`}>{item.status}</span></div>
              <div className="order-history-meta"><span>{item.totalRequestedItems} item{item.totalRequestedItems === 1 ? "" : "s"}</span><Price amount={item.estimatedRefund} /></div>
              <LinkButton href={`/account/returns/${item.id}`} variant="secondary">View return</LinkButton>
            </article>
          ))}
        </div>
        <div className="pagination order-history-pagination"><Button type="button" variant="secondary" disabled={page <= 0} onClick={() => setPage(Math.max(0, page - 1))}>Previous</Button><span>Page {returns.page + 1} of {Math.max(returns.totalPages, 1)}</span><Button type="button" variant="secondary" disabled={returns.page + 1 >= returns.totalPages} onClick={() => setPage(page + 1)}>Next</Button></div>
      </div>
    </section>
  );
}

function formatDate(value: string) { return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)); }
