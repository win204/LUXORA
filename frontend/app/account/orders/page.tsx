import type { Metadata } from "next";
import { AccountOrdersView } from "@/components/AccountOrdersView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Orders | LUXORA",
  description: "View your LUXORA order history."
};

export default function AccountOrdersPage() {
  return (
    <main className="page">
      <Container>
        <AccountOrdersView />
      </Container>
    </main>
  );
}