import type { Metadata } from "next";
import { AdminProductsView } from "@/components/AdminProductsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Products | LUXORA",
  description: "Review LUXORA catalog products."
};

export default function AdminProductsPage() {
  return (
    <main className="page">
      <Container>
        <AdminProductsView />
      </Container>
    </main>
  );
}