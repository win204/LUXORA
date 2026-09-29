import type { ProductListItem } from "@/lib/types";
import { ProductCard } from "./ProductCard";

type ProductGridProps = {
  products: ProductListItem[];
};

export function ProductGrid({ products }: ProductGridProps) {
  return (
    <div className="product-grid">
      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}
