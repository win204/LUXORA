import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/Button";
import { Container } from "@/components/Container";
import { EmptyState } from "@/components/EmptyState";
import { ProductGrid } from "@/components/ProductGrid";
import { getBrands, getCategories, getProducts } from "@/lib/api";
import { FALLBACK_PRODUCTS } from "@/lib/fallbackProducts";
import type { Brand, Category, ProductListItem, ProductQuery } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Bộ Sưu Tập Tuyệt Tác | LUXORA Maison",
  description: "Khám phá danh mục xa xỉ phẩm kỹ thuật số, đồng hồ chế tác và thiết bị âm thanh đỉnh cao."
};

const DEFAULT_CATEGORIES: Category[] = [
  { id: "c-1", name: "Smartphones", slug: "smartphones" },
  { id: "c-2", name: "Smart Horology", slug: "horology" },
  { id: "c-3", name: "Audio & Acoustics", slug: "audio" },
  { id: "c-4", name: "Luxury Accessories", slug: "accessories" }
];

const DEFAULT_BRANDS: Brand[] = [
  { id: "b-1", name: "Aero Flagship", slug: "aero" },
  { id: "b-2", name: "Chrono Horology", slug: "chrono" },
  { id: "b-3", name: "Opus Acoustics", slug: "opus" },
  { id: "b-4", name: "Lumina Studio", slug: "lumina" },
  { id: "b-5", name: "LUXORA Maison", slug: "luxora-studio" }
];

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

  const categories = categoriesResult.ok && categoriesResult.categories.length > 0
    ? categoriesResult.categories
    : DEFAULT_CATEGORIES;

  const brands = brandsResult.ok && brandsResult.brands.length > 0
    ? brandsResult.brands
    : DEFAULT_BRANDS;

  // Resolved products: live API data if available, or filtered curated showcase
  let displayProducts: ProductListItem[] = [];
  let totalCount = 0;
  let totalPages = 1;
  let isFallbackMode = false;

  if (productsResult.ok && productsResult.page.content.length > 0) {
    displayProducts = productsResult.page.content;
    totalCount = productsResult.page.totalElements;
    totalPages = productsResult.page.totalPages;
  } else if (productsResult.ok && productsResult.page.content.length === 0) {
    displayProducts = [];
    totalCount = 0;
    totalPages = 0;
  } else {
    // Graceful luxury showcase fallback
    isFallbackMode = true;
    let filtered = [...FALLBACK_PRODUCTS];
    if (query.category) {
      filtered = filtered.filter((p) => p.category.slug === query.category);
    }
    if (query.brand) {
      filtered = filtered.filter((p) => p.brand.slug === query.brand);
    }
    if (query.search) {
      const s = query.search.toLowerCase();
      filtered = filtered.filter((p) => p.name.toLowerCase().includes(s) || (p.subtitle && p.subtitle.toLowerCase().includes(s)));
    }
    displayProducts = filtered;
    totalCount = filtered.length;
    totalPages = 1;
  }

  return (
    <main className="page shop-luxury-page">
      <Container>
        <div className="page-heading shop-page-heading">
          <p className="eyebrow">LUXORA MAISON · CATALOGUE D&apos;EXCEPTION</p>
          <h1 className="gold-text">Curated Catalogue</h1>
          <p className="shop-heading-sub">
            Tuyển tập những tuyệt tác công nghệ và chế tác tinh xảo, định hình chuẩn mực xa xỉ đương đại.
          </p>
        </div>

        {/* Quick Category Navigation Pills */}
        <div className="category-quick-pills" role="navigation" aria-label="Quick category filters">
          <Link
            href="/shop"
            className={!query.category ? "category-pill is-active" : "category-pill"}
          >
            Tất cả Tuyệt tác
          </Link>
          {categories.map((cat) => (
            <Link
              key={cat.id}
              href={`/shop?category=${cat.slug}`}
              className={query.category === cat.slug ? "category-pill is-active" : "category-pill"}
            >
              {cat.name}
            </Link>
          ))}
        </div>

        {/* Filter Bar */}
        <form className="filter-bar shop-filter-bar" action="/shop">
          <label>
            <span>Tìm kiếm</span>
            <input name="search" defaultValue={query.search} placeholder="AeroPhone, Chrono..." />
          </label>
          <label>
            <span>Danh mục</span>
            <select name="category" defaultValue={query.category}>
              <option value="">Tất cả danh mục</option>
              {categories.map((category) => (
                <option key={category.id} value={category.slug}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Thương hiệu</span>
            <select name="brand" defaultValue={query.brand}>
              <option value="">Tất cả thương hiệu</option>
              {brands.map((brand) => (
                <option key={brand.id} value={brand.slug}>
                  {brand.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Giá từ ($)</span>
            <input name="minPrice" defaultValue={query.minPrice} inputMode="decimal" placeholder="0" />
          </label>
          <label>
            <span>Đến ($)</span>
            <input name="maxPrice" defaultValue={query.maxPrice} inputMode="decimal" placeholder="5000" />
          </label>
          <label>
            <span>Sắp xếp</span>
            <select name="sort" defaultValue={query.sort ?? "newest"}>
              <option value="newest">Mới nhất Maison</option>
              <option value="priceAsc">Giá tăng dần</option>
              <option value="priceDesc">Giá giảm dần</option>
              <option value="nameAsc">Tên A-Z</option>
            </select>
          </label>
          <Button type="submit">Lọc Sản Phẩm</Button>
          <Link className="clear-link" href="/shop">
            Đặt Lại
          </Link>
        </form>

        {isFallbackMode && (
          <div className="showcase-mode-banner">
            <span className="showcase-mode-badge">MAISON SHOWCASE</span>
            <span>Hiển thị tuyển tập tuyển chọn trực tiếp từ danh mục chính thức của Maison LUXORA</span>
          </div>
        )}

        <div className="results-bar">
          <p>
            <strong className="gold-text">{totalCount}</strong> tuyệt tác sẵn sàng phục vụ
          </p>
          <p>
            Trang {Number(query.page ?? "0") + 1} / {Math.max(totalPages, 1)}
          </p>
        </div>

        {displayProducts.length > 0 ? (
          <ProductGrid products={displayProducts} />
        ) : (
          <EmptyState
            title="Không tìm thấy tuyệt tác phù hợp"
            message="Quý khách vui lòng điều chỉnh lại bộ lọc hoặc đặt lại tiêu chí tìm kiếm."
          />
        )}

        <Pagination query={query} totalPages={totalPages} />
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
