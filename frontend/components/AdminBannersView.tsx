"use client";

import { useEffect, useState, useCallback } from "react";
import Image from "next/image";
import { AdminShell } from "@/components/AdminShell";
import { Button } from "@/components/Button";
import { useAuth } from "@/components/AuthProvider";
import {
  fetchAdminBanners,
  createBanner,
  updateBanner,
  toggleBannerStatus,
  deleteBanner
} from "@/lib/bannerClient";
import type { BannerResponse, BannerUpsertPayload } from "@/lib/types";

export function AdminBannersView() {
  const { accessToken } = useAuth();
  const [banners, setBanners] = useState<BannerResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [editingBanner, setEditingBanner] = useState<BannerResponse | null>(null);

  // Form Fields
  const [title, setTitle] = useState("");
  const [subtitle, setSubtitle] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [linkUrl, setLinkUrl] = useState("/shop");
  const [ctaText, setCtaText] = useState("Khám phá ngay");
  const [displayOrder, setDisplayOrder] = useState("0");
  const [active, setActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const reloadBanners = useCallback(async () => {
    try {
      const pageRes = await fetchAdminBanners(accessToken || undefined, 0, 50);
      setBanners(pageRes.content);
      setError("");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể tải danh sách banner.");
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        const pageRes = await fetchAdminBanners(accessToken || undefined, 0, 50);
        if (!ignore) {
          setBanners(pageRes.content);
        }
      } catch (caught) {
        if (!ignore) {
          setError(caught instanceof Error ? caught.message : "Không thể tải danh sách banner.");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      ignore = true;
    };
  }, [accessToken]);

  function openCreateModal() {
    setEditingBanner(null);
    setTitle("");
    setSubtitle("");
    setImageUrl("");
    setLinkUrl("/shop");
    setCtaText("Khám phá ngay");
    setDisplayOrder(String(banners.length));
    setActive(true);
    setShowModal(true);
  }

  function openEditModal(banner: BannerResponse) {
    setEditingBanner(banner);
    setTitle(banner.title);
    setSubtitle(banner.subtitle || "");
    setImageUrl(banner.imageUrl);
    setLinkUrl(banner.linkUrl || "/shop");
    setCtaText(banner.ctaText || "Khám phá ngay");
    setDisplayOrder(String(banner.displayOrder));
    setActive(banner.active);
    setShowModal(true);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError("");

    const payload: BannerUpsertPayload = {
      title,
      subtitle: subtitle || undefined,
      imageUrl,
      linkUrl: linkUrl || undefined,
      ctaText: ctaText || undefined,
      displayOrder: parseInt(displayOrder, 10) || 0,
      active
    };

    try {
      if (editingBanner) {
        await updateBanner(accessToken || "", editingBanner.id, payload);
        setActionSuccess(`Đã cập nhật banner "${title}" thành công.`);
      } else {
        await createBanner(accessToken || "", payload);
        setActionSuccess(`Đã tạo mới banner "${title}" thành công.`);
      }
      setShowModal(false);
      await reloadBanners();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Có lỗi xảy ra khi lưu banner.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(id: string) {
    try {
      await toggleBannerStatus(accessToken || "", id);
      setActionSuccess("Đã cập nhật trạng thái hiển thị của banner.");
      await reloadBanners();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Lỗi khi cập nhật trạng thái banner.");
    }
  }

  async function handleDelete(id: string, bannerTitle: string) {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa banner "${bannerTitle}" không?`)) {
      return;
    }

    try {
      await deleteBanner(accessToken || "", id);
      setActionSuccess(`Đã xóa banner "${bannerTitle}" khỏi hệ thống.`);
      await reloadBanners();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể xóa banner này.");
    }
  }

  return (
    <AdminShell>
      <div className="admin-heading admin-heading-row">
        <div>
          <p className="eyebrow">Content & Marketing</p>
          <h1>Quản Lý Banner Quảng Cáo</h1>
        </div>
        <Button type="button" onClick={openCreateModal}>
          + Tạo Banner Mới
        </Button>
      </div>

      {actionSuccess ? (
        <div className="state-panel" style={{ background: "rgba(22, 101, 52, 0.08)", border: "1px solid rgba(22, 101, 52, 0.2)", color: "#166534" }}>
          {actionSuccess}
        </div>
      ) : null}

      {error ? (
        <div className="state-panel" style={{ background: "rgba(153, 27, 27, 0.08)", border: "1px solid rgba(153, 27, 27, 0.2)", color: "#991b1b" }}>
          {error}
        </div>
      ) : null}

      {loading ? (
        <div className="state-panel" aria-busy="true">
          Đang tải danh sách banner...
        </div>
      ) : banners.length === 0 ? (
        <div className="state-panel">
          <p>Chưa có banner nào trong hệ thống.</p>
          <Button type="button" onClick={openCreateModal} style={{ marginTop: 12 }}>
            Tạo banner đầu tiên
          </Button>
        </div>
      ) : (
        <div style={{ display: "grid", gap: 16 }}>
          {banners.map((b) => (
            <article
              key={b.id}
              className="admin-card"
              style={{
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                justifyContent: "space-between",
                gap: 20
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 18, minWidth: 260, flex: 1 }}>
                <div
                  style={{
                    position: "relative",
                    width: 130,
                    height: 75,
                    borderRadius: 10,
                    overflow: "hidden",
                    border: "1px solid var(--line-strong)",
                    background: "var(--surface-soft)",
                    flexShrink: 0
                  }}
                >
                  <Image
                    src={b.imageUrl}
                    alt={b.title}
                    fill
                    sizes="130px"
                    style={{ objectFit: "cover" }}
                    unoptimized
                  />
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
                    <span style={{ fontSize: "0.72rem", color: "var(--muted)", fontWeight: 700 }}>
                      Thứ tự: {b.displayOrder}
                    </span>
                    <span className={`status-badge ${b.active ? "status-paid" : "status-pending"}`}>
                      {b.active ? "Đang hiển thị" : "Tạm ẩn"}
                    </span>
                  </div>
                  <h3 style={{ margin: "2px 0 4px", fontSize: "1.05rem", fontFamily: "var(--font-display), serif", color: "var(--foreground)" }}>
                    {b.title}
                  </h3>
                  {b.subtitle ? (
                    <p style={{ margin: 0, fontSize: "0.82rem", color: "var(--muted)", maxWidth: 540 }}>
                      {b.subtitle}
                    </p>
                  ) : null}
                  <div style={{ marginTop: 6, fontSize: "0.75rem", color: "var(--gold-strong)", display: "flex", gap: 10 }}>
                    <span>Link: {b.linkUrl || "—"}</span>
                    <span>•</span>
                    <span>Nút: &quot;{b.ctaText || "—"}&quot;</span>
                  </div>
                </div>
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => void handleToggleStatus(b.id)}
                  style={{ fontSize: "0.8rem", padding: "8px 14px" }}
                >
                  {b.active ? "Tạm ẩn" : "Kích hoạt"}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => openEditModal(b)}
                  style={{ fontSize: "0.8rem", padding: "8px 14px" }}
                >
                  Chỉnh sửa
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => void handleDelete(b.id, b.title)}
                  style={{ fontSize: "0.8rem", padding: "8px 14px", color: "#991b1b", borderColor: "rgba(153, 27, 27, 0.3)" }}
                >
                  Xóa
                </Button>
              </div>
            </article>
          ))}
        </div>
      )}

      {showModal ? (
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
              maxWidth: 600,
              background: "#ffffff",
              borderRadius: 16,
              border: "1px solid rgba(179, 138, 56, 0.35)",
              boxShadow: "0 24px 60px rgba(35, 28, 20, 0.2)",
              padding: 28
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid var(--line)", paddingBottom: 16, marginBottom: 20 }}>
              <h2 style={{ margin: 0, fontSize: "1.4rem", fontFamily: "var(--font-display), serif" }}>
                {editingBanner ? "Chỉnh sửa Banner" : "Tạo Banner Mới"}
              </h2>
              <button
                type="button"
                onClick={() => setShowModal(false)}
                style={{ background: "none", border: "none", fontSize: "1.5rem", cursor: "pointer", color: "var(--muted)" }}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmit} style={{ display: "grid", gap: 16 }}>
              <div>
                <label style={{ display: "block", fontSize: "0.75rem", fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--muted)", marginBottom: 6 }}>
                  Tiêu đề chính *
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="VD: Tuyệt Tác Công Nghệ Đỉnh Cao"
                  className="input"
                  style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid var(--line-strong)" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.75rem", fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--muted)", marginBottom: 6 }}>
                  Phụ đề mô tả
                </label>
                <textarea
                  rows={2}
                  value={subtitle}
                  onChange={(e) => setSubtitle(e.target.value)}
                  placeholder="VD: Bộ sưu tập phiên bản giới hạn chế tác từ Titanium cấp 5..."
                  className="input"
                  style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid var(--line-strong)" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.75rem", fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--muted)", marginBottom: 6 }}>
                  URL Hình ảnh (Image URL) *
                </label>
                <input
                  type="url"
                  required
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                  placeholder="https://images.unsplash.com/photo-..."
                  className="input"
                  style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid var(--line-strong)" }}
                />
                {imageUrl ? (
                  <div style={{ marginTop: 10, position: "relative", width: "100%", height: 110, borderRadius: 8, overflow: "hidden", border: "1px solid var(--line)" }}>
                    <Image src={imageUrl} alt="Preview" fill sizes="500px" style={{ objectFit: "cover" }} unoptimized />
                  </div>
                ) : null}
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.75rem", fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--muted)", marginBottom: 6 }}>
                    Link điều hướng
                  </label>
                  <input
                    type="text"
                    value={linkUrl}
                    onChange={(e) => setLinkUrl(e.target.value)}
                    placeholder="/shop"
                    className="input"
                    style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid var(--line-strong)" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.75rem", fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--muted)", marginBottom: 6 }}>
                    Chữ trên nút (CTA)
                  </label>
                  <input
                    type="text"
                    value={ctaText}
                    onChange={(e) => setCtaText(e.target.value)}
                    placeholder="Khám phá ngay"
                    className="input"
                    style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid var(--line-strong)" }}
                  />
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14, alignItems: "center" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.75rem", fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "var(--muted)", marginBottom: 6 }}>
                    Thứ tự hiển thị
                  </label>
                  <input
                    type="number"
                    value={displayOrder}
                    onChange={(e) => setDisplayOrder(e.target.value)}
                    className="input"
                    style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid var(--line-strong)" }}
                  />
                </div>
                <div style={{ paddingTop: 18 }}>
                  <label style={{ display: "inline-flex", alignItems: "center", gap: 8, cursor: "pointer", fontSize: "0.88rem", fontWeight: 600 }}>
                    <input
                      type="checkbox"
                      checked={active}
                      onChange={(e) => setActive(e.target.checked)}
                      style={{ width: 18, height: 18 }}
                    />
                    <span>Kích hoạt hiển thị</span>
                  </label>
                </div>
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, borderTop: "1px solid var(--line)", paddingTop: 18, marginTop: 8 }}>
                <Button type="button" variant="secondary" onClick={() => setShowModal(false)}>
                  Hủy bỏ
                </Button>
                <Button type="submit" disabled={submitting}>
                  {submitting ? "Đang lưu..." : editingBanner ? "Lưu thay đổi" : "Tạo Banner"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </AdminShell>
  );
}
