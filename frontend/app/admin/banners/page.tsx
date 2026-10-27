import type { Metadata } from "next";
import { AdminBannersView } from "@/components/AdminBannersView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Banners | LUXORA",
  description: "Manage promotional hero banners and marketing campaigns."
};

export default function AdminBannersPage() {
  return (
    <main className="page">
      <Container>
        <AdminBannersView />
      </Container>
    </main>
  );
}
