"use client";

import { useState } from "react";
import type { ProductCard, ProductPage } from "@/lib/types";
import { ProductGrid } from "./ProductGrid";

export function LoadMoreGrid({ initial, query }: { initial: ProductPage; query: string }) {
  const [items, setItems] = useState<ProductCard[]>(initial.items);
  const [page, setPage] = useState(initial.page);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const loadMore = async () => {
    setLoading(true);
    setFailed(false);
    try {
      const params = new URLSearchParams(query);
      params.set("page", String(page + 1));
      const response = await fetch(`/api/v1/products?${params}`);
      if (!response.ok) throw new Error(String(response.status));
      const next = (await response.json()) as ProductPage;
      setItems((current) => [...current, ...next.items.filter((n) => !current.some((c) => c.id === n.id))]);
      setPage(next.page);
    } catch {
      setFailed(true);
    } finally {
      setLoading(false);
    }
  };
  return (
    <>
      <ProductGrid products={items} dense={false} />
      {items.length < initial.total && (
        <div className="mt-8 flex flex-col items-center gap-2">
          <button type="button" onClick={loadMore} disabled={loading} className="rounded-xl bg-ink px-8 py-3 text-sm font-bold text-white disabled:opacity-50">
            {loading ? "Loading…" : `Show more (${initial.total - items.length})`}
          </button>
          {failed && <p role="alert" className="text-sm text-berry">Couldn&rsquo;t load more. Please try again.</p>}
        </div>
      )}
    </>
  );
}
