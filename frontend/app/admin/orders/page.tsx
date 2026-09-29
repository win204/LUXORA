import type { Metadata } from "next";
import { AdminOrdersView } from "@/components/AdminOrdersView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Orders | LUXORA",
  description: "Review LUXORA customer orders."
};

export default function AdminOrdersPage() {
  return (
    <main className="page">
      <Container>
        <AdminOrdersView />
      </Container>
    </main>
  );
}