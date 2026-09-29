import Link from "next/link";
import type { ProductListItem } from "@/lib/types";
import { Price } from "./Price";

type ProductCardProps = {
  product: ProductListItem;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="product-card">
      <Link className="product-card-link" href={`/products/${product.slug}`} aria-label={product.name}>
        <div className="product-media">
          <span>{product.name}</span>
        </div>
        <div className="product-card-body">
          <div>
            <p className="meta">{product.brand.name}</p>
            <h3>{product.name}</h3>
            {product.subtitle ? <p>{product.subtitle}</p> : null}
          </div>
          <div className="product-card-footer">
            <Price amount={product.minPrice} prefix="From " />
            <span className={product.inStock ? "stock in" : "stock out"}>
              {product.inStock ? "In stock" : "Limited"}
            </span>
          </div>
        </div>
      </Link>
    </article>
  );
}
