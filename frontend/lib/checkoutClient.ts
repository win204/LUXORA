"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import { getStoredCartId } from "./cartClient";
import type { CheckoutAddress, CheckoutPreview } from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class CheckoutClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "CheckoutClientError";
  }
}

export async function previewCheckout(
  address: CheckoutAddress,
  promotionCode?: string,
  redeemPoints?: number,
  autoApplyBestVoucher?: boolean
) {
  let accessToken = getAccessToken();
  const payload = {
    ...address,
    promotionCode: promotionCode?.trim() || undefined,
    redeemPoints: redeemPoints != null && redeemPoints > 0 ? redeemPoints : undefined,
    autoApplyBestVoucher: autoApplyBestVoucher || undefined
  };
  let response = await sendPreviewRequest(payload, accessToken);

  if (response.status === 401) {
    try {
      const refreshed = await refreshAndStoreSession();
      accessToken = refreshed.accessToken;
      response = await sendPreviewRequest(payload, accessToken);
    } catch {
      window.dispatchEvent(new Event("luxora-auth-updated"));
    }
  }

  if (!response.ok) {
    let message = "Unable to preview checkout.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new CheckoutClientError(message, response.status);
  }

  return response.json() as Promise<CheckoutPreview>;
}

function sendPreviewRequest(payload: Record<string, unknown>, accessToken: string | null) {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  headers.set("Content-Type", "application/json");
  const cartId = getStoredCartId();
  if (cartId) {
    headers.set("X-Cart-Id", cartId);
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return fetch(`${apiBaseUrl}/api/v1/checkout/preview`, {
    method: "POST",
    credentials: "include",
    headers,
    body: JSON.stringify(payload)
  });
}
