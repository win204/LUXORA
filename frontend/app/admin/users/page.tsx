import type { Metadata } from "next";
import { AdminUsersView } from "@/components/AdminUsersView";
import { Container } from "@/components/Container";

export const metadata: Metadata = {
  title: "Quản Lý Hội Viên & Tài Khoản | LUXORA Admin",
  description: "Quản lý danh sách hội viên VIP, thêm, xóa và khóa tài khoản người dùng."
};

export default function AdminUsersPage() {
  return (
    <main className="page">
      <Container>
        <AdminUsersView />
      </Container>
    </main>
  );
}
