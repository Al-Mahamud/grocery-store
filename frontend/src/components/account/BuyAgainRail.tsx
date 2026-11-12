"use client";

import { useEffect, useState } from "react";
import { RotateCcw } from "lucide-react";
import { request } from "@/lib/client";
import type { BuyAgainItem } from "@/lib/types";
import { ProductRail } from "@/components/shop/ProductRail";
import { SectionHeading } from "@/components/shop/SectionHeading";
import { firstName, useAccount } from "./AccountProvider";

/** On the home page, for signed-in customers who have ordered before. */
export function BuyAgainRail() {
  const { me } = useAccount();
  const [items, setItems] = useState<BuyAgainItem[]>([]);
  useEffect(() => {
    if (!me) return;
    request<BuyAgainItem[]>("GET", "/account/buy-again").then(setItems).catch(() => setItems([]));
  }, [me]);
  if (!me || items.length === 0) return null;
  return (
    <section className="animate-rise rounded-3xl bg-white p-4 md:p-6">
      <SectionHeading
        title={<span className="inline-flex items-center gap-2"><RotateCcw className="text-leaf" size={22} /> Your usuals, {firstName(me.name)}</span>}
        note="From your past orders, set to the pack you bought last"
        href="/account/buy-again"
      />
      <ProductRail products={items.slice(0, 12).map((i) => i.product)} packs={Object.fromEntries(items.map((i) => [i.product.id, i.packId]))} />
    </section>
  );
}
