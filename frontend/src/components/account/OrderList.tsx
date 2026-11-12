"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { CalendarClock, ChevronRight, RotateCcw, ShoppingBasket } from "lucide-react";
import { request } from "@/lib/client";
import { taka } from "@/lib/money";
import { slotText } from "@/lib/slots";
import type { OrderHistory, OrderSummary } from "@/lib/types";
import { StatusPill } from "./StatusPill";
import { useReorder } from "./useReorder";

/* eslint-disable @next/next/no-img-element */
export function OrderList() {
  const [orders, setOrders] = useState<OrderSummary[] | null>(null);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const { reorder, busy } = useReorder();

  useEffect(() => {
    let live = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- loading a page of orders.
    setLoading(true);
    request<OrderHistory>("GET", `/account/orders?page=${page}`)
      .then((h) => {
        if (!live) return;
        setOrders((o) => (page === 0 ? h.items : [...(o ?? []), ...h.items]));
        setTotal(h.total);
      })
      .catch(() => live && setFailed(true))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
  }, [page]);

  if (failed) return <p className="rounded-3xl bg-white p-6 text-sm font-semibold text-berry">We couldn&rsquo;t load your orders. Please refresh the page.</p>;
  if (!orders) return <div className="h-64 animate-pulse rounded-3xl bg-white" aria-busy="true" />;
  if (orders.length === 0) {
    return (
      <div className="grid place-items-center rounded-3xl bg-white px-6 py-14 text-center">
        <span className="grid h-16 w-16 place-items-center rounded-2xl bg-sun-soft"><ShoppingBasket size={28} /></span>
        <h2 className="mt-4 font-display text-2xl font-bold">No orders yet</h2>
        <p className="mt-1 max-w-xs text-sm text-muted">Your orders will show up here, ready to repeat with one tap.</p>
        <Link href="/" className="mt-5 rounded-xl bg-tangerine px-6 py-3 text-sm font-bold text-white">Start shopping</Link>
      </div>
    );
  }

  return (
    <section aria-labelledby="orders-title">
      <h2 id="orders-title" className="sr-only">Your orders</h2>
      <ul className="space-y-3">
        {orders.map((o) => (
          <li key={o.number} className="rounded-3xl bg-white p-4 md:p-5">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <Link href={`/account/orders/${o.number}`} className="font-display text-lg font-bold hover:text-tangerine">{o.number}</Link>
              <StatusPill status={o.status} />
              <span className="ml-auto text-xs text-muted">Placed {new Date(o.placedAt).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}</span>
            </div>
            <div className="mt-3 flex flex-wrap items-center gap-4">
              <div className="flex -space-x-2">
                {o.images.map((src, i) => (
                  <span key={i} className="grid h-12 w-12 place-items-center overflow-hidden rounded-xl border-2 border-white bg-cream"><img src={src} alt="" className="h-full w-full object-contain" /></span>
                ))}
                {o.lineCount > o.images.length && (
                  <span className="grid h-12 w-12 place-items-center rounded-xl border-2 border-white bg-cream-deep text-xs font-bold">+{o.lineCount - o.images.length}</span>
                )}
              </div>
              <div className="min-w-0 flex-1 text-sm">
                <p className="flex items-center gap-1.5 font-semibold"><CalendarClock size={15} className="text-leaf" /> {slotText(o.slot)}</p>
                <p className="text-muted">{o.itemCount} item{o.itemCount === 1 ? "" : "s"} · <strong className="text-ink tabular-nums">{taka(o.total)}</strong></p>
              </div>
              <div className="flex w-full gap-2 sm:w-auto">
                <button type="button" onClick={() => reorder(o.number)} disabled={busy !== null} className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-tangerine px-4 py-2.5 text-sm font-bold text-white hover:bg-tangerine-dark disabled:opacity-60 sm:flex-none">
                  <RotateCcw size={15} /> {busy === o.number ? "Adding…" : "Order again"}
                </button>
                <Link href={`/account/orders/${o.number}`} className="inline-flex flex-1 items-center justify-center gap-0.5 rounded-xl bg-cream px-4 py-2.5 text-sm font-bold hover:bg-cream-deep sm:flex-none">
                  Details <ChevronRight size={15} />
                </Link>
              </div>
            </div>
          </li>
        ))}
      </ul>
      {orders.length < total && (
        <div className="mt-4 text-center">
          <button type="button" onClick={() => setPage((p) => p + 1)} disabled={loading} className="rounded-xl border-2 border-ink px-6 py-2.5 text-sm font-bold disabled:opacity-50">
            {loading ? "Loading…" : "Show older orders"}
          </button>
        </div>
      )}
    </section>
  );
}
