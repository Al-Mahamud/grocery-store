"use client";

import { useState, useTransition } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Check, ChevronDown, SlidersHorizontal, X } from "lucide-react";
import type { ProductPage } from "@/lib/types";
import { SORTS } from "@/lib/listing";
import { Drawer } from "@/components/ui/Drawer";

type Facets = ProductPage["facets"];

function useFilters() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [pending, start] = useTransition();
  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params.toString());
    for (const [k, v] of Object.entries(changes)) {
      if (v === null || v === "") next.delete(k);
      else next.set(k, v);
    }
    const s = next.toString();
    start(() => router.replace(s ? `${pathname}?${s}` : pathname, { scroll: false }));
  };
  const brands = (params.get("brand") ?? "").split(",").filter(Boolean);
  const toggleBrand = (b: string) => update({ brand: (brands.includes(b) ? brands.filter((x) => x !== b) : [...brands, b]).join(",") || null });
  return { params, update, brands, toggleBrand, pending };
}

export function FilterPanel({ facets, hideDeal = false }: { facets: Facets; hideDeal?: boolean }) {
  const { params, update, brands, toggleBrand, pending } = useFilters();
  const flags = [
    { key: "organic", label: "Organic", count: facets.organic },
    { key: "local", label: "Grown or made in Bangladesh", count: facets.local },
    ...(hideDeal ? [] : [{ key: "deal", label: "On offer", count: facets.deals }]),
  ];
  return (
    <div className={`space-y-4 transition-opacity ${pending ? "opacity-60" : ""}`}>
      <div className="rounded-2xl bg-white p-4">
        <p className="mb-2 text-sm font-bold">Show only</p>
        <div className="space-y-1">
          {flags.map((f) => {
            const on = params.get(f.key) === "true";
            return (
              <label key={f.key} className="flex cursor-pointer items-center gap-2.5 rounded-lg px-1.5 py-1.5 text-sm hover:bg-cream">
                <input type="checkbox" checked={on} onChange={() => update({ [f.key]: on ? null : "true" })} className="peer sr-only" />
                <span className={`grid h-5 w-5 shrink-0 place-items-center rounded-md border-2 ${on ? "border-tangerine bg-tangerine text-white" : "border-line"}`}>{on && <Check size={12} strokeWidth={3} />}</span>
                <span className="flex-1">{f.label}</span>
                <span className="text-xs text-muted">{f.count}</span>
              </label>
            );
          })}
        </div>
      </div>
      {facets.brands.length > 1 && (
        <div className="rounded-2xl bg-white p-4">
          <p className="mb-2 text-sm font-bold">Brand</p>
          <div className="space-y-1">
            {facets.brands.map((b) => {
              const on = brands.includes(b.value);
              return (
                <label key={b.value} className="flex cursor-pointer items-center gap-2.5 rounded-lg px-1.5 py-1.5 text-sm hover:bg-cream">
                  <input type="checkbox" checked={on} onChange={() => toggleBrand(b.value)} className="sr-only" />
                  <span className={`grid h-5 w-5 shrink-0 place-items-center rounded-md border-2 ${on ? "border-tangerine bg-tangerine text-white" : "border-line"}`}>{on && <Check size={12} strokeWidth={3} />}</span>
                  <span className="flex-1 truncate">{b.label}</span>
                  <span className="text-xs text-muted">{b.count}</span>
                </label>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

export function MobileFilters({ facets, total, hideDeal }: { facets: Facets; total: number; hideDeal?: boolean }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)} className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3.5 py-2 text-sm font-bold lg:hidden"><SlidersHorizontal size={15} /> Filter</button>
      <Drawer open={open} onClose={() => setOpen(false)} side="left" label="Filters">
        <div className="flex h-16 items-center justify-between px-5">
          <span className="font-display text-xl font-bold">Filter</span>
          <button type="button" aria-label="Close" onClick={() => setOpen(false)} className="grid h-10 w-10 place-items-center rounded-xl bg-cream"><X size={18} /></button>
        </div>
        <div className="flex-1 overflow-y-auto bg-cream p-4"><FilterPanel facets={facets} hideDeal={hideDeal} /></div>
        <div className="p-4"><button type="button" onClick={() => setOpen(false)} className="w-full rounded-xl bg-tangerine py-3.5 text-sm font-bold text-white">Show {total} products</button></div>
      </Drawer>
    </>
  );
}

export function SortSelect() {
  const { params, update } = useFilters();
  return (
    <label className="relative inline-flex items-center rounded-xl border border-line bg-white text-sm">
      <span className="sr-only">Sort by</span>
      <select value={params.get("sort") ?? "recommended"} onChange={(e) => update({ sort: e.target.value === "recommended" ? null : e.target.value })} className="appearance-none bg-transparent py-2 pl-3.5 pr-9 font-semibold outline-none">
        {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
      </select>
      <ChevronDown size={15} className="pointer-events-none absolute right-3" />
    </label>
  );
}

export function ActiveFilters() {
  const { params, update, brands, toggleBrand } = useFilters();
  const chips: { label: string; remove: () => void }[] = brands.map((b) => ({ label: b, remove: () => toggleBrand(b) }));
  if (params.get("organic")) chips.push({ label: "Organic", remove: () => update({ organic: null }) });
  if (params.get("local")) chips.push({ label: "Local", remove: () => update({ local: null }) });
  if (params.get("deal")) chips.push({ label: "On offer", remove: () => update({ deal: null }) });
  if (chips.length === 0) return null;
  return (
    <div className="mb-4 flex flex-wrap gap-2">
      {chips.map((c) => (
        <button key={c.label} type="button" onClick={c.remove} aria-label={`Remove ${c.label}`} className="inline-flex items-center gap-1 rounded-lg bg-ink px-2.5 py-1.5 text-xs font-bold text-white">{c.label} <X size={12} /></button>
      ))}
      <button type="button" onClick={() => update({ brand: null, organic: null, local: null, deal: null })} className="text-xs font-bold text-tangerine">Clear all</button>
    </div>
  );
}
