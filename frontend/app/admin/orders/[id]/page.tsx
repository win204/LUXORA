import type { Metadata } from "next";
import { AdminOrderDetailsView } from "@/components/AdminOrderDetailsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Order | LUXORA",
  description: "Review and operate a LUXORA order."
};

export default async function AdminOrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <main className="page">
      <Container>
        <AdminOrderDetailsView orderId={id} />
      </Container>
    </main>
  );
}