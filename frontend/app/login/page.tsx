import type { Metadata } from "next";
import { AuthForm } from "@/components/AuthForm";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Login",
  description: "Login to your LUXORA account"
};

export default function LoginPage() {
  return (
    <main className="page auth-page">
      <Container>
        <div className="auth-layout">
          <section className="auth-copy">
            <p className="eyebrow">Account</p>
            <h1>Welcome back</h1>
            <p>Sign in to keep your selected pieces connected to your account.</p>
          </section>
          <section className="auth-panel" aria-label="Login form">
            <AuthForm mode="login" />
          </section>
        </div>
      </Container>
    </main>
  );
}
