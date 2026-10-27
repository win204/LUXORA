import type { Metadata } from "next";
import { AdminPromotionsView } from "@/components/AdminPromotionsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Promotions | LUXORA",
  description: "Manage discount codes and promotional vouchers."
};

export default function AdminPromotionsPage() {
  return (
    <main className="page">
      <Container>
        <AdminPromotionsView />
      </Container>
    </main>
  );
}