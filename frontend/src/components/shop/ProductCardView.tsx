"use client";

import { useState } from "react";
import Link from "next/link";
import { Leaf } from "lucide-react";
import type { ProductCard } from "@/lib/types";
import { taka } from "@/lib/money";
import { percentOffPack, unitPriceText } from "@/lib/grocery";
import { QuantityControl } from "@/components/cart/QuantityControl";

/* eslint-disable @next/next/no-img-element -- product images are served by the backend as-is. */
export function ProductCardView({ product }: { product: ProductCard }) {
  const firstInStock = product.packs.findIndex((p) => p.stock > 0);
  const [index, setIndex] = useState(firstInStock < 0 ? 0 : firstInStock);
  const pack = product.packs[index];
  if (!pack) return null;
  const off = percentOffPack(pack);
  const unit = unitPriceText(pack);
  return (
    <article className="group flex flex-col rounded-2xl border border-line/70 bg-white p-3 transition hover:border-line hover:shadow-[0_12px_30px_-18px_rgba(26,26,46,0.35)]">
      <Link href={`/p/${product.slug}`} className="relative block">
        <div className="aspect-square overflow-hidden rounded-xl">
          {product.image && <img src={product.image} alt={product.name} loading="lazy" className="h-full w-full object-contain transition duration-500 group-hover:scale-105" />}
        </div>
        <div className="absolute left-0 top-0 flex flex-col items-start gap-1">
          {off ? <span className="rounded-lg bg-sun px-2 py-1 text-[0.6875rem] font-extrabold text-ink">{off}% OFF</span> : null}
          {product.organic && <span className="inline-flex items-center gap-1 rounded-lg bg-leaf-soft px-2 py-1 text-[0.6875rem] font-bold text-leaf"><Leaf size={11} /> Organic</span>}
        </div>
      </Link>
      <div className="mt-2 flex flex-1 flex-col">
        <Link href={`/p/${product.slug}`} className="line-clamp-2 min-h-10 text-sm font-semibold leading-5 hover:text-tangerine">{product.name}</Link>
        <p className="mt-0.5 truncate text-xs text-muted">
          {product.localName && <span className="font-bangla">{product.localName}</span>}
          {product.localName && product.brand && " · "}
          {product.brand}
        </p>
        {product.packs.length > 1 ? (
          <div className="no-scrollbar -mx-0.5 mt-2 flex gap-1 overflow-x-auto px-0.5" role="radiogroup" aria-label="Pack size">
            {product.packs.map((p, i) => (
              <button
                key={p.id}
                type="button"
                role="radio"
                aria-checked={i === index}
                onClick={() => setIndex(i)}
                className={`shrink-0 rounded-lg border px-2 py-1 text-[0.6875rem] font-semibold transition ${i === index ? "border-ink bg-ink text-white" : "border-line text-ink-soft hover:border-ink"} ${p.stock === 0 ? "line-through opacity-50" : ""}`}
              >
                {p.label}
              </button>
            ))}
          </div>
        ) : (
          <p className="mt-2 text-xs font-semibold text-ink-soft"><span className="rounded-lg bg-cream px-2 py-1">{pack.label}</span></p>
        )}
        <div className="mt-auto pt-3">
          <div className="flex items-baseline gap-1.5">
            <span className="font-display text-lg font-bold tabular-nums">{taka(pack.price)}</span>
            {pack.compareAtPrice && <span className="text-xs text-muted line-through tabular-nums">{taka(pack.compareAtPrice)}</span>}
          </div>
          <p className="h-4 text-[0.6875rem] text-muted">{unit ?? (product.weighed ? "approx. weight" : "")}</p>
          <div className="mt-2"><QuantityControl pack={pack} name={product.name} /></div>
        </div>
      </div>
    </article>
  );
}
