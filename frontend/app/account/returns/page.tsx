import type { Metadata } from "next";
import { AccountReturnsView } from "@/components/AccountReturnsView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Returns | LUXORA",
  description: "View your LUXORA return requests."
};

export default function AccountReturnsPage() {
  return <main className="page"><Container><AccountReturnsView /></Container></main>;
}
