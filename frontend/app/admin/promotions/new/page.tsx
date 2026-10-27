import type { Metadata } from "next";
import { AdminPromotionForm } from "@/components/AdminPromotionForm";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "New Promotion | LUXORA Admin",
  description: "Create a new discount code or promotional voucher."
};

export default function NewAdminPromotionPage() {
  return (
    <main className="page">
      <Container>
        <AdminPromotionForm />
      </Container>
    </main>
  );
}