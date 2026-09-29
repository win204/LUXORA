import type { Metadata } from "next";
import Image from "next/image";
import { notFound } from "next/navigation";
import { Container } from "@/components/Container";
import { ErrorState } from "@/components/ErrorState";
import { LinkButton } from "@/components/Button";
import { Price } from "@/components/Price";
import { ProductGrid } from "@/components/ProductGrid";
import { ProductVariantSelector } from "@/components/ProductVariantSelector";
import { ApiError, getProduct, getProducts } from "@/lib/api";

export const dynamic = "force-dynamic";

type ProductPageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const product = await getProduct(slug);
    return {
      title: product.name,
      description: product.subtitle ?? product.description
    };
  } catch {
    return {
      title: "Product not found"
    };
  }
}

export default async function ProductPage({ params }: ProductPageProps) {
  const { slug } = await params;
  const productResult = await getProductResult(slug);

  if (!productResult.ok) {
    if (productResult.status === 404) {
      notFound();
    }

    return (
      <main className="page">
        <Container>
          <ErrorState message={productResult.message} />
        </Container>
      </main>
    );
  }

  const product = productResult.product;
  const activeVariant = product.variants.find((variant) => variant.inStock) ?? product.variants[0];
  const related = await getRelatedProducts(product.category.slug, product.slug);

  return (
    <main className="page">
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
            <div className="detail-image image-fallback">
              <span>{product.name}</span>
            </div>
          )}
        </section>

        <section className="product-summary" aria-labelledby="product-title">
          <p className="eyebrow">{product.brand.name}</p>
          <h1 id="product-title">{product.name}</h1>
          {product.subtitle ? <p className="subtitle">{product.subtitle}</p> : null}
          <Price amount={activeVariant?.price ?? null} prefix={product.variants.length > 1 ? "From " : undefined} />
          <p className="description">{product.description}</p>
          <ProductVariantSelector variants={product.variants} />

          <div className="detail-block">
            <h2>Specifications</h2>
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
            Back to shop
          </LinkButton>
        </section>
      </Container>

      {related.length > 0 ? (
        <section className="section">
          <Container>
            <div className="section-heading">
              <p className="eyebrow">Related</p>
              <h2>More in {product.category.name}</h2>
            </div>
            <ProductGrid products={related} />
          </Container>
        </section>
      ) : null}
    </main>
  );
}

async function getProductResult(slug: string) {
  try {
    return { ok: true as const, product: await getProduct(slug) };
  } catch (error) {
    return {
      ok: false as const,
      status: error instanceof ApiError ? error.status : undefined,
      message: error instanceof Error ? error.message : "Unable to load this product."
    };
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
