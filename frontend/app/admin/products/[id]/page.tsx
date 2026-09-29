import type { Metadata } from "next";
import { AdminProductForm } from "@/components/AdminProductForm";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Edit Admin Product | LUXORA",
  description: "Manage a LUXORA catalog product."
};

export default async function EditAdminProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <main className="page">
      <Container>
        <AdminProductForm productId={id} />
      </Container>
    </main>
  );
}