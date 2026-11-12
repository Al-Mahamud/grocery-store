"use client";

import { useCallback, useEffect, useState } from "react";
import { MapPin, Pencil, Plus, Star, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { Area, SavedAddress } from "@/lib/types";
import { Modal } from "@/components/ui/Modal";
import { useCart } from "@/components/cart/CartProvider";
import { useAccount } from "./AccountProvider";
import { AddressForm, addressValues, emptyAddress } from "./AddressForm";

export function AddressBook() {
  const { me } = useAccount();
  const { notify } = useCart();
  const [list, setList] = useState<SavedAddress[] | null>(null);
  const [areas, setAreas] = useState<Area[]>([]);
  const [editing, setEditing] = useState<SavedAddress | "new" | null>(null);

  const load = useCallback(() => request<SavedAddress[]>("GET", "/account/addresses").then(setList).catch(() => setList([])), []);
  useEffect(() => {
    void load();
    request<Area[]>("GET", "/delivery/areas").then(setAreas).catch(() => setAreas([]));
  }, [load]);

  const act = async (method: string, path: string, done: string) => {
    try {
      await request(method, path);
      await load();
      notify(done);
    } catch (e) {
      notify(e instanceof ApiError ? e.message : "Something went wrong. Please try again.");
    }
  };

  if (!list) return <div className="h-64 animate-pulse rounded-3xl bg-white" aria-busy="true" />;

  return (
    <section aria-labelledby="addresses-title">
      <div className="mb-4 flex items-end justify-between gap-3">
        <div>
          <h2 id="addresses-title" className="font-display text-2xl font-bold">Saved addresses</h2>
          <p className="text-sm text-muted">Pick one at checkout instead of typing it again.</p>
        </div>
        {list.length > 0 && list.length < 10 && (
          <button type="button" onClick={() => setEditing("new")} className="inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-xl bg-ink px-4 py-2.5 text-sm font-bold text-white"><Plus size={16} /> Add address</button>
        )}
      </div>
      {list.length === 0 ? (
        <button type="button" onClick={() => setEditing("new")} className="grid w-full place-items-center rounded-3xl border-2 border-dashed border-line bg-white px-6 py-14 text-center hover:border-ink/40">
          <span className="grid h-14 w-14 place-items-center rounded-2xl bg-tangerine-soft text-tangerine"><MapPin size={26} /></span>
          <span className="mt-3 font-display text-xl font-bold">Add your first address</span>
          <span className="mt-1 text-sm text-muted">Home, office or anywhere we deliver in Dhaka.</span>
        </button>
      ) : (
        <ul className="grid gap-3 md:grid-cols-2">
          {list.map((a) => (
            <li key={a.id} className={`flex flex-col rounded-3xl border-2 bg-white p-5 ${a.isDefault ? "border-leaf" : "border-transparent"}`}>
              <div className="flex items-center gap-2">
                <span className="font-display text-lg font-bold">{a.label}</span>
                {a.isDefault && <span className="rounded-lg bg-leaf-soft px-2 py-0.5 text-xs font-bold text-leaf">Default</span>}
              </div>
              <p className="mt-2 text-sm font-semibold">{a.name} · {a.phone}</p>
              <p className="text-sm text-ink-soft">{a.address}, {a.area}</p>
              {a.note && <p className="mt-1 text-xs italic text-muted">“{a.note}”</p>}
              <div className="mt-4 flex flex-wrap gap-1.5 pt-1">
                <button type="button" onClick={() => setEditing(a)} className="inline-flex items-center gap-1.5 rounded-lg bg-cream px-3 py-2 text-xs font-bold hover:bg-cream-deep"><Pencil size={13} /> Edit</button>
                {!a.isDefault && (
                  <button type="button" onClick={() => act("POST", `/account/addresses/${a.id}/default`, `${a.label} is now your default address.`)} className="inline-flex items-center gap-1.5 rounded-lg bg-cream px-3 py-2 text-xs font-bold hover:bg-cream-deep"><Star size={13} /> Make default</button>
                )}
                <button
                  type="button"
                  onClick={() => window.confirm(`Remove “${a.label}”?`) && act("DELETE", `/account/addresses/${a.id}`, "Address removed.")}
                  className="ml-auto inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold text-berry hover:bg-berry/10"
                >
                  <Trash2 size={13} /> Remove
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Modal open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "New address" : "Edit address"}>
        {editing !== null && (
          <AddressForm
            key={editing === "new" ? "new" : editing.id}
            areas={areas}
            id={editing === "new" ? undefined : editing.id}
            initial={editing === "new" ? emptyAddress(me?.name ?? "", me?.phone ?? "") : addressValues(editing)}
            onCancel={() => setEditing(null)}
            onSaved={async () => {
              setEditing(null);
              await load();
              notify("Address saved.");
            }}
          />
        )}
      </Modal>
    </section>
  );
}
