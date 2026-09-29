import type { Metadata } from "next";
import { AdminProductForm } from "@/components/AdminProductForm";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "New Admin Product | LUXORA",
  description: "Create a LUXORA catalog product."
};

export default function NewAdminProductPage() {
  return (
    <main className="page">
      <Container>
        <AdminProductForm />
      </Container>
    </main>
  );
}