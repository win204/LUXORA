import type { Metadata } from "next";
import { AccountReturnDetailsView } from "@/components/AccountReturnDetailsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Return Detail | LUXORA",
  description: "View your LUXORA return request."
};

export default async function AccountReturnDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <main className="page"><Container><AccountReturnDetailsView returnId={id} /></Container></main>;
}
