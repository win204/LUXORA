import type { Metadata } from "next";
import { AdminReturnsView } from "@/components/AdminReturnsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Admin Returns | LUXORA",
  description: "Review LUXORA return requests."
};

export default function AdminReturnsPage() {
  return <main className="page"><Container><AdminReturnsView /></Container></main>;
}
