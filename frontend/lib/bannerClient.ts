import type { BannerResponse, BannerUpsertPayload, PageResponse } from "./types";
import { getAccessToken, refreshAndStoreSession } from "./authClient";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.API_BASE_URL ??
  "http://localhost:8080";

export class BannerApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string
  ) {
    super(message);
    this.name = "BannerApiError";
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
    throw new BannerApiError("Banner service is unreachable.");
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
    throw new BannerApiError(
      body?.message ?? body?.error ?? `Request failed with status ${response.status}`,
      response.status,
      body?.code
    );
  }

  return body as T;
}

export async function fetchActiveBanners(): Promise<BannerResponse[]> {
  try {
    return await request<BannerResponse[]>("/api/v1/banners");
  } catch {
    return [];
  }
}

export async function fetchAdminBanners(
  token?: string,
  page = 0,
  size = 20,
  active?: boolean,
  search?: string
): Promise<PageResponse<BannerResponse>> {
  const authToken = token || getAccessToken();
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  if (active !== undefined) {
    params.set("active", String(active));
  }
  if (search) {
    params.set("search", search);
  }

  return request<PageResponse<BannerResponse>>(`/api/v1/admin/banners?${params.toString()}`, {
    headers: authToken
      ? {
          Authorization: `Bearer ${authToken}`
        }
      : {}
  });
}

export async function createBanner(
  token: string | undefined,
  payload: BannerUpsertPayload
): Promise<BannerResponse> {
  const authToken = token || getAccessToken();
  return request<BannerResponse>("/api/v1/admin/banners", {
    method: "POST",
    headers: {
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
}

export async function updateBanner(
  token: string | undefined,
  id: string,
  payload: BannerUpsertPayload
): Promise<BannerResponse> {
  const authToken = token || getAccessToken();
  return request<BannerResponse>(`/api/v1/admin/banners/${id}`, {
    method: "PUT",
    headers: {
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
}

export async function toggleBannerStatus(
  token: string | undefined,
  id: string
): Promise<BannerResponse> {
  const authToken = token || getAccessToken();
  return request<BannerResponse>(`/api/v1/admin/banners/${id}/status`, {
    method: "PATCH",
    headers: authToken
      ? {
          Authorization: `Bearer ${authToken}`
        }
      : {}
  });
}

export async function deleteBanner(
  token: string | undefined,
  id: string
): Promise<void> {
  const authToken = token || getAccessToken();
  return request<void>(`/api/v1/admin/banners/${id}`, {
    method: "DELETE",
    headers: authToken
      ? {
          Authorization: `Bearer ${authToken}`
        }
      : {}
  });
}
