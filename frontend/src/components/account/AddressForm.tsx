"use client";

import { useState } from "react";
import { ApiError, request } from "@/lib/client";
import type { Area, SavedAddress } from "@/lib/types";
import { Field } from "@/components/ui/Field";

const LABELS = ["Home", "Office", "Family"];

export type AddressValues = { label: string; name: string; phone: string; address: string; areaId: string; note: string; makeDefault: boolean };

export function emptyAddress(name = "", phone = ""): AddressValues {
  return { label: "Home", name, phone, address: "", areaId: "", note: "", makeDefault: false };
}

export function addressValues(a: SavedAddress): AddressValues {
  return { label: a.label, name: a.name, phone: a.phone, address: a.address, areaId: String(a.areaId), note: a.note ?? "", makeDefault: a.isDefault };
}

/** Add or edit a saved address. */
export function AddressForm({ areas, initial, id, onSaved, onCancel }: { areas: Area[]; initial: AddressValues; id?: number; onSaved: () => void; onCancel: () => void }) {
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = <K extends keyof AddressValues>(key: K, value: AddressValues[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: "" }));
  };
  const custom = !LABELS.includes(form.label);

  return (
    <form
      noValidate
      className="space-y-3"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setFormError(null);
        try {
          const body = { ...form, areaId: form.areaId ? Number(form.areaId) : null };
          await request(id ? "PUT" : "POST", id ? `/account/addresses/${id}` : "/account/addresses", body);
          onSaved();
        } catch (err) {
          setBusy(false);
          if (err instanceof ApiError) {
            setErrors(err.fields);
            setFormError(err.message);
          } else setFormError("Something went wrong. Please try again.");
        }
      }}
    >
      <fieldset>
        <legend className="mb-1 block text-xs font-bold text-ink-soft">Label</legend>
        <div className="flex flex-wrap gap-1.5">
          {[...LABELS, "Other"].map((l) => {
            const chosen = l === "Other" ? custom : form.label === l;
            return (
              <button key={l} type="button" aria-pressed={chosen} onClick={() => set("label", l === "Other" ? (custom ? form.label : "") : l)} className={`rounded-xl border-2 px-3.5 py-2 text-sm font-bold transition ${chosen ? "border-ink bg-ink text-white" : "border-line hover:border-ink/40"}`}>
                {l}
              </button>
            );
          })}
        </div>
        {custom && (
          <Field label="Name this address" error={errors.label} className="mt-2">
            <input value={form.label} maxLength={20} placeholder="e.g. Parents' flat" onChange={(e) => set("label", e.target.value)} />
          </Field>
        )}
      </fieldset>
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Receiver's name" error={errors.name}><input autoComplete="name" value={form.name} onChange={(e) => set("name", e.target.value)} /></Field>
        <Field label="Mobile number" error={errors.phone}><input type="tel" inputMode="tel" autoComplete="tel" value={form.phone} onChange={(e) => set("phone", e.target.value)} /></Field>
      </div>
      <Field label="Area" error={errors.areaId}>
        <select value={form.areaId} onChange={(e) => set("areaId", e.target.value)}>
          <option value="">Choose the area</option>
          {areas.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
        </select>
      </Field>
      <Field label="Address" error={errors.address} hint="House, road, flat and any landmark"><input autoComplete="street-address" value={form.address} onChange={(e) => set("address", e.target.value)} /></Field>
      <Field label="Note for the rider (optional)" error={errors.note}><input value={form.note} onChange={(e) => set("note", e.target.value)} placeholder="e.g. Gate code 1234" /></Field>
      <label className="flex items-center gap-2.5 text-sm font-semibold">
        <input type="checkbox" checked={form.makeDefault} onChange={(e) => set("makeDefault", e.target.checked)} className="h-4 w-4 accent-tangerine" /> Use this address by default
      </label>
      {formError && <p role="alert" className="rounded-xl bg-berry/10 p-3 text-sm font-semibold text-berry">{formError}</p>}
      <div className="flex gap-2 pt-1">
        <button type="submit" disabled={busy} className="flex-1 rounded-xl bg-tangerine py-3 text-sm font-bold text-white hover:bg-tangerine-dark disabled:opacity-60">{busy ? "Saving…" : "Save address"}</button>
        <button type="button" onClick={onCancel} className="rounded-xl bg-cream px-5 py-3 text-sm font-bold hover:bg-cream-deep">Cancel</button>
      </div>
    </form>
  );
}
