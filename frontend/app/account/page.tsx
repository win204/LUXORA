import type { Metadata } from "next";
import { AccountDashboard } from "@/components/AccountDashboard";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Account | LUXORA",
  description: "View your LUXORA account and order navigation."
};

export default function AccountPage() {
  return (
    <main className="page">
      <Container>
        <AccountDashboard />
      </Container>
    </main>
  );
}