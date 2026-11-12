"use client";

import { use, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Check, MessageSquarePlus, Phone, Printer, Undo2, X } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { AdminOrderDetail, OrderStatus } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { dateTime } from "@/lib/format";
import { slotText } from "@/lib/slots";
import { StatusPill } from "@/components/account/StatusPill";
import { Modal } from "@/components/ui/Modal";
import { Card, Loading, PaymentBadge, btn, input } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

const action: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "",
  PLACED: "",
  PACKING: "Start packing",
  OUT_FOR_DELIVERY: "Hand to rider",
  DELIVERED: "Mark delivered",
  CANCELLED: "Cancel order",
};

const substitution = { CALL: "Call the customer", REPLACE: "Replace with similar", REMOVE: "Remove from order" } as const;

/* eslint-disable @next/next/no-img-element */
export default function AdminOrderPage({ params }: { params: Promise<{ number: string }> }) {
  const { number } = use(params);
  const { notify } = useAdmin();
  const [data, setData] = useState<AdminOrderDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [note, setNote] = useState("");
  const [restock, setRestock] = useState(true);
  const [reference, setReference] = useState("");

  const load = useCallback(() => {
    request<AdminOrderDetail>("GET", `/admin/orders/${number}`)
      .then(setData)
      .catch((e) => setError(e instanceof ApiError && e.status === 404 ? "There's no order with that number." : "Couldn't load the order."));
  }, [number]);
  useEffect(load, [load]);

  const send = async (path: string, body: unknown, done: string) => {
    setBusy(true);
    try {
      setData(await request<AdminOrderDetail>("POST", `/admin/orders/${number}${path}`, body));
      notify(done);
      return true;
    } catch (e) {
      notify(e instanceof ApiError ? e.message : "Something went wrong.");
      return false;
    } finally {
      setBusy(false);
    }
  };

  if (error) return <p className="rounded-2xl bg-white p-6 font-semibold text-berry">{error}</p>;
  if (!data) return <Loading />;
  const o = data.order;
  const forward = data.next.filter((s) => s !== "CANCELLED");

  return (
    <div className="space-y-4">
      <Link href="/admin/orders" className="inline-flex items-center gap-1.5 text-sm font-bold text-ink-soft hover:text-ink print:hidden"><ArrowLeft size={15} /> Orders</Link>
      <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-line bg-white p-4 md:p-5">
        <div>
          <p className="text-xs font-bold text-muted">Placed {dateTime(o.placedAt)}</p>
          <h1 className="font-display text-3xl font-extrabold">{o.number}</h1>
        </div>
        <StatusPill status={o.status} />
        <PaymentBadge method={o.paymentMethod} status={o.paymentStatus} />
        <div className="ml-auto flex flex-wrap gap-2 print:hidden">
          <button type="button" onClick={() => window.print()} className={btn.ghost}><Printer size={15} /> Print</button>
          {data.next.includes("CANCELLED") && <button type="button" onClick={() => setCancelling(true)} className={btn.ghost}><X size={15} /> Cancel</button>}
          {forward.map((s) => (
            <button key={s} type="button" disabled={busy} onClick={() => send("/status", { status: s }, `${o.number}: ${action[s].toLowerCase()} — done`)} className={btn.primary}>
              <Check size={15} /> {action[s]}{s === "DELIVERED" && o.paymentMethod === "COD" && o.paymentStatus === "UNPAID" ? ` · collect ${taka(o.total)}` : ""}
            </button>
          ))}
        </div>
      </div>

      {data.refundDue && (
        <Card title="Refund due" className="border-berry/40 bg-berry/5">
          <p className="text-sm">This order was paid {taka(o.total)} online and then cancelled. Refund it in the SSLCommerz merchant panel, then record the refund reference here.</p>
          <form className="mt-3 flex flex-wrap gap-2" onSubmit={async (e) => { e.preventDefault(); if (await send("/refund", { reference }, "Refund recorded")) setReference(""); }}>
            <input value={reference} onChange={(e) => setReference(e.target.value)} placeholder="Refund reference" className={`${input} max-w-xs`} />
            <button type="submit" disabled={busy || !reference.trim()} className={btn.danger}><Undo2 size={15} /> Mark refunded</button>
          </form>
        </Card>
      )}
      {data.refundedAt && <p className="rounded-2xl bg-cream-deep px-4 py-3 text-sm">Refunded {dateTime(data.refundedAt)} · reference <strong>{data.refundReference}</strong></p>}

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title={`Items (${o.items.reduce((n, i) => n + i.quantity, 0)})`} className="lg:col-span-2">
          <ul className="divide-y divide-line">
            {o.items.map((i) => (
              <li key={`${i.slug}-${i.packLabel}`} className="flex items-center gap-3 py-2.5">
                <span className="h-11 w-11 shrink-0 overflow-hidden rounded-lg border border-line">{i.image && <img src={i.image} alt="" className="h-full w-full object-contain" />}</span>
                <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{i.name}</span><span className="text-xs text-muted">{i.packLabel} · {taka(i.unitPrice)} each</span></span>
                <span className="rounded-lg bg-cream px-2.5 py-1 text-sm font-extrabold tabular-nums">×{i.quantity}</span>
                <span className="w-20 text-right text-sm font-bold tabular-nums">{taka(i.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <dl className="mt-2 space-y-1 border-t border-line pt-3 text-sm">
            <div className="flex justify-between"><dt className="text-muted">Items</dt><dd className="tabular-nums">{taka(o.subtotal)}</dd></div>
            {o.discount > 0 && <div className="flex justify-between text-leaf"><dt>Coupon {o.couponCode}</dt><dd className="tabular-nums">−{taka(o.discount)}</dd></div>}
            <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="tabular-nums">{o.deliveryFee === 0 ? "Free" : taka(o.deliveryFee)}</dd></div>
            <div className="flex justify-between text-base font-bold"><dt>Total</dt><dd className="tabular-nums">{taka(o.total)}</dd></div>
          </dl>
        </Card>
        <div className="space-y-4">
          <Card title="Delivery">
            <p className="font-bold text-leaf">{slotText(o.slot)}</p>
            <p className="mt-2 font-semibold">{o.name}</p>
            <a href={`tel:${o.phone}`} className="inline-flex items-center gap-1 text-sm font-semibold text-tangerine"><Phone size={13} /> {o.phone}</a>
            <p className="text-sm text-ink-soft">{o.address}, {o.area}</p>
            {o.note && <p className="mt-2 rounded-lg bg-sun-soft px-3 py-2 text-sm">“{o.note}”</p>}
            <p className="mt-3 text-sm"><span className="text-muted">If out of stock:</span> <strong>{substitution[o.substitution]}</strong></p>
            <p className="mt-1 text-xs text-muted">{data.customerId ? "Has an account" : "Guest"} · {data.customerOrders} order{data.customerOrders === 1 ? "" : "s"} with this number or account</p>
          </Card>
          {data.attempts.length > 0 && (
            <Card title="Online payment">
              <ul className="space-y-2 text-sm">
                {data.attempts.map((a) => (
                  <li key={a.tranId} className="flex items-center justify-between gap-2">
                    <span><span className="block font-mono text-xs">{a.tranId}</span><span className="text-xs text-muted">{dateTime(a.at)}{a.cardType ? ` · ${a.cardType}` : ""}</span></span>
                    <span className={`rounded-md px-2 py-0.5 text-xs font-bold ${a.status === "PAID" ? "bg-leaf-soft text-leaf" : "bg-cream text-ink-soft"}`}>{a.status.toLowerCase()}</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
          <Card title="History">
            <ol className="space-y-3 border-l-2 border-line pl-4">
              {[...o.events].reverse().map((e, i) => (
                <li key={i} className="relative text-sm">
                  <span className="absolute -left-[1.4rem] top-1 h-2.5 w-2.5 rounded-full bg-ink" />
                  <span className="font-bold">{e.status.replaceAll("_", " ").toLowerCase()}</span>
                  <span className="block text-xs text-muted">{dateTime(e.at)}</span>
                  {e.note && <span className="block text-ink-soft">{e.note}</span>}
                </li>
              ))}
            </ol>
            <form className="mt-4 flex gap-2 print:hidden" onSubmit={async (e) => { e.preventDefault(); if (await send("/notes", { note }, "Note added")) setNote(""); }}>
              <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Add a note for the team" className={input} />
              <button type="submit" aria-label="Add note" disabled={busy || !note.trim()} className={btn.dark}><MessageSquarePlus size={16} /></button>
            </form>
          </Card>
        </div>
      </div>

      <Modal open={cancelling} onClose={() => setCancelling(false)} title={`Cancel ${o.number}?`}>
        <div className="space-y-3 text-sm">
          {o.paymentStatus === "PAID" && <p className="rounded-xl bg-berry/10 p-3 font-semibold text-berry">This order is paid. It will show as a refund due.</p>}
          <label className="block"><span className="mb-1 block text-xs font-bold text-ink-soft">Reason (the team sees this)</span><input value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Customer asked to cancel" className={input} /></label>
          <label className="flex items-center gap-2 font-semibold"><input type="checkbox" checked={restock} onChange={(e) => setRestock(e.target.checked)} className="h-4 w-4 accent-tangerine" /> Put the items back in stock</label>
          <p className="text-xs text-muted">Untick if the goods can&rsquo;t be sold again (e.g. returned fish).</p>
          <div className="flex gap-2 pt-2">
            <button type="button" disabled={busy} onClick={async () => { if (await send("/status", { status: "CANCELLED", note, restock }, `${o.number} cancelled`)) { setCancelling(false); setNote(""); } }} className={btn.danger}>Cancel order</button>
            <button type="button" onClick={() => setCancelling(false)} className={btn.ghost}>Keep it</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
