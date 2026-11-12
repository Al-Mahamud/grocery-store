import type { ProductCard } from "@/lib/types";
import { ProductCardView } from "./ProductCardView";

export function ProductGrid({ products, dense = true }: { products: ProductCard[]; dense?: boolean }) {
  return (
    <div className={`grid grid-cols-2 gap-2.5 sm:grid-cols-3 md:gap-3 lg:grid-cols-4 ${dense ? "xl:grid-cols-5 2xl:grid-cols-6" : "xl:grid-cols-5"}`}>
      {products.map((p) => <ProductCardView key={p.id} product={p} />)}
    </div>
  );
}
