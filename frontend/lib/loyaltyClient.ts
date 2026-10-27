import type { LoyaltyAccount, LoyaltyTransaction, PageResponse } from "./types";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.API_BASE_URL ??
  "http://localhost:8080";

export class LoyaltyApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string
  ) {
    super(message);
    this.name = "LoyaltyApiError";
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  const headers = new Headers(options.headers ?? {});

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      headers
    });
  } catch {
    throw new LoyaltyApiError("Loyalty service is currently unreachable.");
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
    const message = body?.message ?? body?.error ?? "Loyalty operation failed.";
    const code = body?.code;
    throw new LoyaltyApiError(message, response.status, code);
  }

  return body as T;
}

export async function fetchMyLoyaltyAccount(token: string): Promise<LoyaltyAccount> {
  return request<LoyaltyAccount>("/api/v1/loyalty/me", {
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export async function fetchMyLoyaltyTransactions(
  token: string,
  page = 0,
  size = 20
): Promise<PageResponse<LoyaltyTransaction>> {
  return request<PageResponse<LoyaltyTransaction>>(
    `/api/v1/loyalty/transactions?page=${page}&size=${size}`,
    {
      headers: {
        Authorization: `Bearer ${token}`
      }
    }
  );
}

export async function fetchAdminLoyaltyAccount(
  userId: string,
  token: string
): Promise<LoyaltyAccount> {
  return request<LoyaltyAccount>(`/api/v1/admin/loyalty/users/${userId}`, {
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
}

export async function adjustLoyaltyPoints(
  userId: string,
  points: number,
  reason: string,
  token: string
): Promise<LoyaltyAccount> {
  return request<LoyaltyAccount>(`/api/v1/admin/loyalty/users/${userId}/adjust`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ points, reason })
  });
}
