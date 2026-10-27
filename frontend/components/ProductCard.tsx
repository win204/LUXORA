import Link from "next/link";
import type { ProductListItem } from "@/lib/types";
import { Price } from "./Price";
import { getProductDisplayImage } from "@/lib/fallbackProducts";

type ProductCardProps = {
  product: ProductListItem;
};

export function ProductCard({ product }: ProductCardProps) {
  const monogram = product.name ? product.name.charAt(0) : "L";
  const displayImage = getProductDisplayImage(product.slug, product.imageUrl);

  return (
    <article className="product-card">
      <Link className="product-card-link" href={`/products/${product.slug}`} aria-label={product.name}>
          <div className="product-media">
          {product.category?.name ? (
            <span className="product-category-tag">{product.category.name}</span>
          ) : null}
          {displayImage ? (
            /* eslint-disable-next-line @next/next/no-img-element */
            <img
              src={displayImage}
              alt={product.name}
              className="product-card-image"
              style={{
                position: "absolute",
                inset: 0,
                width: "100%",
                height: "100%",
                objectFit: "cover",
                transition: "transform 700ms var(--ease)"
              }}
            />
          ) : (
            <>
              <div className="product-media-ring" aria-hidden="true" />
              <span className="product-monogram" aria-hidden="true">{monogram}</span>
            </>
          )}
          <div className="product-quick" aria-hidden="true">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
          </div>
        </div>
        <div className="product-card-body">
          <div>
            <p className="meta">{product.brand?.name ?? "LUXORA MAISON"}</p>
            <h3>{product.name}</h3>
            {product.subtitle ? <p>{product.subtitle}</p> : null}
          </div>
          <div className="product-card-footer">
            <Price amount={product.minPrice} prefix="Từ " />
            <span className={product.inStock ? "stock in" : "stock out"}>
              {product.inStock ? "Sẵn sàng giao" : "Số lượng ít"}
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}
