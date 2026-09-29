"use client";

import Link from "next/link";
import { useAuth } from "./AuthProvider";

export function AuthNav() {
  const { user, loading, logout, sessionExpired } = useAuth();

  if (loading) {
    return <span className="nav-muted">Account</span>;
  }

  if (!user) {
    return (
      <span className="auth-links" aria-live="polite">
        {sessionExpired ? <span className="nav-muted">Session expired</span> : null}
        <Link href="/login">Login</Link>
        <Link href="/register">Register</Link>
      </span>
    );
  }

  return (
    <span className="auth-user">
      {user.roles.includes("ROLE_ADMIN") ? <Link href="/admin">Admin</Link> : null}
      <Link href="/account" className="auth-user-link">
        {user.firstName}
      </Link>
      <Link href="/account/orders">Orders</Link>
      <button type="button" onClick={() => void logout()}>
        Logout
      </button>
    </span>
  );
}