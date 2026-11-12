import Link from "next/link";
import type { AdminRow } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { dateTime, day, hour } from "@/lib/format";
import { StatusPill } from "@/components/account/StatusPill";
import { PaymentBadge } from "./ui";

/** Orders as rows on wide screens and as cards on phones. */
export function OrderTable({ rows }: { rows: AdminRow[] }) {
  return (
    <>
      <div className="hidden overflow-hidden rounded-2xl border border-line bg-white md:block">
        <table className="w-full text-sm">
          <thead className="bg-cream text-left text-xs uppercase tracking-wide text-muted">
            <tr>
              <th className="px-4 py-3 font-bold">Order</th>
              <th className="px-4 py-3 font-bold">Customer</th>
              <th className="px-4 py-3 font-bold">Slot</th>
              <th className="px-4 py-3 font-bold">Status</th>
              <th className="px-4 py-3 font-bold">Payment</th>
              <th className="px-4 py-3 text-right font-bold">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {rows.map((o) => (
              <tr key={o.number} className="hover:bg-cream/60">
                <td className="px-4 py-3">
                  <Link href={`/admin/orders/${o.number}`} className="font-bold text-ink hover:text-tangerine">{o.number}</Link>
                  <span className="block text-xs text-muted">{dateTime(o.placedAt)}</span>
                </td>
                <td className="px-4 py-3"><span className="block font-semibold">{o.name}</span><span className="text-xs text-muted">{o.phone} · {o.area}</span></td>
                <td className="px-4 py-3 whitespace-nowrap"><span className="block font-semibold">{day(o.slot.date)}</span><span className="text-xs text-muted">{hour(o.slot.start)}–{hour(o.slot.end)}</span></td>
                <td className="px-4 py-3"><StatusPill status={o.status} /></td>
                <td className="px-4 py-3"><PaymentBadge method={o.paymentMethod} status={o.paymentStatus} /></td>
                <td className="px-4 py-3 text-right"><span className="font-bold tabular-nums">{taka(o.total)}</span><span className="block text-xs text-muted">{o.itemCount} items</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul className="space-y-2 md:hidden">
        {rows.map((o) => (
          <li key={o.number}>
            <Link href={`/admin/orders/${o.number}`} className="block rounded-2xl border border-line bg-white p-4">
              <div className="flex items-center gap-2"><span className="font-bold">{o.number}</span><StatusPill status={o.status} /><span className="ml-auto font-bold tabular-nums">{taka(o.total)}</span></div>
              <p className="mt-1 text-sm">{o.name} · {o.area}</p>
              <p className="text-xs text-muted">{day(o.slot.date)}, {hour(o.slot.start)}–{hour(o.slot.end)} · <PaymentBadge method={o.paymentMethod} status={o.paymentStatus} /></p>
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}
