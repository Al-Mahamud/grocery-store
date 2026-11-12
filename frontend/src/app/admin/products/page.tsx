"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Plus, Search } from "lucide-react";
import { request } from "@/lib/client";
import type { AdminProductPage } from "@/lib/admin-types";
import type { CategoryNode } from "@/lib/types";
import { taka } from "@/lib/money";
import { Empty, Loading, PageHeader, btn, input } from "@/components/admin/ui";

const statusLabel = { ACTIVE: "On sale", DRAFT: "Draft", ARCHIVED: "Archived" } as const;
const statusTone = { ACTIVE: "bg-leaf-soft text-leaf", DRAFT: "bg-sun-soft text-ink", ARCHIVED: "bg-cream-deep text-muted" } as const;

/* eslint-disable @next/next/no-img-element */
function Products() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [data, setData] = useState<AdminProductPage | null>(null);
  const [categories, setCategories] = useState<CategoryNode[]>([]);
  const [search, setSearch] = useState(params.get("q") ?? "");
  const q = params.get("q") ?? "";
  const category = params.get("category") ?? "";
  const status = params.get("status") ?? "";
  const low = params.get("low") === "1";
  const page = Number(params.get("page") ?? "0");

  useEffect(() => {
    request<CategoryNode[]>("GET", "/categories").then(setCategories).catch(() => undefined);
  }, []);
  useEffect(() => {
    const query = new URLSearchParams({ page: String(page) });
    if (q) query.set("q", q);
    if (category) query.set("category", category);
    if (status) query.set("status", status);
    if (low) query.set("low", "true");
    request<AdminProductPage>("GET", `/admin/products?${query}`).then(setData);
  }, [q, category, status, low, page]);

  const go = (changes: Record<string, string>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)));
    if (!("page" in changes)) next.delete("page");
    router.push(`${pathname}?${next}`);
  };

  return (
    <>
      <PageHeader title="Products" note={data ? `${data.total} product${data.total === 1 ? "" : "s"}` : undefined}>
        <Link href="/admin/products/new" className={btn.primary}><Plus size={16} /> New product</Link>
      </PageHeader>
      <form className="mb-4 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); go({ q: search.trim() }); }}>
        <label className="relative min-w-60 flex-1"><span className="sr-only">Search products</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Name, Bangla name, brand or SKU" className={`${input} pl-9`} /></label>
        <select aria-label="Category" value={category} onChange={(e) => go({ category: e.target.value })} className={`${input} w-auto`}>
          <option value="">All categories</option>
          {categories.map((top) => (
            <optgroup key={top.id} label={top.name}>
              <option value={top.id}>All {top.name}</option>
              {top.children.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </optgroup>
          ))}
        </select>
        <select aria-label="Status" value={status} onChange={(e) => go({ status: e.target.value })} className={`${input} w-auto`}>
          <option value="">Any status</option><option value="ACTIVE">On sale</option><option value="DRAFT">Draft</option><option value="ARCHIVED">Archived</option>
        </select>
        <label className="flex items-center gap-2 rounded-xl bg-white px-3 text-sm font-semibold"><input type="checkbox" checked={low} onChange={(e) => go({ low: e.target.checked ? "1" : "" })} className="h-4 w-4 accent-tangerine" /> Running low</label>
      </form>
      {!data ? <Loading /> : data.items.length === 0 ? <Empty>No products match.</Empty> : (
        <ul className="grid gap-2 sm:grid-cols-2 2xl:grid-cols-3">
          {data.items.map((p) => (
            <li key={p.id}>
              <Link href={`/admin/products/${p.id}`} className="flex gap-3 rounded-2xl border border-line bg-white p-3 transition hover:border-ink/40">
                <span className="h-16 w-16 shrink-0 overflow-hidden rounded-xl bg-cream">{p.image && <img src={p.image} alt="" className="h-full w-full object-contain" />}</span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-2"><span className="truncate font-bold">{p.name}</span><span className={`shrink-0 rounded-md px-1.5 py-0.5 text-[0.6875rem] font-bold ${statusTone[p.status]}`}>{statusLabel[p.status]}</span></span>
                  <span className="block truncate text-xs text-muted">{p.category}{p.brand ? ` · ${p.brand}` : ""}{p.localName ? ` · ${p.localName}` : ""}</span>
                  <span className="mt-1 flex flex-wrap gap-x-3 text-xs">
                    <span>{p.packCount} pack{p.packCount === 1 ? "" : "s"} from <strong>{taka(p.fromPrice)}</strong></span>
                    <span className={p.low ? "font-bold text-berry" : "text-ink-soft"}>{p.stock} in stock{p.low ? " · low" : ""}</span>
                    <span className="text-muted">{p.soldCount} sold</span>
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {data && data.total > data.pageSize && (
        <div className="mt-4 flex items-center justify-center gap-3 text-sm">
          <button type="button" className={btn.ghost} disabled={page === 0} onClick={() => go({ page: String(page - 1) })}>Previous</button>
          <span className="text-muted">Page {page + 1} of {Math.ceil(data.total / data.pageSize)}</span>
          <button type="button" className={btn.ghost} disabled={(page + 1) * data.pageSize >= data.total} onClick={() => go({ page: String(page + 1) })}>Next</button>
        </div>
      )}
    </>
  );
}

export default function ProductsPage() {
  return <Suspense fallback={<Loading />}><Products /></Suspense>;
}
