import type { AdminReview, PageResponse, Review, ReviewPayload, ReviewStatus, ReviewSummary } from "./types";
import { getAccessToken, refreshAndStoreSession } from "./authClient";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.API_BASE_URL ??
  "http://localhost:8080";

export class ReviewApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string
  ) {
    super(message);
    this.name = "ReviewApiError";
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  const headers = new Headers(options.headers ?? {});

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  if (!headers.has("Authorization")) {
    const token = getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      headers,
      credentials: "include"
    });
  } catch {
    throw new ReviewApiError("Review service is currently unreachable.");
  }

  if (response.status === 401) {
    try {
      const refreshed = await refreshAndStoreSession();
      headers.set("Authorization", `Bearer ${refreshed.accessToken}`);
      response = await fetch(`${apiBaseUrl}${path}`, {
        ...options,
        headers,
        credentials: "include"
      });
    } catch {
      window.dispatchEvent(new Event("luxora-auth-updated"));
    }
  }

  if (response.status === 204) {
    return null as T;
  }

  let body: { message?: string; error?: string; code?: string } | null = null;
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    try {
      body = (await response.json()) as { message?: string; error?: string; code?: string };
    } catch {
      // Ignore JSON parse errors
    }
  }

  if (!response.ok) {
    const message = body?.message ?? body?.error ?? "Review operation failed.";
    const code = body?.code;
    throw new ReviewApiError(message, response.status, code);
  }

  return body as T;
}

export async function fetchProductReviews(
  slug: string,
  page = 0,
  size = 10
): Promise<PageResponse<Review>> {
  return request<PageResponse<Review>>(
    `/api/v1/products/${encodeURIComponent(slug)}/reviews?page=${page}&size=${size}`,
    { cache: "no-store" }
  );
}

export async function fetchProductReviewSummary(slug: string): Promise<ReviewSummary> {
  return request<ReviewSummary>(
    `/api/v1/products/${encodeURIComponent(slug)}/reviews/summary`,
    { cache: "no-store" }
  );
}

export async function fetchMyReview(
  productId: string,
  token: string
): Promise<Review | null> {
  return request<Review | null>(`/api/v1/products/${productId}/reviews/me`, {
    headers: {
      Authorization: `Bearer ${token}`
    },
    cache: "no-store"
  });
}

export async function submitProductReview(
  productId: string,
  payload: ReviewPayload,
  token: string
): Promise<Review> {
  return request<Review>(`/api/v1/products/${productId}/reviews`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
}

export async function fetchAdminReviews(
  token: string,
  status?: ReviewStatus,
  page = 0,
  size = 15
): Promise<PageResponse<AdminReview>> {
  const statusParam = status ? `&status=${status}` : "";
  return request<PageResponse<AdminReview>>(
    `/api/v1/admin/reviews?page=${page}&size=${size}${statusParam}`,
    {
      headers: {
        Authorization: `Bearer ${token}`
      },
      cache: "no-store"
    }
  );
}

export async function updateAdminReviewStatus(
  token: string,
  reviewId: string,
  status: ReviewStatus
): Promise<AdminReview> {
  return request<AdminReview>(`/api/v1/admin/reviews/${reviewId}/status`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ status })
  });
}

export async function deleteAdminReview(token: string, reviewId: string): Promise<void> {
  await request<void>(`/api/v1/admin/reviews/${reviewId}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export async function requestPresignedUploadUrl(
  filename: string,
  contentType: string,
  folder = "reviews"
): Promise<{ uploadUrl: string; objectKey: string; publicUrl: string }> {
  return request<{ uploadUrl: string; objectKey: string; publicUrl: string }>(
    `/api/v1/media/presigned-upload-url`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        folder,
        filename,
        contentType
      })
    }
  );
}

export async function uploadImageToS3(uploadUrl: string, file: File): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: "PUT",
    headers: {
      "Content-Type": file.type
    },
    body: file
  });

  if (!response.ok) {
    throw new ReviewApiError("Failed to upload image to cloud storage.", response.status);
  }
}
