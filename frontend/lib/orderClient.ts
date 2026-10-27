"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import { getStoredCartId } from "./cartClient";
import type { CheckoutAddress, CreatePaymentRequest, MockPaymentOutcome, Order, OrderSummary, PageResponse, Payment } from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const guestTokensKey = "luxora.guestOrderTokens";

export class OrderClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "OrderClientError";
  }
}

export function getStoredGuestToken(orderId: string): string | null {
  if (typeof window === "undefined") return null;
  try {
    const stored = window.sessionStorage.getItem(guestTokensKey);
    if (!stored) return null;
    const parsed = JSON.parse(stored) as Record<string, string>;
    return parsed[orderId] ?? null;
  } catch {
    return null;
  }
}

export function storeGuestToken(orderId: string, token: string) {
  if (typeof window === "undefined") return;
  try {
    const stored = window.sessionStorage.getItem(guestTokensKey);
    const parsed = (stored ? JSON.parse(stored) : {}) as Record<string, string>;
    parsed[orderId] = token;
    window.sessionStorage.setItem(guestTokensKey, JSON.stringify(parsed));
  } catch {
  }
}

export async function createOrder(address: CheckoutAddress) {
  const order = await orderRequest<Order>("/api/v1/orders", {
    method: "POST",
    headers: jsonHeaders(),
    body: JSON.stringify(address)
  });
  if (order.guestAccessToken) {
    storeGuestToken(order.id, order.guestAccessToken);
  }
  window.dispatchEvent(new Event("luxora-cart-updated"));
  return order;
}

export async function getOrders(page = 0, size = 10) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  return orderRequest<PageResponse<OrderSummary>>(`/api/v1/orders?${params.toString()}`, {
    method: "GET",
    headers: acceptJsonHeaders()
  });
}

export async function getOrder(orderId: string, token?: string) {
  const guestToken = token ?? getStoredGuestToken(orderId);
  return orderRequest<Order>(`/api/v1/orders/${encodeURIComponent(orderId)}`, {
    method: "GET",
    headers: acceptJsonHeaders()
  }, guestToken);
}

export async function cancelOrder(orderId: string, token?: string) {
  const guestToken = token ?? getStoredGuestToken(orderId);
  return orderRequest<Order>(`/api/v1/orders/${encodeURIComponent(orderId)}/cancel`, {
    method: "POST",
    headers: acceptJsonHeaders()
  }, guestToken);
}

export async function createPayment(orderId: string, request: CreatePaymentRequest, token?: string) {
  const guestToken = token ?? getStoredGuestToken(orderId);
  return orderRequest<Payment>(`/api/v1/orders/${encodeURIComponent(orderId)}/payments`, {
    method: "POST",
    headers: jsonHeaders(),
    body: JSON.stringify(request)
  }, guestToken);
}

export async function createMockPayment(orderId: string, mockOutcome: MockPaymentOutcome, token?: string) {
  return createPayment(orderId, { provider: "MOCK", mockOutcome }, token);
}

export async function createVnPayPayment(orderId: string, bankCode?: string, token?: string) {
  const returnUrl = typeof window !== "undefined" ? `${window.location.origin}/orders/${orderId}` : undefined;
  return createPayment(orderId, { provider: "VNPAY", bankCode, returnUrl }, token);
}

export async function getLatestPayment(orderId: string, token?: string) {
  const guestToken = token ?? getStoredGuestToken(orderId);
  return orderRequest<Payment>(`/api/v1/orders/${encodeURIComponent(orderId)}/payments/latest`, {
    method: "GET",
    headers: acceptJsonHeaders()
  }, guestToken);
}

async function orderRequest<T>(path: string, init: RequestInit, guestToken?: string | null) {
  let accessToken = getAccessToken();
  let response = await sendOrderRequest(path, init, accessToken, guestToken);

  if (response.status === 401) {
    try {
      const refreshed = await refreshAndStoreSession();
      accessToken = refreshed.accessToken;
      response = await sendOrderRequest(path, init, accessToken, guestToken);
    } catch {
      window.dispatchEvent(new Event("luxora-auth-updated"));
    }
  }

  if (!response.ok) {
    let message = "Order request failed.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new OrderClientError(message, response.status);
  }

  return response.json() as Promise<T>;
}

function sendOrderRequest(path: string, init: RequestInit, accessToken: string | null, guestToken?: string | null) {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  const cartId = getStoredCartId();
  if (cartId) {
    headers.set("X-Cart-Id", cartId);
  }
  if (guestToken) {
    headers.set("X-Guest-Access-Token", guestToken);
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return fetch(`${apiBaseUrl}${path}`, {
    ...init,
    credentials: "include",
    headers
  });
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