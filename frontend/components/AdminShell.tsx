"use client";

import Link from "next/link";
import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { EmptyState } from "@/components/EmptyState";
import { useAuth } from "@/components/AuthProvider";

const ADMIN_LINKS = [
  { href: "/admin", label: "Dashboard" },
  { href: "/admin/products", label: "Products" },
  { href: "/admin/orders", label: "Orders" },
  { href: "/admin/returns", label: "Returns" },
  { href: "/admin/promotions", label: "Promotions" },
  { href: "/admin/flash-sales", label: "Flash Sales" },
  { href: "/admin/banners", label: "Banners" },
  { href: "/admin/reviews", label: "Reviews" },
];

export function AdminShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
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
        <p className="eyebrow">Admin Console</p>
        <nav>
          {ADMIN_LINKS.map((link) => {
            const isActive =
              link.href === "/admin"
                ? pathname === "/admin"
                : pathname === link.href || pathname.startsWith(`${link.href}/`);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={isActive ? "is-active" : undefined}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>
      </aside>
      <div className="admin-content">{children}</div>
    </section>
  );
}
