"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import Link from "next/link";
import { Button, LinkButton } from "@/components/Button";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { Price } from "@/components/Price";
import { getAdminProducts } from "@/lib/adminClient";
import type { AdminProductSummary, PageResponse } from "@/lib/types";

const pageSize = 10;

export function AdminProductsView() {
  const [page, setPage] = useState(0);
  const [products, setProducts] = useState<PageResponse<AdminProductSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const nextProducts = await getAdminProducts(page, pageSize);
        if (active) setProducts(nextProducts);
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : "Unable to load products.");
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [page]);

  return (
    <AdminShell>
      <div className="admin-heading admin-heading-row">
        <div>
          <p className="eyebrow">Catalog</p>
          <h1>Products</h1>
        </div>
        <LinkButton href="/admin/products/new">New product</LinkButton>
      </div>
      {loading ? <div className="state-panel" aria-busy="true">Loading products...</div> : null}
      {error ? <ErrorState title="Products unavailable" message={error} /> : null}
      {!loading && !error && products?.content.length === 0 ? <EmptyState title="No products" message="Products will appear here once catalog data exists." /> : null}
      {products && products.content.length > 0 ? (
        <>
          <div className="admin-table" role="table" aria-label="Admin products">
            <div className="admin-table-row admin-table-head" role="row">
              <span>Product</span><span>Brand</span><span>Category</span><span>Variants</span><span>Price</span><span>Status</span>
            </div>
            {products.content.map((product) => (
              <div className="admin-table-row" role="row" key={product.id}>
                <span><Link href={`/admin/products/${product.id}`}><strong>{product.name}</strong></Link><small>{product.slug}</small></span>
                <span>{product.brandName}</span>
                <span>{product.categoryName}</span>
                <span>{product.variantCount}</span>
                <span><Price amount={product.minPrice} /></span>
                <span className={`status-badge ${product.active && product.inStock ? "status-paid" : "status-pending"}`}>{product.active ? product.inStock ? "Active" : "No stock" : "Inactive"}</span>
              </div>
            ))}
          </div>
          <Pagination page={products.page} totalPages={products.totalPages} setPage={setPage} />
        </>
      ) : null}
    </AdminShell>
  );
}

function Pagination({ page, totalPages, setPage }: { page: number; totalPages: number; setPage: (next: number) => void }) {
  return (
    <div className="pagination order-history-pagination">
      <Button type="button" variant="secondary" disabled={page <= 0} onClick={() => setPage(Math.max(0, page - 1))}>Previous</Button>
      <span>Page {page + 1} of {Math.max(totalPages, 1)}</span>
      <Button type="button" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</Button>
    </div>
  );
}