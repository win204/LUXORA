import type { Metadata } from "next";
import Image from "next/image";
import { Container } from "@/components/Container";
import { LinkButton } from "@/components/Button";
import { Price } from "@/components/Price";
import { ProductGrid } from "@/components/ProductGrid";
import { ProductVariantSelector } from "@/components/ProductVariantSelector";
import { ProductReviewsSection } from "@/components/ProductReviewsSection";
import { getProduct, getProducts } from "@/lib/api";
import { getFallbackProductDetail } from "@/lib/fallbackProducts";
import type { ProductDetail } from "@/lib/types";

export const dynamic = "force-dynamic";

type ProductPageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const product = await getProduct(slug);
    return {
      title: `${product.name} | LUXORA Maison`,
      description: product.subtitle ?? product.description
    };
  } catch {
    const fallback = getFallbackProductDetail(slug);
    return {
      title: `${fallback.name} | LUXORA Maison`,
      description: fallback.subtitle ?? fallback.description
    };
  }
}

export default async function ProductPage({ params }: ProductPageProps) {
  const { slug } = await params;
  const product = await getProductResult(slug);
  const activeVariant = product.variants.find((variant) => variant.inStock) ?? product.variants[0];
  const related = await getRelatedProducts(product.category.slug, product.slug);

  return (
    <main className="page product-detail-page">
      <Container className="detail-layout">
        <section className="gallery" aria-label={`${product.name} images`}>
          {product.images.length > 0 ? (
            product.images.map((image) => (
              <div className="detail-image" key={image.id}>
                <Image
                  src={image.url}
                  alt={image.altText ?? product.name}
                  fill
                  sizes="(max-width: 1024px) 100vw, 56vw"
                />
              </div>
            ))
          ) : (
            <div className="detail-image image-fallback luxe-detail-fallback">
              <div className="luxe-fallback-decor">
                <span className="luxe-monogram-ring"></span>
                <span className="luxe-monogram-letter">{product.name.charAt(0)}</span>
              </div>
              <div className="luxe-fallback-info">
                <span className="luxe-fallback-brand">{product.brand.name}</span>
                <strong className="luxe-fallback-title">{product.name}</strong>
                <span className="luxe-fallback-edition">Maison Haute Technologie · Édition Limitée</span>
              </div>
            </div>
          )}
        </section>

        <section className="product-summary" aria-labelledby="product-title">
          <div className="product-brand-strip">
            <p className="eyebrow">{product.brand.name} · MAISON LUXORA</p>
            <span className="authenticity-pill">AUTHENTIC CERTIFIED</span>
          </div>

          <h1 id="product-title" className="product-title-serif">{product.name}</h1>
          {product.subtitle ? <p className="subtitle">{product.subtitle}</p> : null}

          <div className="product-price-row">
            <Price amount={activeVariant?.price ?? null} prefix={product.variants.length > 1 ? "Từ " : undefined} />
            <span className="vat-included-tag">Đã bao gồm thuế &amp; phí hải quan VIP</span>
          </div>

          <p className="description">{product.description}</p>

          {/* Maison Privileges Guarantee Box */}
          <div className="maison-guarantee-box">
            <div className="guarantee-item">
              <span className="guarantee-icon">✦</span>
              <div>
                <strong>White-Glove Delivery</strong>
                <span>Giao hàng tận tay trong 2 giờ tại đô thị lớn</span>
              </div>
            </div>
            <div className="guarantee-item">
              <span className="guarantee-icon">✦</span>
              <div>
                <strong>Luxury Packaging</strong>
                <span>Hộp sơn mài thủ công kèm phong thư bảo chứng</span>
              </div>
            </div>
            <div className="guarantee-item">
              <span className="guarantee-icon">✦</span>
              <div>
                <strong>Concierge VIP 24/7</strong>
                <span>Chăm sóc khách hàng cá nhân hóa riêng biệt</span>
              </div>
            </div>
          </div>

          <ProductVariantSelector variants={product.variants} />

          <div className="detail-block">
            <h2>Thông Số Tuyệt Tác (Specifications)</h2>
            <dl className="spec-list">
              {product.specifications.map((specification) => (
                <div key={specification.id}>
                  <dt>{specification.name}</dt>
                  <dd>{specification.value}</dd>
                </div>
              ))}
            </dl>
          </div>

          <LinkButton href="/shop" variant="secondary">
            ← Quay lại Bộ sưu tập
          </LinkButton>
        </section>
      </Container>

      <Container>
        <ProductReviewsSection
          productId={product.id}
          productSlug={product.slug}
          productName={product.name}
        />
      </Container>

      {related.length > 0 ? (
        <section className="section">
          <Container>
            <div className="section-heading">
              <p className="eyebrow">Tuyệt Tác Liên Quan</p>
              <h2>Khám phá thêm trong {product.category.name}</h2>
            </div>
            <ProductGrid products={related} />
          </Container>
        </section>
      ) : null}
    </main>
  );
}

async function getProductResult(slug: string): Promise<ProductDetail> {
  try {
    return await getProduct(slug);
  } catch {
    return getFallbackProductDetail(slug);
  }
}

async function getRelatedProducts(category: string, currentSlug: string) {
  try {
    const page = await getProducts({ category, size: "3" });
    return page.content.filter((product) => product.slug !== currentSlug).slice(0, 3);
  } catch {
    return [];
  }
}
