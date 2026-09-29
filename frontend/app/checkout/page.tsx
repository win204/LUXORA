import type { Metadata } from "next";
import { CheckoutView } from "@/components/CheckoutView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Checkout | LUXORA",
  description: "Review your LUXORA order preview."
};

export default function CheckoutPage() {
  return (
    <main className="page">
      <Container>
        <CheckoutView />
      </Container>
    </main>
  );
}
