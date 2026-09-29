"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import type {
  AdminDashboard,
  AdminPromotion,
  AdminPromotionPayload,
  AdminImagePayload,
  AdminInventoryPayload,
  AdminOrderDetail,
  AdminOrderNotePayload,
  AdminOrderQuery,
  AdminOrderSummary,
  AdminShipmentPayload,
  AdminApproveReturnPayload,
  AdminReturnNotePayload,
  AdminReturnQuery,
  AdminReceiveReturnPayload,
  AdminRejectReturnPayload,
  MockRefundOutcome,
  OrderStatus,
  AdminProductDetail,
  AdminProductPayload,
  AdminProductSummary,
  AdminProductVariant,
  AdminSpecificationPayload,
  AdminVariantCreatePayload,
  AdminVariantUpdatePayload,
  PageResponse,
  ReturnRefundPayload,
  ReturnRequest,
  ReturnSummary
} from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class AdminClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "AdminClientError";
  }
}

export function getAdminPromotions(page = 0, size = 10, active?: boolean, search?: string) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (active !== undefined) params.set("active", String(active));
  if (search) params.set("search", search);
  return adminRequest<PageResponse<AdminPromotion>>(`/api/v1/admin/promotions?${params.toString()}`);
}
export function getAdminPromotion(id: string) { return adminRequest<AdminPromotion>(`/api/v1/admin/promotions/${encodeURIComponent(id)}`); }
export function createAdminPromotion(payload: AdminPromotionPayload) { return adminRequest<AdminPromotion>("/api/v1/admin/promotions", { method: "POST", body: payload }); }
export function updateAdminPromotion(id: string, payload: AdminPromotionPayload) { return adminRequest<AdminPromotion>(`/api/v1/admin/promotions/${encodeURIComponent(id)}`, { method: "PATCH", body: payload }); }
export function getAdminDashboard() {
  return adminRequest<AdminDashboard>("/api/v1/admin/dashboard");
}

export function getAdminProducts(page = 0, size = 10) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return adminRequest<PageResponse<AdminProductSummary>>(`/api/v1/admin/products?${params.toString()}`);
}

export function createAdminProduct(payload: AdminProductPayload) {
  return adminRequest<AdminProductDetail>("/api/v1/admin/products", { method: "POST", body: payload });
}

export function getAdminProduct(id: string) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(id)}`);
}

export function updateAdminProduct(id: string, payload: AdminProductPayload) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(id)}`, { method: "PATCH", body: payload });
}

export function createAdminVariant(productId: string, payload: AdminVariantCreatePayload) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(productId)}/variants`, { method: "POST", body: payload });
}

export function updateAdminVariant(id: string, payload: AdminVariantUpdatePayload) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/variants/${encodeURIComponent(id)}`, { method: "PATCH", body: payload });
}

export function updateAdminInventory(variantId: string, payload: AdminInventoryPayload) {
  return adminRequest<AdminProductVariant>(`/api/v1/admin/inventory/${encodeURIComponent(variantId)}`, { method: "PATCH", body: payload });
}

export function addAdminProductImage(productId: string, payload: AdminImagePayload) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(productId)}/images`, { method: "POST", body: payload });
}

export function removeAdminProductImage(productId: string, imageId: string) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(productId)}/images/${encodeURIComponent(imageId)}`, { method: "DELETE" });
}

export function addAdminProductSpecification(productId: string, payload: AdminSpecificationPayload) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(productId)}/specifications`, { method: "POST", body: payload });
}

export function removeAdminProductSpecification(productId: string, specId: string) {
  return adminRequest<AdminProductDetail>(`/api/v1/admin/products/${encodeURIComponent(productId)}/specifications/${encodeURIComponent(specId)}`, { method: "DELETE" });
}


export function getAdminReturns(query: AdminReturnQuery = {}) {
  const params = new URLSearchParams({
    page: String(query.page ?? 0),
    size: String(query.size ?? 10)
  });
  if (query.status) params.set("status", query.status);
  if (query.orderId) params.set("orderId", query.orderId);
  if (query.customerEmail) params.set("customerEmail", query.customerEmail);
  if (query.trackingNumber) params.set("trackingNumber", query.trackingNumber);
  if (query.dateFrom) params.set("dateFrom", query.dateFrom);
  if (query.dateTo) params.set("dateTo", query.dateTo);
  if (query.sort) params.set("sort", query.sort);
  return adminRequest<PageResponse<ReturnSummary>>(`/api/v1/admin/returns?${params.toString()}`);
}

export function getAdminReturn(id: string) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}`);
}

export function updateAdminReturnNote(id: string, payload: AdminReturnNotePayload) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/note`, { method: "PATCH", body: payload });
}

export function approveAdminReturn(id: string, payload: AdminApproveReturnPayload) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/approve`, { method: "POST", body: payload });
}

export function rejectAdminReturn(id: string, payload: AdminRejectReturnPayload) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/reject`, { method: "POST", body: payload });
}

export function receiveAdminReturn(id: string, payload: AdminReceiveReturnPayload) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/receive`, { method: "POST", body: payload });
}

export function generateAdminReturnShippingLabel(id: string) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/shipping-label`, { method: "POST" });
}

export function markAdminReturnReceived(id: string, payload: AdminReceiveReturnPayload) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/mark-received`, { method: "POST", body: payload });
}

export function refundAdminReturn(id: string, payload: ReturnRefundPayload) {
  return adminRequest<ReturnRequest>(`/api/v1/admin/returns/${encodeURIComponent(id)}/refund`, { method: "POST", body: payload });
}
export function getAdminOrders(query: AdminOrderQuery = {}) {
  const params = new URLSearchParams({
    page: String(query.page ?? 0),
    size: String(query.size ?? 10)
  });
  if (query.status) params.set("status", query.status);
  if (query.orderId) params.set("orderId", query.orderId);
  if (query.customerEmail) params.set("customerEmail", query.customerEmail);
  if (query.trackingNumber) params.set("trackingNumber", query.trackingNumber);
  if (query.dateFrom) params.set("dateFrom", query.dateFrom);
  if (query.dateTo) params.set("dateTo", query.dateTo);
  if (query.sort) params.set("sort", query.sort);
  return adminRequest<PageResponse<AdminOrderSummary>>(`/api/v1/admin/orders?${params.toString()}`);
}

export function getAdminOrder(id: string) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}`);
}

export function updateAdminOrderNote(id: string, payload: AdminOrderNotePayload) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}/note`, { method: "PATCH", body: payload });
}
export function cancelAdminOrder(id: string) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}/cancel`, { method: "POST" });
}

export function refundAndCancelAdminOrder(id: string, payload: { mockOutcome: MockRefundOutcome; reason?: string }) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}/refund-and-cancel`, { method: "POST", body: payload });
}

export function createAdminShipment(id: string, payload: AdminShipmentPayload) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}/shipment`, { method: "POST", body: payload });
}

export function updateAdminShipment(id: string, payload: AdminShipmentPayload) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}/shipment`, { method: "PATCH", body: payload });
}
export function updateAdminOrderStatus(id: string, status: OrderStatus) {
  return adminRequest<AdminOrderDetail>(`/api/v1/admin/orders/${encodeURIComponent(id)}/status`, { method: "PATCH", body: { status } });
}

type AdminRequestOptions = {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
};

async function adminRequest<T>(path: string, options: AdminRequestOptions = {}) {
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
    let message = "Admin request failed.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new AdminClientError(message, response.status);
  }

  return response.json() as Promise<T>;
}

function send(path: string, accessToken: string | null, options: AdminRequestOptions) {
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


