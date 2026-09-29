import type { Metadata } from "next";
import { AuthForm } from "@/components/AuthForm";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Register",
  description: "Create a LUXORA account"
};

export default function RegisterPage() {
  return (
    <main className="page auth-page">
      <Container>
        <div className="auth-layout">
          <section className="auth-copy">
            <p className="eyebrow">Account</p>
            <h1>Create your account</h1>
            <p>Save your session and keep your cart ready across visits while it remains active.</p>
          </section>
          <section className="auth-panel" aria-label="Register form">
            <AuthForm mode="register" />
          </section>
        </div>
      </Container>
    </main>
  );
}
