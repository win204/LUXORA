import type { Metadata } from "next";
import { Container } from "@/components/Container";
import { OrderDetailsView } from "@/components/OrderDetailsView";

export const metadata: Metadata = {
  title: "Order | LUXORA",
  description: "View your LUXORA order confirmation."
};

type OrderPageProps = {
  params: Promise<{ id: string }>;
};

export default async function OrderPage({ params }: OrderPageProps) {
  const { id } = await params;

  return (
    <main className="page">
      <Container>
        <OrderDetailsView orderId={id} />
      </Container>
    </main>
  );
}