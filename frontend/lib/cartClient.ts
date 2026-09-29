"use client";

import { getAccessToken, refreshAndStoreSession } from "./authClient";
import type { Cart } from "./types";

const cartIdKey = "luxora.cartId";
const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class CartClientError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "CartClientError";
  }
}

export function getStoredCartId() {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage.getItem(cartIdKey);
}

function storeCartId(cartId: string) {
  window.localStorage.setItem(cartIdKey, cartId);
  window.dispatchEvent(new Event("luxora-cart-updated"));
}

async function cartRequest(path: string, init: RequestInit = {}) {
  let accessToken = getAccessToken();
  let response = await sendCartRequest(path, init, accessToken);

  if (response.status === 401 && accessToken) {
    try {
      const refreshed = await refreshAndStoreSession();
      accessToken = refreshed.accessToken;
      response = await sendCartRequest(path, init, accessToken);
    } catch {
      window.dispatchEvent(new Event("luxora-auth-updated"));
    }
  }

  if (!response.ok) {
    let message = "Cart request failed.";
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
    }
    throw new CartClientError(message);
  }

  const cart = (await response.json()) as Cart;
  if (cart.cartId && !accessToken) {
    storeCartId(cart.cartId);
  } else {
    window.dispatchEvent(new Event("luxora-cart-updated"));
  }
  return cart;
}

function sendCartRequest(path: string, init: RequestInit, accessToken: string | null) {
  const cartId = getStoredCartId();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (cartId) {
    headers.set("X-Cart-Id", cartId);
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

export function getCart() {
  return cartRequest("/api/v1/cart");
}

export function addCartItem(variantId: string, quantity = 1) {
  const headers = new Headers();
  headers.set("Content-Type", "application/json");

  return cartRequest("/api/v1/cart/items", {
    method: "POST",
    headers,
    body: JSON.stringify({
      cartId: getStoredCartId(),
      variantId,
      quantity
    })
  });
}

export function updateCartItem(itemId: string, quantity: number) {
  const headers = new Headers();
  headers.set("Content-Type", "application/json");

  return cartRequest(`/api/v1/cart/items/${encodeURIComponent(itemId)}`, {
    method: "PATCH",
    headers,
    body: JSON.stringify({
      cartId: getStoredCartId(),
      quantity
    })
  });
}

export function removeCartItem(itemId: string) {
  return cartRequest(`/api/v1/cart/items/${encodeURIComponent(itemId)}`, {
    method: "DELETE"
  });
}

export function clearCart() {
  return cartRequest("/api/v1/cart", {
    method: "DELETE"
  });
}