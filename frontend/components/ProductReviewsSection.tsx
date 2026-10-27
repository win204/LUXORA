"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/Button";
import {
  fetchMyReview,
  fetchProductReviews,
  fetchProductReviewSummary,
  requestPresignedUploadUrl,
  submitProductReview,
  uploadImageToS3
} from "@/lib/reviewClient";
import type { PageResponse, Review, ReviewSummary } from "@/lib/types";

function StarIcon({ filled, className = "w-5 h-5" }: { filled: boolean; className?: string }) {
  return (
    <svg
      className={`${className} ${filled ? "text-amber-400 fill-amber-400" : "text-neutral-300 dark:text-neutral-600 fill-transparent"}`}
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

export function ProductReviewsSection({
  productId,
  productSlug,
  productName
}: {
  productId: string;
  productSlug: string;
  productName: string;
}) {
  const { user, accessToken } = useAuth();

  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [reviewsPage, setReviewsPage] = useState<PageResponse<Review>>({
    content: [],
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 0
  });
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [showForm, setShowForm] = useState(false);
  const [formRating, setFormRating] = useState(5);
  const [hoverRating, setHoverRating] = useState(0);
  const [formTitle, setFormTitle] = useState("");
  const [formComment, setFormComment] = useState("");
  const [formImages, setFormImages] = useState<string[]>([]);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  // Lightbox modal state
  const [lightboxImage, setLightboxImage] = useState<string | null>(null);

  const refreshReviews = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [sum, reviews] = await Promise.all([
        fetchProductReviewSummary(productSlug),
        fetchProductReviews(productSlug, currentPage, 5)
      ]);
      setSummary(sum);
      setReviewsPage(reviews);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to load reviews.");
    } finally {
      setLoading(false);
    }
  }, [productSlug, currentPage]);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        setLoading(true);
        setError(null);
        const [sum, reviews] = await Promise.all([
          fetchProductReviewSummary(productSlug),
          fetchProductReviews(productSlug, currentPage, 5)
        ]);
        if (active) {
          setSummary(sum);
          setReviewsPage(reviews);
        }
      } catch (caught) {
        if (active) {
          setError(caught instanceof Error ? caught.message : "Unable to load reviews.");
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
  }, [productSlug, currentPage]);

  // Load existing user review if logged in
  useEffect(() => {
    let active = true;
    if (!accessToken) return;

    fetchMyReview(productId, accessToken)
      .then((myReview) => {
        if (active && myReview) {
          setFormRating(myReview.rating);
          setFormTitle(myReview.title ?? "");
          setFormComment(myReview.comment);
          setFormImages(myReview.imageUrls ?? []);
        }
      })
      .catch(() => {
        // Non-critical, ignore
      });

    return () => {
      active = false;
    };
  }, [productId, accessToken]);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    if (formImages.length + files.length > 5) {
      setFormError("Maximum 5 photos allowed per review.");
      return;
    }

    try {
      setUploadingImage(true);
      setFormError(null);

      const uploadedUrls: string[] = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const presigned = await requestPresignedUploadUrl(file.name, file.type, "reviews");
        await uploadImageToS3(presigned.uploadUrl, file);
        uploadedUrls.push(presigned.publicUrl);
      }

      setFormImages((prev) => [...prev, ...uploadedUrls]);
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Failed to upload image.");
    } finally {
      setUploadingImage(false);
      e.target.value = "";
    }
  };

  const handleRemoveImage = (indexToRemove: number) => {
    setFormImages((prev) => prev.filter((_, idx) => idx !== indexToRemove));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessToken) return;

    if (!formComment.trim()) {
      setFormError("Please write a review comment.");
      return;
    }

    try {
      setSubmitting(true);
      setFormError(null);
      setFormSuccess(null);

      await submitProductReview(
        productId,
        {
          rating: formRating,
          title: formTitle.trim() || undefined,
          comment: formComment.trim(),
          imageUrls: formImages
        },
        accessToken
      );

      setFormSuccess("Thank you! Your review has been submitted successfully.");
      setShowForm(false);
      await refreshReviews();
    } catch (caught) {
      setFormError(caught instanceof Error ? caught.message : "Failed to submit review.");
    } finally {
      setSubmitting(false);
    }
  };

  const totalReviews = summary?.totalReviews ?? 0;
  const avgRating = summary?.averageRating ?? 0;

  return (
    <section className="product-reviews-section mt-16 pt-12 border-t border-neutral-200 dark:border-neutral-800" aria-label="Customer Reviews">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-8">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
            Customer Reviews & Ratings
          </h2>
          <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            Real feedback from verified purchasers of {productName}
          </p>
        </div>

        {user ? (
          <Button
            type="button"
            variant={showForm ? "secondary" : "primary"}
            onClick={() => setShowForm(!showForm)}
          >
            {showForm ? "Cancel Review" : "Write a Review"}
          </Button>
        ) : (
          <Link
            href="/login"
            className="inline-flex items-center justify-center px-4 py-2 text-sm font-medium rounded-lg border border-neutral-300 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-800 dark:text-neutral-200 hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
          >
            Sign in to review
          </Link>
        )}
      </div>

      {/* Review Summary Breakdown */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 p-6 rounded-2xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200/80 dark:border-neutral-800/80 mb-10">
          <div className="flex flex-col items-center justify-center text-center p-4 border-b md:border-b-0 md:border-r border-neutral-200 dark:border-neutral-800">
            <span className="text-5xl font-black tracking-tight text-neutral-900 dark:text-neutral-100">
              {avgRating.toFixed(1)}
            </span>
            <div className="flex items-center gap-1 my-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <StarIcon key={star} filled={star <= Math.round(avgRating)} className="w-5 h-5" />
              ))}
            </div>
            <span className="text-sm text-neutral-500 dark:text-neutral-400">
              Based on {totalReviews} {totalReviews === 1 ? "review" : "reviews"}
            </span>
          </div>

          <div className="md:col-span-2 flex flex-col justify-center space-y-2.5">
            {[5, 4, 3, 2, 1].map((stars) => {
              const count = summary.ratingDistribution[stars] ?? 0;
              const percentage = totalReviews > 0 ? (count / totalReviews) * 100 : 0;
              return (
                <div key={stars} className="flex items-center gap-3 text-sm">
                  <span className="w-12 text-neutral-600 dark:text-neutral-400 font-medium flex items-center gap-1">
                    {stars} <StarIcon filled={true} className="w-3.5 h-3.5 text-amber-400" />
                  </span>
                  <div className="flex-1 h-2.5 rounded-full bg-neutral-200 dark:bg-neutral-800 overflow-hidden">
                    <div
                      className="h-full bg-amber-400 rounded-full transition-all duration-500"
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                  <span className="w-10 text-right text-xs text-neutral-500 dark:text-neutral-400">
                    {count}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Review Submission Form */}
      {showForm && user && (
        <form onSubmit={handleSubmit} className="p-6 md:p-8 rounded-2xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 shadow-sm mb-12 space-y-5">
          <h3 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">
            Write your review
          </h3>

          {formError && (
            <div className="p-3 text-sm rounded-lg bg-red-50 text-red-600 dark:bg-red-950/50 dark:text-red-400 border border-red-200 dark:border-red-900">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
              Overall Rating *
            </label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  type="button"
                  key={star}
                  onClick={() => setFormRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  className="p-1 focus:outline-none transition-transform hover:scale-110"
                  aria-label={`${star} star`}
                >
                  <StarIcon
                    filled={star <= (hoverRating || formRating)}
                    className="w-7 h-7 cursor-pointer"
                  />
                </button>
              ))}
              <span className="ml-2 text-sm text-neutral-500 font-medium">
                {formRating} of 5 stars
              </span>
            </div>
          </div>

          <div>
            <label htmlFor="review-title" className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Review Title (optional)
            </label>
            <input
              id="review-title"
              type="text"
              maxLength={200}
              value={formTitle}
              onChange={(e) => setFormTitle(e.target.value)}
              placeholder="e.g. Excellent build quality, highly recommend!"
              className="w-full px-4 py-2.5 rounded-lg border border-neutral-300 dark:border-neutral-700 bg-transparent text-sm focus:ring-2 focus:ring-neutral-900 dark:focus:ring-neutral-100 focus:outline-none"
            />
          </div>

          <div>
            <label htmlFor="review-comment" className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Review Comment *
            </label>
            <textarea
              id="review-comment"
              rows={4}
              maxLength={2000}
              value={formComment}
              onChange={(e) => setFormComment(e.target.value)}
              placeholder="Share details about your experience with this product..."
              required
              className="w-full px-4 py-2.5 rounded-lg border border-neutral-300 dark:border-neutral-700 bg-transparent text-sm focus:ring-2 focus:ring-neutral-900 dark:focus:ring-neutral-100 focus:outline-none"
            />
          </div>

          {/* Photo upload with AWS S3 */}
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
              Attach Photos (AWS S3 Cloud Storage)
            </label>
            <div className="flex flex-wrap items-center gap-3">
              {formImages.map((url, idx) => (
                <div key={idx} className="relative group w-20 h-20 rounded-lg overflow-hidden border border-neutral-300 dark:border-neutral-700">
                  <Image src={url} alt={`Upload ${idx + 1}`} fill className="object-cover" />
                  <button
                    type="button"
                    onClick={() => handleRemoveImage(idx)}
                    className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex items-center justify-center text-white transition-opacity text-xs font-semibold"
                  >
                    Delete
                  </button>
                </div>
              ))}

              {formImages.length < 5 && (
                <label className={`w-20 h-20 rounded-lg border-2 border-dashed border-neutral-300 dark:border-neutral-700 flex flex-col items-center justify-center cursor-pointer hover:border-neutral-500 transition-colors ${uploadingImage ? "opacity-50 pointer-events-none" : ""}`}>
                  <span className="text-xl leading-none text-neutral-400">+</span>
                  <span className="text-[10px] text-neutral-500 mt-1">
                    {uploadingImage ? "Uploading..." : "Add photo"}
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={handleImageUpload}
                    className="hidden"
                  />
                </label>
              )}
            </div>
            <p className="text-xs text-neutral-400 mt-1.5">
              Up to 5 images (JPG, PNG, WebP). Directly uploaded to AWS S3.
            </p>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setShowForm(false)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={submitting || uploadingImage}
            >
              {submitting ? "Submitting..." : "Submit Review"}
            </Button>
          </div>
        </form>
      )}

      {formSuccess && (
        <div className="p-4 mb-8 text-sm rounded-xl bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
          {formSuccess}
        </div>
      )}

      {/* Review List */}
      <div className="space-y-6">
        {loading && reviewsPage.content.length === 0 ? (
          <div className="p-8 text-center text-sm text-neutral-500" aria-busy="true">
            Loading reviews...
          </div>
        ) : error ? (
          <div className="p-4 text-sm text-red-600 bg-red-50 dark:bg-red-950/50 rounded-xl border border-red-200 dark:border-red-900">
            {error}
          </div>
        ) : reviewsPage.content.length > 0 ? (
          <>
            {reviewsPage.content.map((review) => (
              <article
                key={review.id}
                className="p-6 rounded-2xl border border-neutral-200 dark:border-neutral-800 bg-white dark:bg-neutral-950 shadow-xs space-y-3"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center font-bold text-sm text-neutral-700 dark:text-neutral-300">
                      {review.userName ? review.userName.charAt(0).toUpperCase() : "U"}
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                        {review.userName}
                      </h4>
                      {review.verifiedPurchase && (
                        <span className="inline-flex items-center text-xs font-medium text-emerald-600 dark:text-emerald-400 mt-0.5">
                          ✓ Verified Purchase
                        </span>
                      )}
                    </div>
                  </div>

                  <span className="text-xs text-neutral-400">
                    {new Date(review.createdAt).toLocaleDateString("en-US", {
                      year: "numeric",
                      month: "short",
                      day: "numeric"
                    })}
                  </span>
                </div>

                <div className="flex items-center gap-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <StarIcon key={star} filled={star <= review.rating} className="w-4 h-4" />
                  ))}
                  {review.title && (
                    <span className="ml-2 text-sm font-medium text-neutral-900 dark:text-neutral-100">
                      {review.title}
                    </span>
                  )}
                </div>

                <p className="text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed whitespace-pre-line">
                  {review.comment}
                </p>

                {review.imageUrls && review.imageUrls.length > 0 && (
                  <div className="flex flex-wrap gap-2 pt-2">
                    {review.imageUrls.map((imgUrl, imgIdx) => (
                      <button
                        key={imgIdx}
                        type="button"
                        onClick={() => setLightboxImage(imgUrl)}
                        className="relative w-16 h-16 rounded-lg overflow-hidden border border-neutral-200 dark:border-neutral-800 hover:opacity-90 transition-opacity focus:outline-none"
                      >
                        <Image src={imgUrl} alt="Review attachment" fill className="object-cover" />
                      </button>
                    ))}
                  </div>
                )}
              </article>
            ))}

            {/* Pagination */}
            {reviewsPage.totalPages > 1 && (
              <div className="flex items-center justify-between pt-4">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
                  disabled={currentPage === 0}
                >
                  Previous
                </Button>
                <span className="text-xs text-neutral-500">
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
          </>
        ) : (
          <div className="p-8 text-center rounded-2xl border border-dashed border-neutral-200 dark:border-neutral-800 text-sm text-neutral-500">
            No customer reviews yet. Be the first to share your thoughts!
          </div>
        )}
      </div>

      {/* Lightbox Zoom Modal */}
      {lightboxImage && (
        <div
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4 backdrop-blur-xs"
          onClick={() => setLightboxImage(null)}
        >
          <div className="relative max-w-3xl max-h-[90vh] w-full h-[80vh]">
            <Image
              src={lightboxImage}
              alt="Enlarged review photo"
              fill
              className="object-contain"
            />
            <button
              type="button"
              onClick={() => setLightboxImage(null)}
              className="absolute top-4 right-4 bg-black/60 text-white rounded-full p-2 hover:bg-black transition-colors"
            >
              ✕
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
