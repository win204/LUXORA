import type { AdminUserResponse, AdminCreateUserPayload } from "./types";
import { getAccessToken, refreshAndStoreSession } from "./authClient";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.API_BASE_URL ??
  "http://localhost:8080";

export class AdminUserApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string
  ) {
    super(message);
    this.name = "AdminUserApiError";
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
    throw new AdminUserApiError("Dịch vụ tài khoản người dùng không khả dụng.");
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
    throw new AdminUserApiError(
      body?.message ?? body?.error ?? `Yêu cầu thất bại với mã lỗi ${response.status}`,
      response.status,
      body?.code
    );
  }

  return body as T;
}

export async function fetchAdminUsers(): Promise<AdminUserResponse[]> {
  return request<AdminUserResponse[]>("/api/v1/admin/users");
}

export async function createAdminUser(payload: AdminCreateUserPayload): Promise<AdminUserResponse> {
  return request<AdminUserResponse>("/api/v1/admin/users", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
}

export async function updateAdminUserStatus(id: string, enabled: boolean): Promise<AdminUserResponse> {
  return request<AdminUserResponse>(`/api/v1/admin/users/${id}/status`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ enabled })
  });
}

export async function deleteAdminUser(id: string): Promise<void> {
  return request<void>(`/api/v1/admin/users/${id}`, {
    method: "DELETE"
  });
}
