import type { Metadata } from "next";
import { AdminDashboardView } from "@/components/AdminDashboardView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin | LUXORA",
  description: "LUXORA operational dashboard."
};

export default function AdminPage() {
  return (
    <main className="page">
      <Container>
        <AdminDashboardView />
      </Container>
    </main>
  );
}