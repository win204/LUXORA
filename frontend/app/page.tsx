import type { Metadata } from "next";
import { Container } from "@/components/Container";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { LinkButton } from "@/components/Button";
import { ProductGrid } from "@/components/ProductGrid";
import { getProducts } from "@/lib/api";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "LUXORA",
  description: "Shop premium technology essentials with a minimal, polished catalog experience."
};

export default async function Home() {
  const productsResult = await getFeaturedProducts();

  return (
    <main>
      <section className="hero-section">
        <Container className="hero">
          <p className="eyebrow">LUXORA</p>
          <h1>Designed essentials for a quieter digital life.</h1>
          <p>
            Explore a focused collection of refined devices, audio, and screens selected for
            everyday clarity.
          </p>
          <div className="hero-actions">
            <LinkButton href="/shop">Shop catalog</LinkButton>
            <LinkButton href="/shop?sort=priceAsc" variant="secondary">
              View pricing
            </LinkButton>
          </div>
        </Container>
      </section>

      <section className="section">
        <Container>
          <div className="section-heading">
            <p className="eyebrow">Featured</p>
            <h2>Current collection</h2>
          </div>
          {productsResult.ok ? (
            productsResult.products.length > 0 ? (
              <ProductGrid products={productsResult.products} />
            ) : (
              <EmptyState title="No products yet" message="The catalog is ready for seeded products." />
            )
          ) : (
            <ErrorState message={productsResult.message} />
          )}
        </Container>
      </section>

      <section className="story-section">
        <Container className="story">
          <p className="eyebrow">Approach</p>
          <h2>Premium without noise.</h2>
          <p>
            LUXORA keeps the storefront intentional: concise product stories, transparent
            availability, and a catalog built from the backend source of truth.
          </p>
        </Container>
      </section>
    </main>
  );
}

async function getFeaturedProducts() {
  try {
    const page = await getProducts({ size: "6", sort: "nameAsc" });
    return { ok: true as const, products: page.content };
  } catch (error) {
    return {
      ok: false as const,
      message: error instanceof Error ? error.message : "The catalog could not be loaded."
    };
  }
}
