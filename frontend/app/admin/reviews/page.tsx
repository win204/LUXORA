import type { Metadata } from "next";
import { AdminShell } from "@/components/AdminShell";
import { AdminReviewsView } from "@/components/AdminReviewsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Reviews Moderation | LUXORA Admin",
  description: "Moderate product reviews and customer ratings"
};

export default function AdminReviewsPage() {
  return (
    <main className="page">
      <Container>
        <AdminShell>
          <AdminReviewsView />
        </AdminShell>
      </Container>
    </main>
  );
}
