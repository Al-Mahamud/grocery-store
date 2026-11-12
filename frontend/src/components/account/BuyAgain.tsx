"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { RotateCcw } from "lucide-react";
import { request } from "@/lib/client";
import type { BuyAgainItem } from "@/lib/types";
import { ProductCardView } from "@/components/shop/ProductCardView";

/** Everything the customer has bought before, most often bought first, set to the pack they chose last. */
export function BuyAgain() {
  const [items, setItems] = useState<BuyAgainItem[] | null>(null);
  useEffect(() => {
    request<BuyAgainItem[]>("GET", "/account/buy-again").then(setItems).catch(() => setItems([]));
  }, []);

  if (!items) return <div className="h-72 animate-pulse rounded-3xl bg-white" aria-busy="true" />;
  if (items.length === 0) {
    return (
      <div className="grid place-items-center rounded-3xl bg-white px-6 py-14 text-center">
        <span className="grid h-16 w-16 place-items-center rounded-2xl bg-leaf-soft text-leaf"><RotateCcw size={28} /></span>
        <h2 className="mt-4 font-display text-2xl font-bold">Your usuals will live here</h2>
        <p className="mt-1 max-w-xs text-sm text-muted">After your first order, the things you buy show up here so the weekly shop takes a minute.</p>
        <Link href="/" className="mt-5 rounded-xl bg-tangerine px-6 py-3 text-sm font-bold text-white">Start shopping</Link>
      </div>
    );
  }
  return (
    <section aria-labelledby="buy-again-title">
      <div className="mb-4 flex items-end justify-between">
        <div>
          <h2 id="buy-again-title" className="font-display text-2xl font-bold">Buy again</h2>
          <p className="text-sm text-muted">{items.length} product{items.length === 1 ? "" : "s"} from your past orders</p>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 md:gap-3 xl:grid-cols-4 2xl:grid-cols-5">
        {items.map((i) => (
          <ProductCardView key={i.product.id} product={i.product} initialPackId={i.packId} badge={i.timesBought > 1 ? `Bought ${i.timesBought}×` : undefined} />
        ))}
      </div>
    </section>
  );
}
