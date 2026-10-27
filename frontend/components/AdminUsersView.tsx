"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { AdminShell } from "@/components/AdminShell";
import { Button } from "@/components/Button";
import { useAuth } from "@/components/AuthProvider";
import {
  fetchAdminUsers,
  createAdminUser,
  updateAdminUserStatus,
  deleteAdminUser,
  AdminUserApiError
} from "@/lib/adminUserClient";
import type { AdminUserResponse, AdminCreateUserPayload } from "@/lib/types";

export function AdminUsersView() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Search & Filter
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "LOCKED">("ALL");
  const [roleFilter, setRoleFilter] = useState<"ALL" | "ADMIN" | "USER">("ALL");

  // Create Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [formEmail, setFormEmail] = useState("");
  const [formPassword, setFormPassword] = useState("");
  const [formFirstName, setFormFirstName] = useState("");
  const [formLastName, setFormLastName] = useState("");
  const [formRole, setFormRole] = useState("ROLE_USER");
  const [submitting, setSubmitting] = useState(false);

  // Delete Confirm Modal
  const [deletingUser, setDeletingUser] = useState<AdminUserResponse | null>(null);
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await fetchAdminUsers();
      setUsers(data);
      setError("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải danh sách tài khoản.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const showSuccess = (msg: string) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(""), 4000);
  };

  // Toggle Lock/Unlock
  const handleToggleStatus = async (targetUser: AdminUserResponse) => {
    try {
      setActionLoadingId(targetUser.id);
      const newStatus = !targetUser.enabled;
      await updateAdminUserStatus(targetUser.id, newStatus);
      setUsers((prev) =>
        prev.map((u) => (u.id === targetUser.id ? { ...u, enabled: newStatus } : u))
      );
      showSuccess(
        newStatus
          ? `Đã mở khóa tài khoản ${targetUser.email} thành công.`
          : `Đã khóa tài khoản ${targetUser.email}. Tất cả phiên đăng nhập đã bị vô hiệu hóa.`
      );
    } catch (err) {
      setError(err instanceof AdminUserApiError ? err.message : "Không thể cập nhật trạng thái tài khoản.");
    } finally {
      setActionLoadingId(null);
    }
  };

  // Delete User
  const confirmDelete = async () => {
    if (!deletingUser) return;
    try {
      setActionLoadingId(deletingUser.id);
      await deleteAdminUser(deletingUser.id);
      setUsers((prev) => prev.filter((u) => u.id !== deletingUser.id));
      showSuccess(`Đã xóa vĩnh viễn tài khoản ${deletingUser.email}.`);
      setDeletingUser(null);
    } catch (err) {
      setError(err instanceof AdminUserApiError ? err.message : "Không thể xóa tài khoản này.");
    } finally {
      setActionLoadingId(null);
    }
  };

  // Create User
  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formEmail || !formPassword || !formFirstName || !formLastName) {
      setError("Vui lòng điền đầy đủ các trường thông tin bắt buộc.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      const payload: AdminCreateUserPayload = {
        email: formEmail.trim(),
        password: formPassword,
        firstName: formFirstName.trim(),
        lastName: formLastName.trim(),
        role: formRole
      };
      const created = await createAdminUser(payload);
      setUsers((prev) => [created, ...prev]);
      showSuccess(`Đã tạo tài khoản ${created.email} thành công.`);
      setShowCreateModal(false);
      setFormEmail("");
      setFormPassword("");
      setFormFirstName("");
      setFormLastName("");
      setFormRole("ROLE_USER");
    } catch (err) {
      setError(err instanceof AdminUserApiError ? err.message : "Lỗi khi tạo tài khoản mới.");
    } finally {
      setSubmitting(false);
    }
  };

  // Filtered Users
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchSearch =
        u.email.toLowerCase().includes(search.toLowerCase()) ||
        u.fullName.toLowerCase().includes(search.toLowerCase());

      const matchStatus =
        statusFilter === "ALL"
          ? true
          : statusFilter === "ACTIVE"
          ? u.enabled
          : !u.enabled;

      const matchRole =
        roleFilter === "ALL"
          ? true
          : roleFilter === "ADMIN"
          ? u.roles.includes("ROLE_ADMIN")
          : !u.roles.includes("ROLE_ADMIN");

      return matchSearch && matchStatus && matchRole;
    });
  }, [users, search, statusFilter, roleFilter]);

  // Statistics
  const totalUsers = users.length;
  const activeCount = users.filter((u) => u.enabled).length;
  const lockedCount = users.filter((u) => !u.enabled).length;
  const adminCount = users.filter((u) => u.roles.includes("ROLE_ADMIN")).length;

  return (
    <AdminShell>
      <div style={{ maxWidth: 1200, margin: "0 auto", paddingBottom: 60 }}>
        {/* Header */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            flexWrap: "wrap",
            gap: 16,
            marginBottom: 28
          }}
        >
          <div>
            <span
              style={{
                display: "inline-block",
                fontSize: "0.75rem",
                textTransform: "uppercase",
                letterSpacing: "0.15em",
                color: "var(--accent-gold, #B38A38)",
                fontWeight: 600,
                marginBottom: 6
              }}
            >
              Hệ Thống Phân Quyền & Thành Viên
            </span>
            <h1
              style={{
                margin: 0,
                fontFamily: "var(--font-display), serif",
                fontSize: "1.9rem",
                color: "#1B1612",
                fontWeight: 600
              }}
            >
              Quản Lý Hội Viên & Tài Khoản
            </h1>
            <p style={{ margin: "4px 0 0", color: "#666", fontSize: "0.95rem" }}>
              Quản trị danh sách khách hàng VIP, phân quyền quản trị viên và kiểm soát trạng thái hoạt động.
            </p>
          </div>

          <button
            type="button"
            onClick={() => {
              setError("");
              setShowCreateModal(true);
            }}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 8,
              background: "linear-gradient(135deg, #C5A059 0%, #A87932 100%)",
              color: "#FFFFFF",
              border: "none",
              borderRadius: 8,
              padding: "12px 22px",
              fontSize: "0.9rem",
              fontWeight: 600,
              cursor: "pointer",
              boxShadow: "0 4px 14px rgba(179, 138, 56, 0.35)",
              transition: "all 0.2s ease"
            }}
          >
            <span style={{ fontSize: "1.2rem", lineHeight: 1 }}>+</span>
            Thêm Hội Viên Mới
          </button>
        </div>

        {/* Notifications */}
        {error && (
          <div
            style={{
              padding: "14px 18px",
              background: "#FEF2F2",
              border: "1px solid #FCA5A5",
              borderRadius: 8,
              color: "#991B1B",
              fontSize: "0.9rem",
              marginBottom: 20,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center"
            }}
          >
            <span>{error}</span>
            <button
              onClick={() => setError("")}
              style={{ background: "none", border: "none", cursor: "pointer", color: "#991B1B", fontWeight: 700 }}
            >
              &times;
            </button>
          </div>
        )}

        {successMsg && (
          <div
            style={{
              padding: "14px 18px",
              background: "#F0FDF4",
              border: "1px solid #86EFAC",
              borderRadius: 8,
              color: "#166534",
              fontSize: "0.9rem",
              marginBottom: 20
            }}
          >
            {successMsg}
          </div>
        )}

        {/* Metric Stat Cards */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
            gap: 16,
            marginBottom: 28
          }}
        >
          <div
            style={{
              background: "#FFFFFF",
              border: "1px solid rgba(179, 138, 56, 0.25)",
              borderRadius: 12,
              padding: "20px 24px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.03)"
            }}
          >
            <span style={{ fontSize: "0.8rem", color: "#777", textTransform: "uppercase", letterSpacing: "0.08em" }}>
              Tổng Hội Viên
            </span>
            <div style={{ fontSize: "1.8rem", fontWeight: 700, color: "#1B1612", marginTop: 4 }}>
              {totalUsers}
            </div>
            <span style={{ fontSize: "0.8rem", color: "#888" }}>Đã đăng ký tài khoản</span>
          </div>

          <div
            style={{
              background: "#FFFFFF",
              border: "1px solid rgba(34, 197, 94, 0.25)",
              borderRadius: 12,
              padding: "20px 24px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.03)"
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <span
                style={{
                  width: 8,
                  height: 8,
                  borderRadius: "50%",
                  background: "#22C55E",
                  display: "inline-block"
                }}
              />
              <span style={{ fontSize: "0.8rem", color: "#777", textTransform: "uppercase", letterSpacing: "0.08em" }}>
                Đang Hoạt Động
              </span>
            </div>
            <div style={{ fontSize: "1.8rem", fontWeight: 700, color: "#166534", marginTop: 4 }}>
              {activeCount}
            </div>
            <span style={{ fontSize: "0.8rem", color: "#888" }}>Có quyền mua & đăng nhập</span>
          </div>

          <div
            style={{
              background: "#FFFFFF",
              border: "1px solid rgba(239, 68, 68, 0.25)",
              borderRadius: 12,
              padding: "20px 24px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.03)"
            }}
          >
            <span style={{ fontSize: "0.8rem", color: "#777", textTransform: "uppercase", letterSpacing: "0.08em" }}>
              Tài Khoản Đã Khóa
            </span>
            <div style={{ fontSize: "1.8rem", fontWeight: 700, color: "#991B1B", marginTop: 4 }}>
              {lockedCount}
            </div>
            <span style={{ fontSize: "0.8rem", color: "#888" }}>Bị đình chỉ quyền truy cập</span>
          </div>

          <div
            style={{
              background: "#FFFFFF",
              border: "1px solid rgba(179, 138, 56, 0.25)",
              borderRadius: 12,
              padding: "20px 24px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.03)"
            }}
          >
            <span style={{ fontSize: "0.8rem", color: "#777", textTransform: "uppercase", letterSpacing: "0.08em" }}>
              Quản Trị Viên
            </span>
            <div style={{ fontSize: "1.8rem", fontWeight: 700, color: "#B38A38", marginTop: 4 }}>
              {adminCount}
            </div>
            <span style={{ fontSize: "0.8rem", color: "#888" }}>Quyền quản trị cấp cao</span>
          </div>
        </div>

        {/* Filter & Search Bar */}
        <div
          style={{
            background: "#FFFFFF",
            border: "1px solid rgba(179, 138, 56, 0.2)",
            borderRadius: 12,
            padding: "16px 20px",
            display: "flex",
            flexWrap: "wrap",
            gap: 12,
            alignItems: "center",
            marginBottom: 20
          }}
        >
          <div style={{ flex: "1 1 240px" }}>
            <input
              type="text"
              placeholder="Tìm kiếm theo email, họ tên..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                border: "1px solid #E2DCD5",
                borderRadius: 8,
                fontSize: "0.9rem",
                outline: "none",
                background: "#FAF8F5"
              }}
            />
          </div>

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              style={{
                padding: "10px 14px",
                border: "1px solid #E2DCD5",
                borderRadius: 8,
                fontSize: "0.9rem",
                background: "#FAF8F5",
                cursor: "pointer"
              }}
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Chỉ đang hoạt động</option>
              <option value="LOCKED">Chỉ tài khoản bị khóa</option>
            </select>

            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value as any)}
              style={{
                padding: "10px 14px",
                border: "1px solid #E2DCD5",
                borderRadius: 8,
                fontSize: "0.9rem",
                background: "#FAF8F5",
                cursor: "pointer"
              }}
            >
              <option value="ALL">Tất cả vai trò</option>
              <option value="USER">Khách hàng VIP</option>
              <option value="ADMIN">Quản trị viên (Admin)</option>
            </select>
          </div>
        </div>

        {/* Users Table */}
        <div
          style={{
            background: "#FFFFFF",
            border: "1px solid rgba(179, 138, 56, 0.2)",
            borderRadius: 12,
            overflow: "hidden",
            boxShadow: "0 2px 12px rgba(0,0,0,0.02)"
          }}
        >
          {loading ? (
            <div style={{ padding: "50px 20px", textAlign: "center", color: "#777" }}>
              Đang tải danh sách tài khoản...
            </div>
          ) : filteredUsers.length === 0 ? (
            <div style={{ padding: "60px 20px", textAlign: "center", color: "#888" }}>
              <p style={{ margin: 0, fontSize: "1.1rem", fontWeight: 600, color: "#1B1612" }}>
                Không tìm thấy tài khoản nào
              </p>
              <p style={{ margin: "6px 0 0", fontSize: "0.9rem" }}>
                Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm.
              </p>
            </div>
          ) : (
            <div style={{ overflowX: "auto" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left" }}>
                <thead>
                  <tr style={{ background: "#FAF8F5", borderBottom: "1px solid #EAE5DE" }}>
                    <th style={{ padding: "14px 20px", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#777" }}>
                      Hội viên
                    </th>
                    <th style={{ padding: "14px 20px", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#777" }}>
                      Email
                    </th>
                    <th style={{ padding: "14px 20px", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#777" }}>
                      Vai trò
                    </th>
                    <th style={{ padding: "14px 20px", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#777" }}>
                      Trạng thái
                    </th>
                    <th style={{ padding: "14px 20px", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#777" }}>
                      Ngày gia nhập
                    </th>
                    <th style={{ padding: "14px 20px", fontSize: "0.75rem", textTransform: "uppercase", letterSpacing: "0.08em", color: "#777", textAlign: "right" }}>
                      Thao tác
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((u) => {
                    const isAdmin = u.roles.includes("ROLE_ADMIN");
                    const isSelf = currentUser?.id === u.id || currentUser?.email === u.email;
                    const isPrimaryAdmin = u.email.toLowerCase() === "admin@luxora.com";
                    const isBusy = actionLoadingId === u.id;

                    return (
                      <tr
                        key={u.id}
                        style={{
                          borderBottom: "1px solid #F0ECE6",
                          transition: "background 0.15s ease"
                        }}
                      >
                        {/* Member avatar & name */}
                        <td style={{ padding: "16px 20px" }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                            <div
                              style={{
                                width: 38,
                                height: 38,
                                borderRadius: "50%",
                                background: isAdmin
                                  ? "linear-gradient(135deg, #1B1612, #3E352C)"
                                  : "linear-gradient(135deg, #F0E6D2, #D8C29D)",
                                color: isAdmin ? "#FFFFFF" : "#59431D",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                fontWeight: 700,
                                fontSize: "0.9rem",
                                flexShrink: 0
                              }}
                            >
                              {(u.firstName?.[0] || u.email[0]).toUpperCase()}
                            </div>
                            <div>
                              <div style={{ fontWeight: 600, color: "#1B1612", fontSize: "0.95rem" }}>
                                {u.fullName || `${u.firstName} ${u.lastName}`}
                                {isSelf && (
                                  <span
                                    style={{
                                      marginLeft: 6,
                                      fontSize: "0.7rem",
                                      background: "#E0E7FF",
                                      color: "#3730A3",
                                      padding: "2px 6px",
                                      borderRadius: 4,
                                      fontWeight: 600
                                    }}
                                  >
                                    Bạn
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        </td>

                        {/* Email */}
                        <td style={{ padding: "16px 20px", color: "#555", fontSize: "0.9rem" }}>
                          {u.email}
                        </td>

                        {/* Role badge */}
                        <td style={{ padding: "16px 20px" }}>
                          {isAdmin ? (
                            <span
                              style={{
                                display: "inline-block",
                                padding: "4px 10px",
                                borderRadius: 6,
                                background: "rgba(179, 138, 56, 0.15)",
                                color: "#926B22",
                                fontWeight: 600,
                                fontSize: "0.75rem",
                                border: "1px solid rgba(179, 138, 56, 0.3)"
                              }}
                            >
                              👑 Quản trị viên
                            </span>
                          ) : (
                            <span
                              style={{
                                display: "inline-block",
                                padding: "4px 10px",
                                borderRadius: 6,
                                background: "#F3F4F6",
                                color: "#4B5563",
                                fontWeight: 500,
                                fontSize: "0.75rem"
                              }}
                            >
                              Khách hàng VIP
                            </span>
                          )}
                        </td>

                        {/* Status */}
                        <td style={{ padding: "16px 20px" }}>
                          {u.enabled ? (
                            <span
                              style={{
                                display: "inline-flex",
                                alignItems: "center",
                                gap: 6,
                                padding: "4px 10px",
                                borderRadius: 6,
                                background: "#DCFCE7",
                                color: "#166534",
                                fontWeight: 600,
                                fontSize: "0.75rem"
                              }}
                            >
                              <span style={{ width: 6, height: 6, borderRadius: "50%", background: "#16A34A" }} />
                              Hoạt động
                            </span>
                          ) : (
                            <span
                              style={{
                                display: "inline-flex",
                                alignItems: "center",
                                gap: 6,
                                padding: "4px 10px",
                                borderRadius: 6,
                                background: "#FEE2E2",
                                color: "#991B1B",
                                fontWeight: 600,
                                fontSize: "0.75rem"
                              }}
                            >
                              <span style={{ width: 6, height: 6, borderRadius: "50%", background: "#DC2626" }} />
                              Đã khóa
                            </span>
                          )}
                        </td>

                        {/* Created At */}
                        <td style={{ padding: "16px 20px", color: "#777", fontSize: "0.85rem" }}>
                          {u.createdAt ? new Date(u.createdAt).toLocaleDateString("vi-VN") : "—"}
                        </td>

                        {/* Actions */}
                        <td style={{ padding: "16px 20px", textAlign: "right" }}>
                          <div style={{ display: "inline-flex", alignItems: "center", gap: 8 }}>
                            {/* Lock / Unlock button */}
                            <button
                              type="button"
                              disabled={isBusy || isSelf || isPrimaryAdmin}
                              onClick={() => void handleToggleStatus(u)}
                              title={
                                isSelf
                                  ? "Không thể tự khóa tài khoản của chính mình"
                                  : isPrimaryAdmin
                                  ? "Không thể khóa quản trị viên chính"
                                  : u.enabled
                                  ? "Khóa tài khoản"
                                  : "Mở khóa tài khoản"
                              }
                              style={{
                                padding: "6px 12px",
                                borderRadius: 6,
                                fontSize: "0.8rem",
                                fontWeight: 600,
                                cursor: isSelf || isPrimaryAdmin || isBusy ? "not-allowed" : "pointer",
                                opacity: isSelf || isPrimaryAdmin ? 0.45 : 1,
                                border: u.enabled ? "1px solid #FCA5A5" : "1px solid #86EFAC",
                                background: u.enabled ? "#FFF1F2" : "#F0FDF4",
                                color: u.enabled ? "#BE123C" : "#15803D",
                                transition: "all 0.15s ease"
                              }}
                            >
                              {isBusy
                                ? "..."
                                : u.enabled
                                ? "🔒 Khóa"
                                : "🔓 Mở khóa"}
                            </button>

                            {/* Delete button */}
                            <button
                              type="button"
                              disabled={isBusy || isSelf || isPrimaryAdmin}
                              onClick={() => setDeletingUser(u)}
                              title={
                                isSelf
                                  ? "Không thể tự xóa tài khoản của chính mình"
                                  : isPrimaryAdmin
                                  ? "Không thể xóa quản trị viên chính"
                                  : "Xóa tài khoản"
                              }
                              style={{
                                padding: "6px 12px",
                                borderRadius: 6,
                                fontSize: "0.8rem",
                                fontWeight: 600,
                                cursor: isSelf || isPrimaryAdmin || isBusy ? "not-allowed" : "pointer",
                                opacity: isSelf || isPrimaryAdmin ? 0.45 : 1,
                                border: "1px solid #E5E7EB",
                                background: "#FFFFFF",
                                color: "#6B7280",
                                transition: "all 0.15s ease"
                              }}
                            >
                              Xóa
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Modal: Thêm Hội Viên Mới */}
        {showCreateModal && (
          <div
            style={{
              position: "fixed",
              inset: 0,
              zIndex: 100,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: 16,
              background: "rgba(20, 16, 12, 0.6)",
              backdropFilter: "blur(6px)"
            }}
          >
            <div
              style={{
                width: "100%",
                maxWidth: 520,
                background: "#FFFFFF",
                borderRadius: 16,
                border: "1px solid rgba(179, 138, 56, 0.35)",
                boxShadow: "0 24px 60px rgba(35, 28, 20, 0.2)",
                padding: 28
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  borderBottom: "1px solid #EAE5DE",
                  paddingBottom: 16,
                  marginBottom: 20
                }}
              >
                <div>
                  <h2
                    style={{
                      margin: 0,
                      fontSize: "1.35rem",
                      fontFamily: "var(--font-display), serif",
                      color: "#1B1612"
                    }}
                  >
                    Tạo Tài Khoản Mới
                  </h2>
                  <p style={{ margin: "4px 0 0", fontSize: "0.85rem", color: "#777" }}>
                    Thêm khách hàng VIP hoặc bổ sung quản trị viên vào hệ thống.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  style={{
                    background: "none",
                    border: "none",
                    fontSize: "1.5rem",
                    cursor: "pointer",
                    color: "#999"
                  }}
                >
                  &times;
                </button>
              </div>

              <form onSubmit={handleCreateSubmit} style={{ display: "grid", gap: 16 }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div>
                    <label
                      style={{
                        display: "block",
                        fontSize: "0.75rem",
                        fontWeight: 700,
                        letterSpacing: "0.06em",
                        textTransform: "uppercase",
                        color: "#555",
                        marginBottom: 6
                      }}
                    >
                      Họ *
                    </label>
                    <input
                      type="text"
                      required
                      value={formLastName}
                      onChange={(e) => setFormLastName(e.target.value)}
                      placeholder="VD: Nguyễn"
                      style={{
                        width: "100%",
                        padding: "10px 14px",
                        borderRadius: 8,
                        border: "1px solid #D5CFC9",
                        outline: "none",
                        fontSize: "0.9rem"
                      }}
                    />
                  </div>

                  <div>
                    <label
                      style={{
                        display: "block",
                        fontSize: "0.75rem",
                        fontWeight: 700,
                        letterSpacing: "0.06em",
                        textTransform: "uppercase",
                        color: "#555",
                        marginBottom: 6
                      }}
                    >
                      Tên *
                    </label>
                    <input
                      type="text"
                      required
                      value={formFirstName}
                      onChange={(e) => setFormFirstName(e.target.value)}
                      placeholder="VD: Hoàng Long"
                      style={{
                        width: "100%",
                        padding: "10px 14px",
                        borderRadius: 8,
                        border: "1px solid #D5CFC9",
                        outline: "none",
                        fontSize: "0.9rem"
                      }}
                    />
                  </div>
                </div>

                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.75rem",
                      fontWeight: 700,
                      letterSpacing: "0.06em",
                      textTransform: "uppercase",
                      color: "#555",
                      marginBottom: 6
                    }}
                  >
                    Địa chỉ Email *
                  </label>
                  <input
                    type="email"
                    required
                    value={formEmail}
                    onChange={(e) => setFormEmail(e.target.value)}
                    placeholder="vip.client@luxora.com"
                    style={{
                      width: "100%",
                      padding: "10px 14px",
                      borderRadius: 8,
                      border: "1px solid #D5CFC9",
                      outline: "none",
                      fontSize: "0.9rem"
                    }}
                  />
                </div>

                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.75rem",
                      fontWeight: 700,
                      letterSpacing: "0.06em",
                      textTransform: "uppercase",
                      color: "#555",
                      marginBottom: 6
                    }}
                  >
                    Mật khẩu khởi tạo * (Tối thiểu 8 ký tự)
                  </label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={formPassword}
                    onChange={(e) => setFormPassword(e.target.value)}
                    placeholder="Mật khẩu bảo mật..."
                    style={{
                      width: "100%",
                      padding: "10px 14px",
                      borderRadius: 8,
                      border: "1px solid #D5CFC9",
                      outline: "none",
                      fontSize: "0.9rem"
                    }}
                  />
                </div>

                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "0.75rem",
                      fontWeight: 700,
                      letterSpacing: "0.06em",
                      textTransform: "uppercase",
                      color: "#555",
                      marginBottom: 6
                    }}
                  >
                    Vai trò tài khoản *
                  </label>
                  <select
                    value={formRole}
                    onChange={(e) => setFormRole(e.target.value)}
                    style={{
                      width: "100%",
                      padding: "10px 14px",
                      borderRadius: 8,
                      border: "1px solid #D5CFC9",
                      outline: "none",
                      fontSize: "0.9rem",
                      background: "#FFFFFF",
                      cursor: "pointer"
                    }}
                  >
                    <option value="ROLE_USER">Khách hàng VIP (Mua hàng, tích điểm, nhận ưu đãi)</option>
                    <option value="ROLE_ADMIN">Quản trị viên Admin (Toàn quyền quản lý cửa hàng)</option>
                  </select>
                </div>

                <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: 12 }}>
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    style={{
                      padding: "10px 18px",
                      borderRadius: 8,
                      border: "1px solid #D5CFC9",
                      background: "#FFFFFF",
                      color: "#555",
                      fontSize: "0.9rem",
                      cursor: "pointer"
                    }}
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    style={{
                      padding: "10px 22px",
                      borderRadius: 8,
                      border: "none",
                      background: "linear-gradient(135deg, #C5A059 0%, #A87932 100%)",
                      color: "#FFFFFF",
                      fontSize: "0.9rem",
                      fontWeight: 600,
                      cursor: submitting ? "not-allowed" : "pointer",
                      boxShadow: "0 4px 12px rgba(179, 138, 56, 0.3)"
                    }}
                  >
                    {submitting ? "Đang tạo..." : "Xác nhận tạo tài khoản"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Modal: Xác nhận Xóa Tài Khoản */}
        {deletingUser && (
          <div
            style={{
              position: "fixed",
              inset: 0,
              zIndex: 100,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: 16,
              background: "rgba(20, 16, 12, 0.6)",
              backdropFilter: "blur(6px)"
            }}
          >
            <div
              style={{
                width: "100%",
                maxWidth: 460,
                background: "#FFFFFF",
                borderRadius: 16,
                border: "1px solid rgba(220, 38, 38, 0.3)",
                boxShadow: "0 24px 60px rgba(35, 28, 20, 0.2)",
                padding: 24
              }}
            >
              <div style={{ textAlign: "center", marginBottom: 16 }}>
                <div
                  style={{
                    width: 52,
                    height: 52,
                    borderRadius: "50%",
                    background: "#FEE2E2",
                    color: "#DC2626",
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "1.5rem",
                    marginBottom: 12
                  }}
                >
                  ⚠️
                </div>
                <h3 style={{ margin: 0, fontSize: "1.25rem", color: "#1B1612" }}>
                  Xác nhận xóa tài khoản?
                </h3>
                <p style={{ margin: "8px 0 0", color: "#666", fontSize: "0.9rem" }}>
                  Bạn có chắc chắn muốn xóa tài khoản <strong>{deletingUser.email}</strong>? Hành động này không thể hoàn tác. Lịch sử đơn hàng sẽ được bảo lưu an toàn.
                </p>
              </div>

              <div style={{ display: "flex", justifyContent: "center", gap: 12, marginTop: 20 }}>
                <button
                  type="button"
                  onClick={() => setDeletingUser(null)}
                  style={{
                    padding: "10px 18px",
                    borderRadius: 8,
                    border: "1px solid #D5CFC9",
                    background: "#FFFFFF",
                    color: "#555",
                    fontSize: "0.9rem",
                    cursor: "pointer"
                  }}
                >
                  Hủy bỏ
                </button>
                <button
                  type="button"
                  onClick={() => void confirmDelete()}
                  style={{
                    padding: "10px 22px",
                    borderRadius: 8,
                    border: "none",
                    background: "#DC2626",
                    color: "#FFFFFF",
                    fontSize: "0.9rem",
                    fontWeight: 600,
                    cursor: "pointer"
                  }}
                >
                  Xác nhận xóa
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminShell>
  );
}
