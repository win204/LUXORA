"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import type { CheckoutAddress, MockPaymentOutcome, Order, OrderSummary, PageResponse, Payment } from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class OrderClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "OrderClientError";
  }
}

export async function createOrder(address: CheckoutAddress) {
  const order = await orderRequest<Order>("/api/v1/orders", {
    method: "POST",
    headers: jsonHeaders(),
    body: JSON.stringify(address)
  });
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

export async function getOrder(orderId: string) {
  return orderRequest<Order>(`/api/v1/orders/${encodeURIComponent(orderId)}`, {
    method: "GET",
    headers: acceptJsonHeaders()
  });
}

export async function cancelOrder(orderId: string) {
  return orderRequest<Order>(`/api/v1/orders/${encodeURIComponent(orderId)}/cancel`, {
    method: "POST",
    headers: acceptJsonHeaders()
  });
}

export async function createMockPayment(orderId: string, mockOutcome: MockPaymentOutcome) {
  return orderRequest<Payment>(`/api/v1/orders/${encodeURIComponent(orderId)}/payments`, {
    method: "POST",
    headers: jsonHeaders(),
    body: JSON.stringify({ mockOutcome })
  });
}

export async function getLatestPayment(orderId: string) {
  return orderRequest<Payment>(`/api/v1/orders/${encodeURIComponent(orderId)}/payments/latest`, {
    method: "GET",
    headers: acceptJsonHeaders()
  });
}

async function orderRequest<T>(path: string, init: RequestInit) {
  let accessToken = getAccessToken();
  let response = await sendOrderRequest(path, init, accessToken);

  if (response.status === 401 && accessToken) {
    try {
      const refreshed = await refreshAndStoreSession();
      accessToken = refreshed.accessToken;
      response = await sendOrderRequest(path, init, accessToken);
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

function sendOrderRequest(path: string, init: RequestInit, accessToken: string | null) {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
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