"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import type { CreateReturnPayload, PageResponse, ReturnRequest, ReturnSummary } from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ReturnClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "ReturnClientError";
  }
}

export function createReturn(orderId: string, payload: CreateReturnPayload) {
  return returnRequest<ReturnRequest>(`/api/v1/orders/${encodeURIComponent(orderId)}/returns`, { method: "POST", body: payload });
}

export function getReturns(page = 0, size = 10) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return returnRequest<PageResponse<ReturnSummary>>(`/api/v1/returns?${params.toString()}`);
}

export function getReturn(id: string) {
  return returnRequest<ReturnRequest>(`/api/v1/returns/${encodeURIComponent(id)}`);
}

export function cancelReturn(id: string) {
  return returnRequest<ReturnRequest>(`/api/v1/returns/${encodeURIComponent(id)}/cancel`, { method: "POST" });
}

export function markReturnShipped(id: string) {
  return returnRequest<ReturnRequest>(`/api/v1/returns/${encodeURIComponent(id)}/mark-shipped`, { method: "POST" });
}

type ReturnRequestOptions = {
  method?: "GET" | "POST";
  body?: unknown;
};

async function returnRequest<T>(path: string, options: ReturnRequestOptions = {}) {
  let accessToken = getAccessToken();
  let response = await send(path, accessToken, options);

  if (response.status === 401 && accessToken) {
    try {
      const refreshed = await refreshAndStoreSession();
      accessToken = refreshed.accessToken;
      response = await send(path, accessToken, options);
    } catch {
      window.dispatchEvent(new Event("luxora-auth-updated"));
    }
  }

  if (!response.ok) {
    let message = "Return request failed.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new ReturnClientError(message, response.status);
  }

  return response.json() as Promise<T>;
}

function send(path: string, accessToken: string | null, options: ReturnRequestOptions) {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return fetch(`${apiBaseUrl}${path}`, {
    method: options.method ?? "GET",
    credentials: "include",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });
}
