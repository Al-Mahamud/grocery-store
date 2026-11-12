"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Banknote, CreditCard, Lock, PhoneCall, Repeat, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import { taka } from "@/lib/money";
import type { Area, Day, Quote } from "@/lib/types";
import { useCart } from "./CartProvider";

type Substitution = "CALL" | "REPLACE" | "REMOVE";
type Form = { name: string; phone: string; email: string; address: string; areaId: string; note: string; substitution: Substitution };

const SAVED = "haat.checkout";

/* eslint-disable @next/next/no-img-element */
export function CheckoutForm() {
  const { cart, refresh } = useCart();
  const router = useRouter();
  const [form, setForm] = useState<Form>({ name: "", phone: "", email: "", address: "", areaId: "", note: "", substitution: "REPLACE" });
  const [areas, setAreas] = useState<Area[]>([]);
  const [days, setDays] = useState<Day[]>([]);
  const [day, setDay] = useState(0);
  const [slot, setSlot] = useState<{ date: string; start: string; label: string } | null>(null);
  const [quote, setQuote] = useState<Quote | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);

  const loadSlots = () => request<Day[]>("GET", "/delivery/slots").then(setDays).catch(() => undefined);

  useEffect(() => {
    request<Area[]>("GET", "/delivery/areas").then(setAreas).catch(() => setAreas([]));
    request<Day[]>("GET", "/delivery/slots")
      .then((list) => {
        setDays(list);
        const firstOpen = list.findIndex((d) => d.slots.some((s) => s.available));
        if (firstOpen >= 0) setDay(firstOpen);
      })
      .catch(() => setDays([]));
    try {
      const saved = localStorage.getItem(SAVED);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- restoring saved form values once.
      if (saved) setForm((f) => ({ ...f, ...JSON.parse(saved), note: "" }));
    } catch {
      // Storage unavailable; start empty.
    }
  }, []);

  useEffect(() => {
    if (!cart || cart.items.length === 0) return;
    request<Quote>("POST", "/checkout/quote").then(setQuote).catch(() => setQuote(null));
  }, [cart]);

  const set = <K extends keyof Form>(key: K, value: Form[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => {
      const next = { ...e };
      delete next[key];
      return next;
    });
  };

  if (cart && cart.items.length === 0 && !placing) {
    return (
      <div className="container-page grid min-h-[60vh] place-items-center text-center">
        <div>
          <h1 className="font-display text-3xl font-extrabold">Your bag is empty</h1>
          <Link href="/" className="mt-5 inline-block rounded-xl bg-tangerine px-7 py-3.5 text-sm font-bold text-white">Start shopping</Link>
        </div>
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!slot) {
      setErrors((x) => ({ ...x, slot: "Please choose a delivery slot." }));
      setFormError("Please choose a delivery slot.");
      return;
    }
    setPlacing(true);
    setFormError(null);
    try {
      const placed = await request<{ number: string; token: string; paymentUrl: string | null }>("POST", "/checkout", {
        ...form,
        areaId: form.areaId ? Number(form.areaId) : null,
        slotDate: slot.date,
        slotStart: slot.start,
        paymentMethod: "COD",
      });
      try {
        const { name, phone, email, address, areaId, substitution } = form;
        localStorage.setItem(SAVED, JSON.stringify({ name, phone, email, address, areaId, substitution }));
      } catch {
        // Not remembering is fine.
      }
      router.replace(`/order/${placed.number}?t=${encodeURIComponent(placed.token)}`);
      void refresh();
    } catch (err) {
      setPlacing(false);
      if (err instanceof ApiError) {
        setErrors(err.fields);
        setFormError(err.message);
        if (err.code === "delivery.slot") {
          setSlot(null);
          void loadSlots();
        }
        if (err.code.startsWith("checkout.") || err.code.startsWith("cart.")) void refresh();
      } else {
        setFormError("Something went wrong. Please try again.");
      }
    }
  };

  const current = days[day];
  const subs: { value: Substitution; icon: React.ReactNode; title: string; text: string }[] = [
    { value: "REPLACE", icon: <Repeat size={18} />, title: "Replace it", text: "With a similar item at the same or lower price" },
    { value: "CALL", icon: <PhoneCall size={18} />, title: "Call me", text: "We ring you while packing" },
    { value: "REMOVE", icon: <Trash2 size={18} />, title: "Remove it", text: "And take it off the bill" },
  ];

  return (
    <form onSubmit={submit} noValidate className="container-page grid gap-5 py-6 lg:grid-cols-12 md:py-8">
      <div className="space-y-4 lg:col-span-8">
        <Section n={1} title="Delivery slot">
          <div className="no-scrollbar -mx-1 flex gap-2 overflow-x-auto px-1" role="tablist" aria-label="Delivery day">
            {days.map((d, i) => (
              <button key={d.date} type="button" role="tab" aria-selected={i === day} onClick={() => setDay(i)} className={`shrink-0 rounded-xl px-4 py-2.5 text-left transition ${i === day ? "bg-ink text-white" : "bg-cream hover:bg-cream-deep"}`}>
                <span className="block text-sm font-bold">{d.label}</span>
                <span className={`block text-xs ${i === day ? "text-white/70" : "text-muted"}`}>{d.slots.filter((s) => s.available).length} slots open</span>
              </button>
            ))}
          </div>
          {current && (
            <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
              {current.slots.map((s) => {
                const chosen = slot?.date === current.date && slot.start === s.start;
                return (
                  <button
                    key={s.start}
                    type="button"
                    disabled={!s.available}
                    aria-pressed={chosen}
                    onClick={() => { setSlot({ date: current.date, start: s.start, label: `${current.label}, ${s.label}` }); setErrors((e) => ({ ...e, slot: "" })); setFormError(null); }}
                    className={`rounded-xl border-2 px-3 py-2.5 text-left transition ${chosen ? "border-leaf bg-leaf-soft" : s.available ? "border-line hover:border-ink/40" : "cursor-not-allowed border-line bg-cream text-muted"}`}
                  >
                    <span className="block text-sm font-bold">{s.label}</span>
                    <span className={`block text-xs ${chosen ? "text-leaf" : "text-muted"}`}>{!s.available ? "Unavailable" : s.remaining <= 5 ? `Only ${s.remaining} left` : "Available"}</span>
                  </button>
                );
              })}
            </div>
          )}
          {errors.slot && <p className="mt-2 text-xs font-semibold text-berry">{errors.slot}</p>}
        </Section>

        <Section n={2} title="Address & contact">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Full name" error={errors.name}><input autoComplete="name" value={form.name} onChange={(e) => set("name", e.target.value)} /></Field>
            <Field label="Mobile number" error={errors.phone} hint="The rider calls this number."><input type="tel" inputMode="tel" autoComplete="tel" placeholder="01XXXXXXXXX" value={form.phone} onChange={(e) => set("phone", e.target.value)} /></Field>
            <Field label="Area" error={errors.areaId}>
              <select value={form.areaId} onChange={(e) => set("areaId", e.target.value)}>
                <option value="">Choose your area</option>
                {areas.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
              </select>
            </Field>
            <Field label="Email (optional)" error={errors.email}><input type="email" autoComplete="email" value={form.email} onChange={(e) => set("email", e.target.value)} /></Field>
            <Field label="Address" error={errors.address} hint="House, road, flat and any landmark" className="sm:col-span-2"><input autoComplete="street-address" value={form.address} onChange={(e) => set("address", e.target.value)} /></Field>
            <Field label="Note for the rider (optional)" error={errors.note} className="sm:col-span-2"><input value={form.note} onChange={(e) => set("note", e.target.value)} placeholder="e.g. Leave with the guard" /></Field>
          </div>
        </Section>

        <Section n={3} title="If something is out of stock">
          <div className="grid gap-2 sm:grid-cols-3">
            {subs.map((s) => (
              <label key={s.value} className={`flex cursor-pointer gap-3 rounded-xl border-2 p-3 transition ${form.substitution === s.value ? "border-tangerine bg-tangerine-soft" : "border-line hover:border-ink/40"}`}>
                <input type="radio" name="substitution" className="sr-only" checked={form.substitution === s.value} onChange={() => set("substitution", s.value)} />
                <span className="text-tangerine">{s.icon}</span>
                <span><span className="block text-sm font-bold">{s.title}</span><span className="block text-xs text-muted">{s.text}</span></span>
              </label>
            ))}
          </div>
        </Section>

        <Section n={4} title="Payment">
          <div className="grid gap-2 sm:grid-cols-2">
            <div className="flex items-center gap-3 rounded-xl border-2 border-tangerine bg-tangerine-soft p-3">
              <Banknote size={22} className="text-tangerine" />
              <span><span className="block text-sm font-bold">Cash on delivery</span><span className="block text-xs text-muted">Check your items, then pay the rider</span></span>
            </div>
            <div className="flex items-center gap-3 rounded-xl border-2 border-line p-3 opacity-60">
              <CreditCard size={22} />
              <span><span className="block text-sm font-bold">bKash, Nagad or card</span><span className="block text-xs text-muted">Coming soon</span></span>
            </div>
          </div>
        </Section>
      </div>

      <aside className="lg:col-span-4">
        <div className="space-y-4 rounded-3xl bg-white p-5 md:p-6 lg:sticky lg:top-6">
          <h2 className="font-display text-xl font-bold">Your order</h2>
          {cart && (
            <ul className="no-scrollbar max-h-64 space-y-2 overflow-y-auto">
              {cart.items.map((l) => (
                <li key={l.packId} className="flex items-center gap-3">
                  <span className="relative h-12 w-12 shrink-0 rounded-lg border border-line bg-white">
                    {l.image && <img src={l.image} alt="" className="h-full w-full object-contain" />}
                    <span className="absolute -right-1.5 -top-1.5 grid h-5 min-w-5 place-items-center rounded-full bg-ink px-1 text-[0.625rem] font-bold text-white">{l.quantity}</span>
                  </span>
                  <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{l.name}</span><span className="text-xs text-muted">{l.packLabel}</span></span>
                  <span className="text-sm font-bold tabular-nums">{taka(l.lineTotal)}</span>
                </li>
              ))}
            </ul>
          )}
          <dl className="space-y-1.5 border-t border-line pt-3 text-sm">
            <div className="flex justify-between"><dt className="text-muted">Items</dt><dd className="tabular-nums">{quote ? taka(quote.subtotal) : "—"}</dd></div>
            {quote && quote.discount > 0 && <div className="flex justify-between text-leaf"><dt>Promo ({quote.couponCode})</dt><dd className="tabular-nums">−{taka(quote.discount)}</dd></div>}
            <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="tabular-nums">{quote ? (quote.deliveryFee === 0 ? "Free" : taka(quote.deliveryFee)) : "—"}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Slot</dt><dd className="font-semibold">{slot?.label ?? "Not chosen"}</dd></div>
          </dl>
          <div className="flex items-baseline justify-between border-t border-line pt-3">
            <span className="font-bold">Total</span>
            <span className="font-display text-3xl font-extrabold tabular-nums">{quote ? taka(quote.total) : "—"}</span>
          </div>
          {(formError || quote?.problem) && <p role="alert" className="rounded-xl bg-berry/10 p-3 text-sm font-semibold text-berry">{formError ?? quote?.problem}</p>}
          <button type="submit" disabled={placing || !cart?.canCheckout} className="flex h-14 w-full items-center justify-center gap-2 rounded-2xl bg-tangerine font-bold text-white transition hover:bg-tangerine-dark disabled:cursor-not-allowed disabled:opacity-50">
            <Lock size={16} /> {placing ? "Placing order…" : `Place order${quote ? ` · ${taka(quote.total)}` : ""}`}
          </button>
        </div>
      </aside>
    </form>
  );
}

function Section({ n, title, children }: { n: number; title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-3xl bg-white p-5 md:p-6">
      <h2 className="mb-4 flex items-center gap-2.5 font-display text-xl font-bold">
        <span className="grid h-7 w-7 place-items-center rounded-lg bg-tangerine text-sm text-white">{n}</span>{title}
      </h2>
      {children}
    </section>
  );
}

function Field({ label, hint, error, className = "", children }: { label: string; hint?: string; error?: string; className?: string; children: React.ReactNode }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1 block text-xs font-bold text-ink-soft">{label}</span>
      <span className={`block rounded-xl border-2 bg-white transition focus-within:border-tangerine [&>*]:w-full [&>*]:rounded-xl [&>*]:bg-transparent [&>*]:px-3.5 [&>*]:py-2.5 [&>*]:text-sm [&>*]:outline-none ${error ? "border-berry" : "border-line"}`}>{children}</span>
      {error ? <span className="mt-1 block text-xs font-semibold text-berry">{error}</span> : hint ? <span className="mt-1 block text-xs text-muted">{hint}</span> : null}
    </label>
  );
}
