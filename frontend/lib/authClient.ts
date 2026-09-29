import type { AuthResponse, CurrentUser } from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

let currentAccessToken: string | null = null;
let refreshInFlight: Promise<AuthResponse> | null = null;

export class AuthClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "AuthClientError";
  }
}

export type AuthSession = {
  accessToken: string;
  user: CurrentUser;
};

type AuthPayload = {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
};

export function getAccessToken() {
  return currentAccessToken;
}

export function storeSession(response: AuthResponse) {
  currentAccessToken = response.accessToken;
  window.dispatchEvent(new Event("luxora-auth-updated"));
  window.dispatchEvent(new Event("luxora-cart-updated"));
  return toSession(response);
}

export function clearStoredSession() {
  currentAccessToken = null;
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("luxora-auth-updated"));
    window.dispatchEvent(new Event("luxora-cart-updated"));
  }
}

export async function login(payload: AuthPayload, anonymousCartId: string | null) {
  const headers = jsonHeaders();
  if (anonymousCartId) {
    headers.set("X-Cart-Id", anonymousCartId);
  }

  return authRequest("/api/v1/auth/login", {
    method: "POST",
    headers,
    credentials: "include",
    body: JSON.stringify({
      email: payload.email,
      password: payload.password
    })
  });
}

export async function register(payload: AuthPayload) {
  return authRequest("/api/v1/auth/register", {
    method: "POST",
    headers: jsonHeaders(),
    credentials: "include",
    body: JSON.stringify(payload)
  });
}

export async function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = authRequest("/api/v1/auth/refresh", {
      method: "POST",
      headers: acceptJsonHeaders(),
      credentials: "include"
    }).finally(() => {
      refreshInFlight = null;
    });
  }

  return refreshInFlight;
}

export async function logoutSession(accessToken: string | null) {
  try {
    await fetch(`${apiBaseUrl}/api/v1/auth/logout`, {
      method: "POST",
      headers: accessToken ? authorizedJsonHeaders(accessToken) : acceptJsonHeaders(),
      credentials: "include"
    });
  } finally {
    clearStoredSession();
  }
}

export async function getCurrentUser(accessToken: string) {
  const response = await fetch(`${apiBaseUrl}/api/v1/users/me`, {
    headers: authorizedJsonHeaders(accessToken),
    credentials: "include"
  });

  if (!response.ok) {
    throw new AuthClientError("Session expired. Please sign in again.", response.status);
  }

  return response.json() as Promise<CurrentUser>;
}

export async function updateCurrentUserProfile(
  accessToken: string,
  payload: { firstName: string; lastName: string }
) {
  const response = await fetch(`${apiBaseUrl}/api/v1/users/me`, {
    method: "PATCH",
    headers: authorizedJsonHeaders(accessToken),
    credentials: "include",
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    let message = "Unable to update profile.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new AuthClientError(message, response.status);
  }

  return response.json() as Promise<CurrentUser>;
}

export async function changeCurrentUserPassword(
  accessToken: string,
  payload: { currentPassword: string; newPassword: string; confirmPassword: string }
) {
  const response = await fetch(`${apiBaseUrl}/api/v1/users/me/password`, {
    method: "POST",
    headers: authorizedJsonHeaders(accessToken),
    credentials: "include",
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    let message = "Unable to change password.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new AuthClientError(message, response.status);
  }

  return response.json() as Promise<{ message: string }>;
}
export async function refreshAndStoreSession() {
  const response = await refreshSession();
  return storeSession(response);
}

async function authRequest(path: string, init: RequestInit) {
  const response = await fetch(`${apiBaseUrl}${path}`, init);

  if (!response.ok) {
    let message = "Authentication request failed.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new AuthClientError(message, response.status);
  }

  return response.json() as Promise<AuthResponse>;
}

function acceptJsonHeaders() {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  return headers;
}

function jsonHeaders() {
  const headers = acceptJsonHeaders();
  headers.set("Content-Type", "application/json");
  return headers;
}

function authorizedJsonHeaders(accessToken: string) {
  const headers = jsonHeaders();
  headers.set("Authorization", `Bearer ${accessToken}`);
  return headers;
}

function toSession(response: AuthResponse): AuthSession {
  return {
    accessToken: response.accessToken,
    user: response.user
  };
}

