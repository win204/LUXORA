import type { Metadata } from "next";
import { AdminFlashSalesView } from "@/components/AdminFlashSalesView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Flash Sales | LUXORA Admin",
  description: "Manage time-boxed flash sales and countdown campaigns."
};

export default function AdminFlashSalesPage() {
  return (
    <main className="page">
      <Container>
        <AdminFlashSalesView />
      </Container>
    </main>
  );
}
