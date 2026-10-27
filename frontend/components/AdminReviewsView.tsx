"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/Button";
import {
  deleteAdminReview,
  fetchAdminReviews,
  updateAdminReviewStatus
} from "@/lib/reviewClient";
import type { AdminReview, PageResponse, ReviewStatus } from "@/lib/types";

function StarIcon({ filled, className = "w-4 h-4" }: { filled: boolean; className?: string }) {
  return (
    <svg
      className={className}
      style={{ width: 16, height: 16, fill: filled ? "var(--gold)" : "transparent", color: filled ? "var(--gold)" : "#d2cbbe" }}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={filled ? "1" : "1.5"}
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z"
      />
    </svg>
  );
}

export function AdminReviewsView() {
  const { accessToken } = useAuth();

  const [selectedStatus, setSelectedStatus] = useState<ReviewStatus | "ALL">("ALL");
  const [reviewsPage, setReviewsPage] = useState<PageResponse<AdminReview>>({
    content: [],
    page: 0,
    size: 15,
    totalElements: 0,
    totalPages: 0
  });
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [processingId, setProcessingId] = useState<string | null>(null);

  // Lightbox modal state
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);

  const reloadReviews = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const statusFilter = selectedStatus === "ALL" ? undefined : selectedStatus;
      const data = await fetchAdminReviews(accessToken || "", statusFilter, currentPage, 15);
      setReviewsPage(data);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Failed to load reviews queue.");
    } finally {
      setLoading(false);
    }
  }, [accessToken, selectedStatus, currentPage]);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        setLoading(true);
        setError(null);
        const statusFilter = selectedStatus === "ALL" ? undefined : selectedStatus;
        const data = await fetchAdminReviews(accessToken || "", statusFilter, currentPage, 15);
        if (active) {
          setReviewsPage(data);
        }
      } catch (caught) {
        if (active) {
          setError(caught instanceof Error ? caught.message : "Failed to load reviews queue.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [accessToken, selectedStatus, currentPage]);

  const handleUpdateStatus = async (reviewId: string, status: ReviewStatus) => {
    try {
      setProcessingId(reviewId);
      setActionMessage(null);
      await updateAdminReviewStatus(accessToken || "", reviewId, status);
      setActionMessage(`Đã cập nhật trạng thái đánh giá thành "${status}".`);
      await reloadReviews();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể cập nhật trạng thái đánh giá.");
    } finally {
      setProcessingId(null);
    }
  };

  const handleDelete = async (reviewId: string) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa đánh giá này khỏi hệ thống?")) {
      return;
    }

    try {
      setProcessingId(reviewId);
      setActionMessage(null);
      await deleteAdminReview(accessToken || "", reviewId);
      setActionMessage("Đã xóa đánh giá thành công.");
      await reloadReviews();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Không thể xóa đánh giá này.");
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div style={{ display: "grid", gap: 24 }}>
      <div className="admin-heading admin-heading-row" style={{ marginBottom: 0 }}>
        <div>
          <p className="eyebrow">Customer Feedback & S3 Media</p>
          <h1>Kiểm Duyệt Đánh Giá</h1>
        </div>

        {/* Status Filter Tabs */}
        <div className="period-selector">
          {(["ALL", "PENDING", "APPROVED", "REJECTED"] as const).map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => {
                setSelectedStatus(status);
                setCurrentPage(0);
              }}
              className={`period-btn ${selectedStatus === status ? "active" : ""}`}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      {actionMessage && (
        <div className="state-panel" style={{ background: "rgba(22, 101, 52, 0.08)", border: "1px solid rgba(22, 101, 52, 0.2)", color: "#166534" }}>
          {actionMessage}
        </div>
      )}

      {error && (
        <div className="state-panel" style={{ background: "rgba(153, 27, 27, 0.08)", border: "1px solid rgba(153, 27, 27, 0.2)", color: "#991b1b" }}>
          {error}
        </div>
      )}

      {/* Reviews List */}
      <div className="admin-card" style={{ padding: 0, overflow: "hidden" }}>
        {loading ? (
          <div className="state-panel" aria-busy="true">
            Đang tải dữ liệu đánh giá...
          </div>
        ) : reviewsPage.content.length === 0 ? (
          <div className="state-panel">
            Không tìm thấy đánh giá nào với trạng thái: {selectedStatus}.
          </div>
        ) : (
          <div>
            {reviewsPage.content.map((review, idx) => (
              <div
                key={review.id}
                style={{
                  padding: 24,
                  borderTop: idx > 0 ? "1px solid var(--line)" : "none",
                  display: "grid",
                  gap: 12
                }}
              >
                <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <strong style={{ fontSize: "0.95rem", color: "var(--foreground)" }}>{review.userName}</strong>
                    <span style={{ fontSize: "0.78rem", color: "var(--muted)" }}>({review.userEmail})</span>
                    {review.verifiedPurchase && (
                      <span className="badge-luxury" style={{ fontSize: "0.65rem", padding: "2px 8px" }}>
                        ✓ Đã mua hàng
                      </span>
                    )}
                  </div>

                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <span
                      className={`status-badge ${
                        review.status === "APPROVED"
                          ? "status-paid"
                          : review.status === "REJECTED"
                          ? "status-cancelled"
                          : "status-pending"
                      }`}
                    >
                      {review.status}
                    </span>
                    <span style={{ fontSize: "0.78rem", color: "var(--muted)" }}>
                      {new Date(review.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div style={{ fontSize: "0.82rem", color: "var(--muted)", display: "flex", alignItems: "center", gap: 6 }}>
                  <span>Sản phẩm:</span>
                  <Link
                    href={`/products/${review.productSlug}`}
                    target="_blank"
                    style={{ fontWeight: 600, color: "var(--gold-strong)", textDecoration: "underline" }}
                  >
                    {review.productName} ↗
                  </Link>
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <div style={{ display: "flex", gap: 2 }}>
                    {[1, 2, 3, 4, 5].map((star) => (
                      <StarIcon key={star} filled={star <= review.rating} />
                    ))}
                  </div>
                  {review.title && (
                    <strong style={{ fontSize: "0.92rem", color: "var(--foreground)", marginLeft: 6 }}>
                      {review.title}
                    </strong>
                  )}
                </div>

                <p style={{ margin: 0, fontSize: "0.88rem", lineHeight: 1.6, color: "var(--foreground)", whiteSpace: "pre-line" }}>
                  {review.comment}
                </p>

                {/* Attached S3 Photos */}
                {review.imageUrls && review.imageUrls.length > 0 && (
                  <div style={{ display: "flex", flexWrap: "wrap", gap: 10, paddingTop: 6 }}>
                    {review.imageUrls.map((imgUrl, imgIdx) => (
                      <button
                        key={imgIdx}
                        type="button"
                        onClick={() => setLightboxImage(imgUrl)}
                        style={{
                          position: "relative",
                          width: 70,
                          height: 70,
                          borderRadius: 8,
                          overflow: "hidden",
                          border: "1px solid var(--line-strong)",
                          cursor: "pointer",
                          padding: 0,
                          background: "var(--surface-soft)"
                        }}
                      >
                        <Image src={imgUrl} alt="Review attachment" fill style={{ objectFit: "cover" }} />
                      </button>
                    ))}
                  </div>
                )}

                {/* Actions */}
                <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, paddingTop: 12, borderTop: "1px solid var(--line-soft)" }}>
                  {review.status !== "APPROVED" && (
                    <Button
                      type="button"
                      onClick={() => void handleUpdateStatus(review.id, "APPROVED")}
                      disabled={processingId === review.id}
                      style={{ fontSize: "0.8rem", padding: "6px 14px" }}
                    >
                      Duyệt hiển thị
                    </Button>
                  )}
                  {review.status !== "REJECTED" && (
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => void handleUpdateStatus(review.id, "REJECTED")}
                      disabled={processingId === review.id}
                      style={{ fontSize: "0.8rem", padding: "6px 14px" }}
                    >
                      Từ chối
                    </Button>
                  )}
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void handleDelete(review.id)}
                    disabled={processingId === review.id}
                    style={{ fontSize: "0.8rem", padding: "6px 14px", color: "#991b1b", borderColor: "rgba(153, 27, 27, 0.3)" }}
                  >
                    Xóa
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {reviewsPage.totalPages > 1 && (
          <div className="pagination" style={{ borderTop: "1px solid var(--line)", padding: "16px 20px" }}>
            <Button
              type="button"
              variant="secondary"
              onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
              disabled={currentPage === 0}
            >
              Previous
            </Button>
            <span>
              Page {currentPage + 1} of {reviewsPage.totalPages}
            </span>
            <Button
              type="button"
              variant="secondary"
              onClick={() => setCurrentPage((p) => Math.min(p + 1, reviewsPage.totalPages - 1))}
              disabled={currentPage >= reviewsPage.totalPages - 1}
            >
              Next
            </Button>
          </div>
        )}
      </div>

      {/* Lightbox Modal */}
      {lightboxImage && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 110,
            background: "rgba(20, 16, 12, 0.75)",
            backdropFilter: "blur(6px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 16
          }}
          onClick={() => setLightboxImage(null)}
        >
          <div style={{ position: "relative", maxWidth: 800, width: "100%", height: "80vh" }}>
            <Image
              src={lightboxImage}
              alt="Enlarged review photo"
              fill
              style={{ objectFit: "contain" }}
            />
            <button
              type="button"
              onClick={() => setLightboxImage(null)}
              style={{
                position: "absolute",
                top: 12,
                right: 12,
                background: "rgba(0,0,0,0.6)",
                color: "#ffffff",
                border: "none",
                borderRadius: "50%",
                width: 36,
                height: 36,
                fontSize: "1.2rem",
                cursor: "pointer"
              }}
            >
              ✕
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
