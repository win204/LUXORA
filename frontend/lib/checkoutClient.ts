"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import type { CheckoutAddress, CheckoutPreview } from "./types";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class CheckoutClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "CheckoutClientError";
  }
}

export async function previewCheckout(address: CheckoutAddress, promotionCode?: string) {
  let accessToken = getAccessToken();
  let response = await sendPreviewRequest({ ...address, promotionCode: promotionCode?.trim() || undefined }, accessToken);

  if (response.status === 401 && accessToken) {
    try {
      const refreshed = await refreshAndStoreSession();
      accessToken = refreshed.accessToken;
      response = await sendPreviewRequest({ ...address, promotionCode: promotionCode?.trim() || undefined }, accessToken);
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

function sendPreviewRequest(address: CheckoutAddress, accessToken: string | null) {
  const headers = new Headers();
  headers.set("Accept", "application/json");
  headers.set("Content-Type", "application/json");
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return fetch(`${apiBaseUrl}/api/v1/checkout/preview`, {
    method: "POST",
    credentials: "include",
    headers,
    body: JSON.stringify(address)
  });
}
