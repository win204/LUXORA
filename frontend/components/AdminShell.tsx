"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { EmptyState } from "@/components/EmptyState";
import { useAuth } from "@/components/AuthProvider";

export function AdminShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { user, loading } = useAuth();
  const isAdmin = user?.roles.includes("ROLE_ADMIN") ?? false;

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, router, user]);

  if (loading) {
    return <div className="state-panel" aria-busy="true">Loading admin...</div>;
  }

  if (!user) {
    return <EmptyState title="Login required" message="Sign in with an admin account to continue." />;
  }

  if (!isAdmin) {
    return <EmptyState title="Admin access required" message="Your account does not have access to LUXORA Admin." />;
  }

  return (
    <section className="admin-layout">
      <aside className="admin-sidebar" aria-label="Admin navigation">
        <p className="eyebrow">Admin</p>
        <nav>
          <Link href="/admin">Dashboard</Link>
          <Link href="/admin/products">Products</Link>
          <Link href="/admin/orders">Orders</Link>
          <Link href="/admin/returns">Returns</Link>
          <Link href="/admin/promotions">Promotions</Link>
        </nav>
      </aside>
      <div className="admin-content">{children}</div>
    </section>
  );
}
