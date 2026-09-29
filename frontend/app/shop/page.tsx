import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/Button";
import { Container } from "@/components/Container";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { ProductGrid } from "@/components/ProductGrid";
import { getBrands, getCategories, getProducts } from "@/lib/api";
import type { Brand, Category, ProductQuery } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Shop",
  description: "Browse LUXORA products with search, filters, pricing, and availability."
};

type ShopPageProps = {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
};

export default async function ShopPage({ searchParams }: ShopPageProps) {
  const params = await searchParams;
  const query = normalizeQuery(params);
  const [productsResult, categoriesResult, brandsResult] = await Promise.all([
    getProductsResult(query),
    getCategoriesResult(),
    getBrandsResult()
  ]);

  return (
    <main className="page">
      <Container>
        <div className="page-heading">
          <p className="eyebrow">Shop</p>
          <h1>Catalog</h1>
          <p>Filter the live catalog by product story, category, brand, and price.</p>
        </div>

        <form className="filter-bar" action="/shop">
          <label>
            <span>Search</span>
            <input name="search" defaultValue={query.search} placeholder="AeroPhone" />
          </label>
          <label>
            <span>Category</span>
            <select name="category" defaultValue={query.category}>
              <option value="">All categories</option>
              {categoriesResult.ok
                ? categoriesResult.categories.map((category) => (
                    <option key={category.id} value={category.slug}>
                      {category.name}
                    </option>
                  ))
                : null}
            </select>
          </label>
          <label>
            <span>Brand</span>
            <select name="brand" defaultValue={query.brand}>
              <option value="">All brands</option>
              {brandsResult.ok
                ? brandsResult.brands.map((brand) => (
                    <option key={brand.id} value={brand.slug}>
                      {brand.name}
                    </option>
                  ))
                : null}
            </select>
          </label>
          <label>
            <span>Min</span>
            <input name="minPrice" defaultValue={query.minPrice} inputMode="decimal" placeholder="0" />
          </label>
          <label>
            <span>Max</span>
            <input name="maxPrice" defaultValue={query.maxPrice} inputMode="decimal" placeholder="1200" />
          </label>
          <label>
            <span>Sort</span>
            <select name="sort" defaultValue={query.sort ?? "newest"}>
              <option value="newest">Newest</option>
              <option value="nameAsc">Name</option>
              <option value="priceAsc">Price low</option>
              <option value="priceDesc">Price high</option>
            </select>
          </label>
          <Button type="submit">Apply</Button>
          <Link className="clear-link" href="/shop">
            Reset
          </Link>
        </form>

        {productsResult.ok ? (
          <>
            <div className="results-bar">
              <p>{productsResult.page.totalElements} products</p>
              <p>
                Page {productsResult.page.page + 1} of {Math.max(productsResult.page.totalPages, 1)}
              </p>
            </div>
            {productsResult.page.content.length > 0 ? (
              <ProductGrid products={productsResult.page.content} />
            ) : (
              <EmptyState title="No products found" message="Try adjusting the filters." />
            )}
            <Pagination query={query} totalPages={productsResult.page.totalPages} />
          </>
        ) : (
          <ErrorState message={productsResult.message} />
        )}
      </Container>
    </main>
  );
}

function Pagination({ query, totalPages }: { query: ProductQuery; totalPages: number }) {
  const currentPage = Number(query.page ?? "0");
  const previous = Math.max(currentPage - 1, 0);
  const next = Math.min(currentPage + 1, Math.max(totalPages - 1, 0));

  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav className="pagination" aria-label="Product pagination">
      <Link href={`/shop${toQuery({ ...query, page: String(previous) })}`} aria-disabled={currentPage === 0}>
        Previous
      </Link>
      <span>{currentPage + 1}</span>
      <Link
        href={`/shop${toQuery({ ...query, page: String(next) })}`}
        aria-disabled={currentPage >= totalPages - 1}
      >
        Next
      </Link>
    </nav>
  );
}

function normalizeQuery(params: Record<string, string | string[] | undefined>): ProductQuery {
  return {
    page: first(params.page) ?? "0",
    size: first(params.size) ?? "9",
    sort: first(params.sort) ?? "newest",
    search: first(params.search),
    category: first(params.category),
    brand: first(params.brand),
    minPrice: first(params.minPrice),
    maxPrice: first(params.maxPrice)
  };
}

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function toQuery(query: ProductQuery) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value && value.trim() !== "") {
      params.set(key, value);
    }
  });
  const value = params.toString();
  return value ? `?${value}` : "";
}

async function getProductsResult(query: ProductQuery) {
  try {
    return { ok: true as const, page: await getProducts(query) };
  } catch (error) {
    return { ok: false as const, message: error instanceof Error ? error.message : "Unable to load products." };
  }
}

async function getCategoriesResult() {
  try {
    return { ok: true as const, categories: await getCategories() };
  } catch {
    return { ok: false as const, categories: [] as Category[] };
  }
}

async function getBrandsResult() {
  try {
    return { ok: true as const, brands: await getBrands() };
  } catch {
    return { ok: false as const, brands: [] as Brand[] };
  }
}
