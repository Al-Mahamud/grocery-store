"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Check, Search } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { StockRow } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { Empty, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

/* eslint-disable @next/next/no-img-element */
function Stock() {
  const { notify } = useAdmin();
  const params = useSearchParams();
  const [low, setLow] = useState(params.get("low") === "1");
  const [q, setQ] = useState("");
  const [search, setSearch] = useState("");
  const [rows, setRows] = useState<StockRow[] | null>(null);
  const [edits, setEdits] = useState<Record<number, string>>({});
  const [saving, setSaving] = useState<number | null>(null);

  const load = useCallback(() => {
    const query = new URLSearchParams({ low: String(low) });
    if (q) query.set("q", q);
    request<StockRow[]>("GET", `/admin/stock?${query}`).then(setRows);
  }, [low, q]);
  useEffect(load, [load]);

  const save = async (row: StockRow) => {
    const value = edits[row.packId];
    if (value === undefined) return;
    setSaving(row.packId);
    try {
      const updated = await request<StockRow>("PATCH", `/admin/packs/${row.packId}/stock`, { stock: Number(value) });
      setRows((r) => r?.map((x) => (x.packId === row.packId ? updated : x)) ?? null);
      setEdits((e) => { const next = { ...e }; delete next[row.packId]; return next; });
      notify(`${row.product} ${row.label}: ${updated.stock} in stock`);
    } catch (e) {
      notify(e instanceof ApiError ? Object.values(e.fields)[0] ?? e.message : "Couldn't save.");
    } finally {
      setSaving(null);
    }
  };

  return (
    <>
      <PageHeader title="Stock" note="Type the count on the shelf and press Enter. Low packs come first." />
      <form className="mb-4 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); setQ(search.trim()); }}>
        <label className="relative min-w-60 flex-1"><span className="sr-only">Search</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Product or SKU" className={`${input} pl-9`} /></label>
        <label className="flex items-center gap-2 rounded-xl bg-white px-3 text-sm font-semibold"><input type="checkbox" checked={low} onChange={(e) => setLow(e.target.checked)} className="h-4 w-4 accent-tangerine" /> Running low only</label>
      </form>
      {!rows ? <Loading /> : rows.length === 0 ? <Empty>{low ? "Nothing is running low." : "No packs match."}</Empty> : (
        <ul className="divide-y divide-line overflow-hidden rounded-2xl border border-line bg-white">
          {rows.map((r) => {
            const value = edits[r.packId] ?? String(r.stock);
            const changed = edits[r.packId] !== undefined && Number(edits[r.packId]) !== r.stock;
            const tone = r.stock === 0 ? "bg-berry text-white" : r.stock <= 10 ? "bg-tangerine-soft text-tangerine-dark" : "bg-cream text-ink-soft";
            return (
              <li key={r.packId} className="flex flex-wrap items-center gap-3 px-3 py-2.5 md:px-4">
                <span className="h-10 w-10 shrink-0 overflow-hidden rounded-lg bg-cream">{r.image && <img src={r.image} alt="" className="h-full w-full object-contain" />}</span>
                <Link href={`/admin/products/${r.productId}`} className="min-w-0 flex-1 hover:text-tangerine">
                  <span className="block truncate text-sm font-semibold">{r.product} <span className="font-normal text-muted">· {r.label}</span></span>
                  <span className="text-xs text-muted">{r.sku} · {taka(r.price)}{r.status === "DRAFT" ? " · draft" : ""}</span>
                </Link>
                <span className={`rounded-md px-2 py-0.5 text-xs font-bold ${tone}`}>{r.stock === 0 ? "Sold out" : r.stock <= 10 ? "Low" : "OK"}</span>
                <form className="flex items-center gap-1.5" onSubmit={(e) => { e.preventDefault(); void save(r); }}>
                  <label className="sr-only" htmlFor={`stock-${r.packId}`}>Stock for {r.product} {r.label}</label>
                  <input id={`stock-${r.packId}`} inputMode="numeric" value={value} onChange={(e) => setEdits((x) => ({ ...x, [r.packId]: e.target.value.replace(/[^0-9]/g, "") }))} className={`${input} w-20 text-right tabular-nums ${changed ? "border-tangerine" : ""}`} />
                  <button type="submit" disabled={!changed || saving === r.packId} aria-label="Save stock" className={`${btn.dark} px-3`}><Check size={15} /></button>
                </form>
              </li>
            );
          })}
        </ul>
      )}
    </>
  );
}

export default function StockPage() {
  return <Suspense fallback={<Loading />}><Stock /></Suspense>;
}
