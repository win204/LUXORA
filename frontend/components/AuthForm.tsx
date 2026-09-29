"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/Button";
import { getStoredCartId } from "@/lib/cartClient";
import { login, register } from "@/lib/authClient";

type AuthFormProps = {
  mode: "login" | "register";
};

export function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const { setAuthenticated } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const searchParams = useSearchParams();
  const notice = searchParams.get("notice");

  const isRegister = mode === "register";

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = isRegister
        ? await register({ email, password, firstName, lastName })
        : await login({ email, password }, getStoredCartId());
      setAuthenticated(response);
      router.push("/cart");
      router.refresh();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Unable to continue.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      {isRegister ? (
        <div className="auth-name-grid">
          <label>
            <span>First name</span>
            <input
              autoComplete="given-name"
              maxLength={120}
              required
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
            />
          </label>
          <label>
            <span>Last name</span>
            <input
              autoComplete="family-name"
              maxLength={120}
              required
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
            />
          </label>
        </div>
      ) : null}

      <label>
        <span>Email</span>
        <input
          autoComplete="email"
          maxLength={254}
          required
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
      </label>

      <label>
        <span>Password</span>
        <input
          autoComplete={isRegister ? "new-password" : "current-password"}
          minLength={8}
          maxLength={100}
          required
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </label>

      {notice === "password-changed" && !isRegister ? (
        <p className="form-message success">Password changed successfully. Please sign in again.</p>
      ) : null}

      {error ? <p className="form-message error">{error}</p> : null}

      <Button className="auth-submit" type="submit" disabled={loading}>
        {loading ? "Working..." : isRegister ? "Create account" : "Login"}
      </Button>

      <p className="auth-switch">
        {isRegister ? "Already have an account?" : "New to LUXORA?"}{" "}
        <Link href={isRegister ? "/login" : "/register"}>
          {isRegister ? "Login" : "Create one"}
        </Link>
      </p>
    </form>
  );
}

