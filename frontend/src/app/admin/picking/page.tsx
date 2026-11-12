"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { PackageCheck, Printer } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { BoardDay, Picking } from "@/lib/admin-types";
import { taka } from "@/lib/money";
import { day, hour } from "@/lib/format";
import { StatusPill } from "@/components/account/StatusPill";
import { Card, Empty, Loading, PageHeader, btn } from "@/components/admin/ui";
import { useAdmin } from "@/components/admin/AdminShell";

const substitution = { CALL: "Call first", REPLACE: "Replace OK", REMOVE: "Remove" } as const;

/* eslint-disable @next/next/no-img-element */
export default function PickingPage() {
  const { notify } = useAdmin();
  const [days, setDays] = useState<BoardDay[] | null>(null);
  const [chosen, setChosen] = useState<{ date: string; start: string } | null>(null);
  const [list, setList] = useState<Picking | null>(null);
  const [picked, setPicked] = useState<Record<string, boolean>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    request<{ days: BoardDay[] }>("GET", "/admin/slots?days=3").then((r) => {
      setDays(r.days);
      // Start at the next slot that has orders.
      const first = r.days.flatMap((d) => d.slots.filter((s) => !s.past && s.booked > 0).map((s) => ({ date: d.date, start: s.start })))[0];
      const today = r.days[0];
      const fallback = today?.slots.find((s) => !s.past);
      setChosen(first ?? (today && fallback ? { date: today.date, start: fallback.start } : null));
    });
  }, []);

  const load = useCallback(() => {
    if (!chosen) return;
    setList(null);
    request<Picking>("GET", `/admin/picking?date=${chosen.date}&start=${chosen.start.slice(0, 5)}`).then(setList);
  }, [chosen]);
  // eslint-disable-next-line react-hooks/set-state-in-effect -- load the chosen slot's list.
  useEffect(load, [load]);

  const byCategory = useMemo(() => {
    const groups = new Map<string, Picking["lines"]>();
    list?.lines.forEach((l) => groups.set(l.category, [...(groups.get(l.category) ?? []), l]));
    return [...groups.entries()];
  }, [list]);

  const placed = list?.orders.filter((o) => o.status === "PLACED").length ?? 0;
  const done = list ? list.lines.filter((l) => picked[l.sku]).length : 0;

  return (
    <>
      <PageHeader title="Picking list" note="Walk the shelves once per slot, then pack each order.">
        <button type="button" onClick={() => window.print()} className={btn.ghost}><Printer size={15} /> Print</button>
        {placed > 0 && (
          <button
            type="button"
            disabled={busy}
            onClick={async () => {
              if (!chosen) return;
              setBusy(true);
              try {
                const r = await request<{ moved: number }>("POST", `/admin/picking/start?date=${chosen.date}&start=${chosen.start.slice(0, 5)}`);
                notify(`${r.moved} order${r.moved === 1 ? "" : "s"} moved to packing`);
                load();
              } catch (e) {
                notify(e instanceof ApiError ? e.message : "Something went wrong.");
              } finally {
                setBusy(false);
              }
            }}
            className={btn.primary}
          >
            <PackageCheck size={16} /> Start packing all {placed}
          </button>
        )}
      </PageHeader>

      {!days ? <Loading /> : (
        <div className="mb-5 space-y-2 print:hidden">
          {days.map((d) => (
            <div key={d.date} className="flex flex-wrap items-center gap-1.5">
              <span className="w-28 text-sm font-bold">{d.label}</span>
              {d.slots.map((s) => {
                const on = chosen?.date === d.date && chosen.start === s.start;
                return (
                  <button key={s.start} type="button" disabled={s.past && s.booked === 0} onClick={() => { setChosen({ date: d.date, start: s.start }); setPicked({}); }} aria-pressed={on}
                    className={`rounded-xl px-3 py-1.5 text-xs font-bold transition disabled:opacity-35 ${on ? "bg-ink text-white" : s.booked > 0 ? "bg-white text-ink ring-1 ring-tangerine/50" : "bg-white text-muted"}`}>
                    {s.label} <span className={on ? "text-sun" : "text-tangerine"}>{s.booked || ""}</span>
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      )}

      {chosen && <h2 className="mb-3 font-display text-xl font-bold">{day(chosen.date)}, {hour(chosen.start)}–{list ? hour(list.end) : ""} {list && <span className="text-sm font-semibold text-muted">· {list.orders.length} orders · picked {done}/{list.lines.length}</span>}</h2>}
      {!list ? (chosen ? <Loading /> : <Empty>No slots left today.</Empty>) : list.orders.length === 0 ? <Empty>No orders to pack in this slot.</Empty> : (
        <div className="grid gap-4 xl:grid-cols-5">
          <Card title="Pick by aisle" className="xl:col-span-3">
            <div className="space-y-4">
              {byCategory.map(([category, lines]) => (
                <div key={category}>
                  <p className="mb-1 text-xs font-bold uppercase tracking-wide text-muted">{category}</p>
                  <ul className="divide-y divide-line rounded-xl border border-line">
                    {lines.map((l) => (
                      <li key={l.sku}>
                        <label className={`flex cursor-pointer items-center gap-3 px-3 py-2 transition ${picked[l.sku] ? "bg-leaf-soft/60" : ""}`}>
                          <input type="checkbox" checked={!!picked[l.sku]} onChange={(e) => setPicked((p) => ({ ...p, [l.sku]: e.target.checked }))} className="h-5 w-5 accent-leaf" />
                          <span className="h-9 w-9 shrink-0 overflow-hidden rounded-lg bg-white">{l.image && <img src={l.image} alt="" className="h-full w-full object-contain" />}</span>
                          <span className={`min-w-0 flex-1 ${picked[l.sku] ? "text-muted line-through" : ""}`}>
                            <span className="block text-sm font-semibold">{l.name} <span className="font-normal text-muted">· {l.packLabel}</span></span>
                            <span className="block truncate text-xs text-muted">{l.orders.join(", ")}</span>
                          </span>
                          <span className="rounded-lg bg-ink px-2.5 py-1 font-display text-lg font-extrabold tabular-nums text-white">{l.quantity}</span>
                        </label>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </Card>
          <div className="space-y-3 xl:col-span-2">
            {list.orders.map((o) => (
              <Card key={o.number} className="break-inside-avoid">
                <div className="flex flex-wrap items-center gap-2">
                  <Link href={`/admin/orders/${o.number}`} className="font-bold hover:text-tangerine">{o.number}</Link>
                  <StatusPill status={o.status} />
                  <span className="ml-auto text-sm font-bold tabular-nums">{taka(o.total)}</span>
                </div>
                <p className="mt-1 text-sm font-semibold">{o.name} · {o.phone}</p>
                <p className="text-xs text-muted">{o.address}, {o.area}</p>
                <p className="mt-1 text-xs"><span className="rounded-md bg-cream px-1.5 py-0.5 font-bold">{substitution[o.substitution]}</span> {o.paymentMethod === "COD" && o.paymentStatus === "UNPAID" ? <span className="rounded-md bg-sun-soft px-1.5 py-0.5 font-bold">Collect {taka(o.total)}</span> : <span className="rounded-md bg-leaf-soft px-1.5 py-0.5 font-bold text-leaf">Paid</span>}</p>
                {o.note && <p className="mt-1 text-xs italic">“{o.note}”</p>}
                <ul className="mt-2 space-y-0.5 border-t border-line pt-2 text-sm">
                  {o.items.map((i, n) => <li key={n} className="flex justify-between gap-2"><span>{i.name} <span className="text-muted">· {i.packLabel}</span></span><strong className="tabular-nums">×{i.quantity}</strong></li>)}
                </ul>
              </Card>
            ))}
          </div>
        </div>
      )}
    </>
  );
}
