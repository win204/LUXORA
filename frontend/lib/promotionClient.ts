import type { AvailablePromotion } from "./types";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  process.env.API_BASE_URL ??
  "http://localhost:8080";

export async function fetchAvailablePromotions(subtotal = 0): Promise<AvailablePromotion[]> {
  try {
    const res = await fetch(`${apiBaseUrl}/api/v1/promotions/available?subtotal=${encodeURIComponent(subtotal)}`, {
      headers: {
        Accept: "application/json"
      }
    });
    if (!res.ok) return [];
    return (await res.json()) as AvailablePromotion[];
  } catch {
    return [];
  }
}
