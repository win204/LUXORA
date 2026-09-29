import type { Metadata } from "next";
import { CartView } from "@/components/CartView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Cart",
  description: "Review selected LUXORA products"
};

export default function CartPage() {
  return (
    <main className="page">
      <Container>
        <div className="page-heading">
          <p className="eyebrow">Cart</p>
          <h1>Your selections</h1>
        </div>
        <CartView />
      </Container>
    </main>
  );
}
