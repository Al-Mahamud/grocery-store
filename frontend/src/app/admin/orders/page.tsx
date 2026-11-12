"use client";

import { Suspense, useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Search } from "lucide-react";
import { request } from "@/lib/client";
import type { AdminOrderPage } from "@/lib/admin-types";
import { Empty, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { OrderTable } from "@/components/admin/OrderTable";

const tabs: { value: string; label: string; count?: string }[] = [
  { value: "OPEN", label: "To do" },
  { value: "PLACED", label: "New", count: "PLACED" },
  { value: "PACKING", label: "Packing", count: "PACKING" },
  { value: "OUT_FOR_DELIVERY", label: "On the way", count: "OUT_FOR_DELIVERY" },
  { value: "PENDING_PAYMENT", label: "Awaiting payment", count: "PENDING_PAYMENT" },
  { value: "DELIVERED", label: "Delivered" },
  { value: "CANCELLED", label: "Cancelled" },
  { value: "REFUND_DUE", label: "Refund due", count: "REFUND_DUE" },
  { value: "ALL", label: "All" },
];

function Orders() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const filter = params.get("filter") ?? "OPEN";
  const date = params.get("date") ?? "";
  const q = params.get("q") ?? "";
  const page = Number(params.get("page") ?? "0");
  const [data, setData] = useState<AdminOrderPage | null>(null);
  const [search, setSearch] = useState(q);

  useEffect(() => {
    const query = new URLSearchParams({ filter, page: String(page) });
    if (date) query.set("date", date);
    if (q) query.set("q", q);
    request<AdminOrderPage>("GET", `/admin/orders?${query}`).then(setData).catch(() => setData(null));
  }, [filter, date, q, page]);

  const go = (changes: Record<string, string>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)));
    if (!("page" in changes)) next.delete("page");
    router.push(`${pathname}?${next}`);
  };

  return (
    <>
      <PageHeader title="Orders" note={filter === "OPEN" ? "Everything still to pack or deliver, in slot order." : undefined} />
      <div className="no-scrollbar -mx-4 mb-3 flex gap-1.5 overflow-x-auto px-4 md:mx-0 md:flex-wrap md:px-0">
        {tabs.map((t) => {
          const count = t.count ? data?.counts[t.count] : undefined;
          return (
            <button key={t.value} type="button" onClick={() => go({ filter: t.value })} aria-pressed={filter === t.value} className={`inline-flex shrink-0 items-center gap-1.5 rounded-xl px-3 py-2 text-sm font-bold transition ${filter === t.value ? "bg-ink text-white" : "bg-white text-ink-soft hover:text-ink"}`}>
              {t.label}
              {count ? <span className={`rounded-md px-1.5 text-xs ${filter === t.value ? "bg-white/20" : t.value === "REFUND_DUE" ? "bg-berry text-white" : "bg-cream-deep"}`}>{count}</span> : null}
            </button>
          );
        })}
      </div>
      <form className="mb-4 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); go({ q: search.trim() }); }}>
        <label className="relative min-w-60 flex-1"><span className="sr-only">Search orders</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Order number, phone or name" className={`${input} pl-9`} /></label>
        <label className="flex items-center gap-2 text-sm font-semibold"><span className="text-muted">Slot day</span><input type="date" value={date} onChange={(e) => go({ date: e.target.value })} className={`${input} w-auto`} /></label>
        <button type="submit" className={btn.dark}>Search</button>
        {(q || date) && <button type="button" onClick={() => { setSearch(""); go({ q: "", date: "" }); }} className={btn.ghost}>Clear</button>}
      </form>
      {!data ? <Loading /> : data.items.length === 0 ? <Empty>No orders here.</Empty> : <OrderTable rows={data.items} />}
      {data && data.total > data.pageSize && (
        <div className="mt-4 flex items-center justify-center gap-3 text-sm">
          <button type="button" className={btn.ghost} disabled={page === 0} onClick={() => go({ page: String(page - 1) })}>Newer</button>
          <span className="text-muted">Page {page + 1} of {Math.ceil(data.total / data.pageSize)}</span>
          <button type="button" className={btn.ghost} disabled={(page + 1) * data.pageSize >= data.total} onClick={() => go({ page: String(page + 1) })}>Older</button>
        </div>
      )}
    </>
  );
}

export default function OrdersPage() {
  return <Suspense fallback={<Loading />}><Orders /></Suspense>;
}
