import Link from "next/link";
import { Check } from "lucide-react";
import type { OrderView } from "@/lib/types";
import { taka } from "@/lib/money";
import { slotText } from "@/lib/slots";

const steps = [
  { status: "PLACED", label: "Placed" },
  { status: "PACKING", label: "Packing" },
  { status: "OUT_FOR_DELIVERY", label: "On the way" },
  { status: "DELIVERED", label: "Delivered" },
] as const;

const substitutionText = { CALL: "We'll call you", REPLACE: "Replace with similar", REMOVE: "Remove from order" } as const;

export function StatusTimeline({ order }: { order: OrderView }) {
  if (order.status === "CANCELLED") return <p className="rounded-xl bg-berry/10 p-3 text-sm font-semibold text-berry">This order was cancelled.</p>;
  const reached = steps.findIndex((s) => s.status === order.status);
  return (
    <ol className="grid grid-cols-4 gap-1">
      {steps.map((step, i) => (
        <li key={step.status} className="text-center">
          <span className={`mx-auto grid h-9 w-9 place-items-center rounded-xl ${i <= reached ? "bg-leaf text-white" : "bg-cream text-muted"}`}>{i <= reached ? <Check size={16} strokeWidth={3} /> : i + 1}</span>
          <span className={`mt-1.5 block text-xs font-bold ${i <= reached ? "" : "text-muted"}`}>{step.label}</span>
          <span className={`mx-auto mt-2 block h-1 rounded-full ${i <= reached ? "bg-leaf" : "bg-cream"}`} />
        </li>
      ))}
    </ol>
  );
}

/* eslint-disable @next/next/no-img-element */
export function OrderDetails({ order }: { order: OrderView }) {
  return (
    <div className="grid gap-4 md:grid-cols-5">
      <div className="rounded-3xl bg-white p-5 md:col-span-3 md:p-6">
        <h2 className="font-display text-xl font-bold">Items</h2>
        <ul className="mt-3 divide-y divide-line">
          {order.items.map((i) => (
            <li key={`${i.slug}-${i.packLabel}`} className="flex items-center gap-3 py-2.5">
              <Link href={`/p/${i.slug}`} className="h-12 w-12 shrink-0 rounded-lg border border-line">{i.image && <img src={i.image} alt="" className="h-full w-full object-contain" />}</Link>
              <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{i.name}</span><span className="text-xs text-muted">{i.packLabel} × {i.quantity}</span></span>
              <span className="text-sm font-bold tabular-nums">{taka(i.lineTotal)}</span>
            </li>
          ))}
        </ul>
        <dl className="mt-3 space-y-1.5 border-t border-line pt-3 text-sm">
          <div className="flex justify-between"><dt className="text-muted">Items</dt><dd className="tabular-nums">{taka(order.subtotal)}</dd></div>
          {order.discount > 0 && <div className="flex justify-between text-leaf"><dt>Promo ({order.couponCode})</dt><dd className="tabular-nums">−{taka(order.discount)}</dd></div>}
          <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="tabular-nums">{order.deliveryFee === 0 ? "Free" : taka(order.deliveryFee)}</dd></div>
          <div className="flex justify-between pt-1 text-base font-bold"><dt>Total</dt><dd className="tabular-nums">{taka(order.total)}</dd></div>
        </dl>
      </div>
      <div className="space-y-4 md:col-span-2">
        <div className="rounded-3xl bg-white p-5 text-sm md:p-6">
          <h2 className="font-display text-xl font-bold">Delivery</h2>
          <p className="mt-2 font-bold text-leaf">{slotText(order.slot)}</p>
          <p className="mt-2 font-semibold">{order.name} · {order.phone}</p>
          <p className="text-ink-soft">{order.address}, {order.area}</p>
          {order.note && <p className="mt-1 text-xs italic text-muted">“{order.note}”</p>}
        </div>
        <div className="rounded-3xl bg-white p-5 text-sm md:p-6">
          <p><span className="text-muted">If out of stock:</span> <strong>{substitutionText[order.substitution]}</strong></p>
          <p className="mt-1"><span className="text-muted">Payment:</span> <strong>{order.paymentMethod === "COD" ? `Cash on delivery (${taka(order.total)})` : "Online"}</strong></p>
        </div>
      </div>
    </div>
  );
}
