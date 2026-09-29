import type {
  Brand,
  Category,
  PageResponse,
  ProductDetail,
  ProductListItem,
  ProductQuery
} from "./types";

const apiBaseUrl =
  process.env.API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function getJson<T>(path: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      cache: "no-store",
      headers: {
        Accept: "application/json"
      }
    });
  } catch {
    throw new ApiError("Catalog service is unavailable.");
  }

  if (!response.ok) {
    throw new ApiError("Catalog request failed.", response.status);
  }

  return response.json() as Promise<T>;
}

function queryString(query: ProductQuery): string {
  const params = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value.trim() !== "") {
      params.set(key, value);
    }
  });

  const value = params.toString();
  return value ? `?${value}` : "";
}

export function getProducts(query: ProductQuery = {}) {
  return getJson<PageResponse<ProductListItem>>(`/api/v1/products${queryString(query)}`);
}

export function getProduct(slug: string) {
  return getJson<ProductDetail>(`/api/v1/products/${encodeURIComponent(slug)}`);
}

export function getCategories() {
  return getJson<Category[]>("/api/v1/categories");
}

export function getBrands() {
  return getJson<Brand[]>("/api/v1/brands");
}
