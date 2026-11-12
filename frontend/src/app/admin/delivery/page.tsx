"use client";

import { useEffect, useState } from "react";
import { MapPin, Plus } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { AdminArea, BoardDay, BoardSlot } from "@/lib/admin-types";
import { day } from "@/lib/format";
import { Modal } from "@/components/ui/Modal";
import { Card, Loading, PageHeader, btn, input } from "@/components/admin/ui";
import { SlotLoad } from "@/components/admin/SlotLoad";
import { useAdmin } from "@/components/admin/AdminShell";

type Board = { usualCapacity: number; days: BoardDay[] };

export default function DeliveryPage() {
  const { notify } = useAdmin();
  const [board, setBoard] = useState<Board | null>(null);
  const [areas, setAreas] = useState<AdminArea[] | null>(null);
  const [slot, setSlot] = useState<{ date: string; slot: BoardSlot } | null>(null);
  const [capacity, setCapacity] = useState("");
  const [note, setNote] = useState("");
  const [newArea, setNewArea] = useState("");
  const [areaError, setAreaError] = useState<string | null>(null);

  const loadAreas = () => request<AdminArea[]>("GET", "/admin/areas").then(setAreas);
  useEffect(() => {
    request<Board>("GET", "/admin/slots?days=7").then(setBoard);
    void loadAreas();
  }, []);

  const saveSlot = async (value: number | null) => {
    if (!slot) return;
    try {
      setBoard(await request<Board>("PUT", "/admin/slots", { date: slot.date, start: slot.slot.start.slice(0, 5), capacity: value, note: note || null }));
      notify(value === null ? "Back to the usual capacity" : value === 0 ? "Slot closed" : `Capacity set to ${value}`);
      setSlot(null);
    } catch (e) {
      notify(e instanceof ApiError ? Object.values(e.fields)[0] ?? e.message : "Couldn't save.");
    }
  };

  const saveArea = async (id: number | null, name: string, active: boolean) => {
    try {
      await request(id ? "PUT" : "POST", id ? `/admin/areas/${id}` : "/admin/areas", { name, active });
      setNewArea("");
      setAreaError(null);
      notify(id ? (active ? `${name} is on` : `${name} is off — no new deliveries there`) : `${name} added`);
      void loadAreas();
    } catch (e) {
      setAreaError(e instanceof ApiError ? Object.values(e.fields)[0] ?? e.message : "Couldn't save.");
    }
  };

  return (
    <>
      <PageHeader title="Delivery" note="Where we deliver, and how many orders each two-hour slot can take." />
      <Card title="Slots for the next 7 days" action={board && <span className="text-xs text-muted">Usual capacity {board.usualCapacity} orders · click a slot to change it</span>}>
        {!board ? <Loading /> : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {board.days.map((d) => (
              <div key={d.date} className="rounded-xl bg-cream/60 p-3">
                <p className="mb-2 text-sm font-bold">{d.label}{(d.label === "Today" || d.label === "Tomorrow") && <span className="font-normal text-muted"> · {day(d.date)}</span>}</p>
                <div className="space-y-1.5">
                  {d.slots.map((s) => (
                    <button key={s.start} type="button" disabled={s.past} onClick={() => { setSlot({ date: d.date, slot: s }); setCapacity(String(s.capacity)); setNote(s.note ?? ""); }}
                      className={`block w-full rounded-lg bg-white p-2 text-left transition hover:ring-2 hover:ring-tangerine/50 disabled:cursor-default disabled:hover:ring-0 ${s.custom ? "ring-1 ring-sun" : ""}`}>
                      <SlotLoad slot={s} />
                      {s.custom && <span className="mt-1 block text-[0.6875rem] font-semibold text-ink-soft">Changed{s.note ? `: ${s.note}` : ""}</span>}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card title="Delivery areas" className="mt-4">
        <form className="mb-3 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); if (newArea.trim()) void saveArea(null, newArea.trim(), true); }}>
          <input value={newArea} onChange={(e) => setNewArea(e.target.value)} placeholder="New area, e.g. Wari" className={`${input} max-w-xs`} />
          <button type="submit" className={btn.dark}><Plus size={15} /> Add area</button>
          {areaError && <span className="self-center text-sm font-semibold text-berry">{areaError}</span>}
        </form>
        {!areas ? <Loading /> : (
          <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
            {areas.map((a) => (
              <li key={a.id} className={`flex items-center gap-2 rounded-xl border border-line p-2.5 ${a.active ? "bg-white" : "bg-cream text-muted"}`}>
                <MapPin size={16} className={a.active ? "text-tangerine" : ""} />
                <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{a.name}</span><span className="text-xs text-muted">{a.orders} orders</span></span>
                <label className="flex cursor-pointer items-center gap-1.5 text-xs font-bold">
                  <input type="checkbox" checked={a.active} onChange={(e) => saveArea(a.id, a.name, e.target.checked)} className="h-4 w-4 accent-leaf" /> {a.active ? "On" : "Off"}
                </label>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Modal open={slot !== null} onClose={() => setSlot(null)} title={slot ? `${day(slot.date)}, ${slot.slot.label}` : ""}>
        {slot && (
          <div className="space-y-3 text-sm">
            <p>{slot.slot.booked} order{slot.slot.booked === 1 ? "" : "s"} booked. Usually up to {board?.usualCapacity}.</p>
            <label className="block"><span className="mb-1 block text-xs font-bold text-ink-soft">Orders this slot can take</span><input inputMode="numeric" value={capacity} onChange={(e) => setCapacity(e.target.value.replace(/[^0-9]/g, ""))} className={`${input} w-32`} /></label>
            <label className="block"><span className="mb-1 block text-xs font-bold text-ink-soft">Reason (optional)</span><input value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Eid, two riders off" className={input} /></label>
            {slot.slot.booked > Number(capacity || 0) && <p className="rounded-lg bg-sun-soft p-2 text-xs">Orders already booked stay booked; the slot just won&rsquo;t take more.</p>}
            <div className="flex flex-wrap gap-2 pt-2">
              <button type="button" onClick={() => saveSlot(Number(capacity || 0))} className={btn.primary}>Save</button>
              <button type="button" onClick={() => saveSlot(0)} className={btn.danger}>Close this slot</button>
              {slot.slot.custom && <button type="button" onClick={() => saveSlot(null)} className={btn.ghost}>Back to usual</button>}
            </div>
          </div>
        )}
      </Modal>
    </>
  );
}
