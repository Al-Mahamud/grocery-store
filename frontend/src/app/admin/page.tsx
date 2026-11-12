"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { request } from "@/lib/client";
import type { Dashboard } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { day } from "@/lib/format";
import { Card, Loading, PageHeader, Stat } from "@/components/admin/ui";
import { SlotLoad } from "@/components/admin/SlotLoad";
import { OrderTable } from "@/components/admin/OrderTable";
import { useAdmin } from "@/components/admin/AdminShell";

export default function DashboardPage() {
  const { staff } = useAdmin();
  const [data, setData] = useState<Dashboard | null>(null);
  useEffect(() => {
    const load = () => request<Dashboard>("GET", "/admin/dashboard").then(setData).catch(() => undefined);
    void load();
    const timer = setInterval(load, 60_000);
    return () => clearInterval(timer);
  }, []);
  if (!data) return <Loading />;
  const best = Math.max(1, ...data.week.map((d) => d.revenue));
  const hello = new Date().getHours() < 12 ? "Good morning" : new Date().getHours() < 17 ? "Good afternoon" : "Good evening";

  return (
    <>
      <PageHeader title={`${hello}, ${staff.name.split(" ")[0]}`} note="Today at Haat. This page refreshes every minute." />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Orders today" value={data.ordersToday} note={`Average basket ${taka(data.averageBasket)}`} />
        <Stat label="Sales today" value={taka(data.revenueToday)} note="Placed orders, not cancelled" />
        <Stat label="To pack" value={(data.open.PLACED ?? 0) + (data.open.PACKING ?? 0)} note={`${data.open.PLACED ?? 0} new · ${data.open.PACKING ?? 0} packing`} href="/admin/orders" tone={(data.open.PLACED ?? 0) > 0 ? "warn" : "plain"} />
        <Stat label="On the way" value={data.open.OUT_FOR_DELIVERY ?? 0} note={`${data.open.PENDING_PAYMENT ?? 0} awaiting payment`} href="/admin/orders?filter=OUT_FOR_DELIVERY" />
      </div>
      {(data.refundsDue > 0 || data.lowStock > 0) && (
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          {data.refundsDue > 0 && <Stat label="Refunds due" value={data.refundsDue} note="Paid online, then cancelled" href="/admin/orders?filter=REFUND_DUE" tone="warn" />}
          {data.lowStock > 0 && <Stat label="Running low" value={data.lowStock} note="Packs at or below the low-stock level" href="/admin/stock?low=1" tone="warn" />}
        </div>
      )}

      <div className="mt-5 grid gap-4 xl:grid-cols-3">
        <Card title="Delivery slots" action={<Link href="/admin/picking" className="text-sm font-bold text-tangerine">Picking list →</Link>} className="xl:col-span-2">
          <div className="grid gap-5 md:grid-cols-2">
            {data.slots.map((d) => (
              <div key={d.date}>
                <p className="mb-2 text-sm font-bold">{d.label}{(d.label === "Today" || d.label === "Tomorrow") && <span className="font-normal text-muted"> · {day(d.date)}</span>}</p>
                <div className="space-y-2.5">{d.slots.map((s) => <SlotLoad key={s.start} slot={s} />)}</div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Last 7 days">
          <ul className="space-y-2">
            {data.week.map((d) => (
              <li key={d.date} className="grid grid-cols-[4.5rem_1fr_5rem] items-center gap-2 text-xs">
                <span className="font-semibold text-ink-soft">{day(d.date).slice(0, 6)}</span>
                <span className="h-5 overflow-hidden rounded-md bg-cream"><span className="block h-full rounded-md bg-ink" style={{ width: `${(d.revenue / best) * 100}%` }} /></span>
                <span className="text-right tabular-nums"><strong>{taka(d.revenue)}</strong><span className="block text-muted">{d.orders} order{d.orders === 1 ? "" : "s"}</span></span>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <div className="mt-5">
        <div className="mb-3 flex items-center justify-between"><h2 className="font-display text-lg font-bold">Latest orders</h2><Link href="/admin/orders?filter=ALL" className="text-sm font-bold text-tangerine">All orders →</Link></div>
        <OrderTable rows={data.recent} />
      </div>
    </>
  );
}
