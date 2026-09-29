import type { Metadata } from "next";
import { AdminReturnDetailsView } from "@/components/AdminReturnDetailsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Return Detail | LUXORA",
  description: "Review a LUXORA return request."
};

export default async function AdminReturnDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <main className="page"><Container><AdminReturnDetailsView returnId={id} /></Container></main>;
}
