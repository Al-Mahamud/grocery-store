"use client";

import { useState } from "react";
import Link from "next/link";
import { Clock, Leaf, MapPin, Refrigerator, Scale, Truck } from "lucide-react";
import type { ProductDetail } from "@/lib/types";
import { taka } from "@/lib/money";
import { percentOffPack, unitPriceText } from "@/lib/grocery";
import { site } from "@/config/site";
import { QuantityControl } from "@/components/cart/QuantityControl";

/* eslint-disable @next/next/no-img-element */
export function ProductView({ product, nextSlot }: { product: ProductDetail; nextSlot: string | null }) {
  const first = product.packs.findIndex((p) => p.stock > 0);
  const [index, setIndex] = useState(first < 0 ? 0 : first);
  const pack = product.packs[index];
  return (
    <div className="grid gap-5 lg:grid-cols-2 lg:gap-8">
      <div className="relative overflow-hidden rounded-3xl bg-white p-6 lg:sticky lg:top-40 lg:self-start">
        {product.image && <img src={product.image} alt={product.name} className="mx-auto aspect-square w-full max-w-lg object-contain" />}
        <div className="absolute left-5 top-5 flex flex-col gap-1.5">
          {pack && percentOffPack(pack) ? <span className="rounded-lg bg-sun px-2.5 py-1 text-xs font-extrabold">{percentOffPack(pack)}% OFF</span> : null}
          {product.organic && <span className="inline-flex items-center gap-1 rounded-lg bg-leaf-soft px-2.5 py-1 text-xs font-bold text-leaf"><Leaf size={12} /> Organic</span>}
          {product.fresh && <span className="rounded-lg bg-tangerine-soft px-2.5 py-1 text-xs font-bold text-tangerine-dark">Fresh today</span>}
        </div>
      </div>

      <div className="space-y-4">
        <div className="rounded-3xl bg-white p-5 md:p-7">
          <nav aria-label="Breadcrumb" className="text-xs text-muted">
            <ol className="flex flex-wrap gap-1.5">
              <li><Link href="/" className="hover:text-ink">Home</Link></li>
              {product.breadcrumb.map((c) => <li key={c.slug} className="flex gap-1.5"><span aria-hidden>›</span><Link href={`/c/${c.slug}`} className="hover:text-ink">{c.name}</Link></li>)}
            </ol>
          </nav>
          <h1 className="mt-3 font-display text-3xl font-extrabold leading-tight tracking-tight md:text-4xl">{product.name}</h1>
          <p className="mt-1 text-sm text-muted">
            {product.localName && <span className="font-bangla text-base text-ink-soft">{product.localName}</span>}
            {product.brand && <> · by <strong className="text-ink-soft">{product.brand}</strong></>}
          </p>

          <fieldset className="mt-6">
            <legend className="mb-2 text-sm font-bold">Choose a pack</legend>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {product.packs.map((p, i) => {
                const selected = i === index;
                const out = p.stock === 0;
                return (
                  <button key={p.id} type="button" disabled={out} aria-pressed={selected} onClick={() => setIndex(i)} className={`rounded-2xl border-2 p-3 text-left transition ${selected ? "border-tangerine bg-tangerine-soft" : out ? "border-line opacity-50" : "border-line hover:border-ink/40"}`}>
                    <span className="block text-sm font-bold">{p.label}</span>
                    <span className="mt-1 block font-display text-xl font-bold tabular-nums">{out ? "Sold out" : taka(p.price)}</span>
                    <span className="block text-xs text-muted">{p.compareAtPrice ? <s>{taka(p.compareAtPrice)}</s> : null} {unitPriceText(p)}</span>
                  </button>
                );
              })}
            </div>
          </fieldset>

          {pack && (
            <div className="mt-5 flex items-center gap-4">
              <div className="w-full max-w-xs"><QuantityControl pack={pack} name={product.name} size="lg" /></div>
              <p className="text-xs text-muted">{pack.stock > 0 && pack.stock <= 5 ? <strong className="text-berry">Only {pack.stock} left</strong> : `Up to ${pack.maxPerOrder} per order`}</p>
            </div>
          )}
          {product.weighed && (
            <p className="mt-4 flex gap-2 rounded-2xl bg-cream p-3 text-xs text-ink-soft"><Scale size={15} className="mt-0.5 shrink-0" />Sold by approximate weight. Your item may weigh slightly more or less; you pay the listed price.</p>
          )}
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <Info icon={<Clock size={18} />} title="Next delivery" text={nextSlot ?? "Choose a slot at checkout"} />
          <Info icon={<Truck size={18} />} title="Delivery fee" text={`${taka(site.deliveryFee)}, free over ${taka(site.freeDeliveryOver)}`} />
          {product.storage && <Info icon={<Refrigerator size={18} />} title="Storage" text={product.storage} />}
          {product.origin && <Info icon={<MapPin size={18} />} title="Origin" text={product.origin} />}
        </div>

        <div className="rounded-3xl bg-white p-5 md:p-7">
          <h2 className="font-display text-xl font-bold">About this product</h2>
          <p className="mt-2 text-sm leading-relaxed text-ink-soft">{product.description}</p>
        </div>
      </div>
    </div>
  );
}

function Info({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return (
    <div className="flex gap-3 rounded-2xl bg-white p-4">
      <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-cream text-tangerine">{icon}</span>
      <div><p className="text-xs font-bold text-muted">{title}</p><p className="text-sm font-semibold">{text}</p></div>
    </div>
  );
}
