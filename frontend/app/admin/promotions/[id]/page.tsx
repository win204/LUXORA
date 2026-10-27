import type { Metadata } from "next";
import { AdminPromotionForm } from "@/components/AdminPromotionForm";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Edit Promotion | LUXORA Admin",
  description: "Update promotional voucher settings and rules."
};

export default async function EditAdminPromotionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <main className="page">
      <Container>
        <AdminPromotionForm promotionId={id} />
      </Container>
    </main>
  );
}