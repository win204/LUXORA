import type { FlashSale, CreateFlashSalePayload } from "./types";
import { getAccessToken, refreshAndStoreSession } from "./authClient";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.API_BASE_URL ??
  "http://localhost:8080";

export class FlashSaleApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string
  ) {
    super(message);
    this.name = "FlashSaleApiError";
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
    throw new FlashSaleApiError("Flash sale service is unreachable.");
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
    const message = body?.message ?? body?.error ?? "Flash sale operation failed.";
    const code = body?.code;
    throw new FlashSaleApiError(message, response.status, code);
  }

  return body as T;
}

export async function getActiveFlashSale(): Promise<FlashSale | null> {
  return request<FlashSale | null>("/api/v1/flash-sales/active", {
    cache: "no-store"
  });
}

export async function getFlashSaleById(id: string): Promise<FlashSale> {
  return request<FlashSale>(`/api/v1/flash-sales/${id}`);
}

export async function getAdminFlashSales(token: string): Promise<FlashSale[]> {
  return request<FlashSale[]>("/api/v1/admin/flash-sales", {
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export async function createAdminFlashSale(
  payload: CreateFlashSalePayload,
  token: string
): Promise<FlashSale> {
  return request<FlashSale>("/api/v1/admin/flash-sales", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
}

export async function cancelAdminFlashSale(
  id: string,
  token: string
): Promise<FlashSale> {
  return request<FlashSale>(`/api/v1/admin/flash-sales/${id}/cancel`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}
