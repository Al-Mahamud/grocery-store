"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Banknote, Check, CreditCard, Lock, PhoneCall, Plus, Repeat, Trash2 } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import { taka } from "@/lib/money";
import type { Area, Day, PaymentOptions, Quote, SavedAddress } from "@/lib/types";
import { useAccount } from "@/components/account/AccountProvider";
import { Field } from "@/components/ui/Field";
import { useCart } from "./CartProvider";

type Substitution = "CALL" | "REPLACE" | "REMOVE";
type Form = { name: string; phone: string; email: string; address: string; areaId: string; note: string; substitution: Substitution };

const SAVED = "haat.checkout";

/* eslint-disable @next/next/no-img-element */
export function CheckoutForm() {
  const { cart, refresh } = useCart();
  const { me } = useAccount();
  const router = useRouter();
  const [saved, setSaved] = useState<SavedAddress[]>([]);
  const [chosen, setChosen] = useState<number | "new">("new");
  const [saveAddress, setSaveAddress] = useState(true);
  const [method, setMethod] = useState<"COD" | "ONLINE">("COD");
  const [payOptions, setPayOptions] = useState<PaymentOptions | null>(null);
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
    request<PaymentOptions>("GET", "/payments/options").then(setPayOptions).catch(() => setPayOptions(null));
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

  // Signed in: start from the default saved address, or at least the customer's name and number.
  useEffect(() => {
    if (!me) return;
    request<SavedAddress[]>("GET", "/account/addresses")
      .then((list) => {
        setSaved(list);
        const first = list.find((a) => a.isDefault) ?? list[0];
        if (first) {
          setChosen(first.id);
          setForm((f) => ({ ...f, ...fromSaved(first), email: f.email || me.email || "" }));
        } else {
          setForm((f) => ({ ...f, name: f.name || me.name, phone: f.phone || me.phone, email: f.email || me.email || "" }));
        }
      })
      .catch(() => setSaved([]));
  }, [me]);

  const choose = (a: SavedAddress | "new") => {
    setErrors({});
    if (a === "new") {
      setChosen("new");
      setForm((f) => ({ ...f, name: me?.name ?? "", phone: me?.phone ?? "", address: "", areaId: "", note: "" }));
    } else {
      setChosen(a.id);
      setForm((f) => ({ ...f, ...fromSaved(a) }));
    }
  };

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
        paymentMethod: method,
        saveAddress: Boolean(me) && chosen === "new" && saveAddress,
      });
      try {
        const { name, phone, email, address, areaId, substitution } = form;
        localStorage.setItem(SAVED, JSON.stringify({ name, phone, email, address, areaId, substitution }));
      } catch {
        // Not remembering is fine.
      }
      if (placed.paymentUrl) {
        window.location.assign(placed.paymentUrl);
        return;
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
          {me === null && (
            <p className="mb-4 rounded-xl bg-cream px-4 py-3 text-sm">
              Have an account? <Link href="/login?next=/checkout" className="font-bold text-tangerine hover:text-tangerine-dark">Sign in</Link> to use your saved addresses.
            </p>
          )}
          {saved.length > 0 && (
            <div className="mb-4 grid gap-2 sm:grid-cols-2" role="radiogroup" aria-label="Saved addresses">
              {saved.map((a) => (
                <button key={a.id} type="button" role="radio" aria-checked={chosen === a.id} onClick={() => choose(a)} className={`relative rounded-xl border-2 p-3 text-left transition ${chosen === a.id ? "border-leaf bg-leaf-soft" : "border-line hover:border-ink/40"}`}>
                  <span className="block text-sm font-bold">{a.label}{a.isDefault && <span className="ml-1.5 text-xs font-semibold text-muted">· default</span>}</span>
                  <span className="block truncate text-xs text-ink-soft">{a.address}, {a.area}</span>
                  <span className="block text-xs text-muted">{a.name} · {a.phone}</span>
                  {chosen === a.id && <Check size={16} strokeWidth={3} className="absolute right-3 top-3 text-leaf" />}
                </button>
              ))}
              <button type="button" role="radio" aria-checked={chosen === "new"} onClick={() => choose("new")} className={`flex items-center gap-2 rounded-xl border-2 border-dashed p-3 text-sm font-bold transition ${chosen === "new" ? "border-tangerine bg-tangerine-soft" : "border-line hover:border-ink/40"}`}>
                <Plus size={16} /> Deliver somewhere else
              </button>
            </div>
          )}
          <div className={`grid gap-3 sm:grid-cols-2 ${chosen !== "new" ? "hidden" : ""}`}>
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
            {me && (
              <label className="flex items-center gap-2.5 text-sm font-semibold sm:col-span-2">
                <input type="checkbox" checked={saveAddress} onChange={(e) => setSaveAddress(e.target.checked)} className="h-4 w-4 accent-tangerine" /> Save this address to my account
              </label>
            )}
          </div>
          {chosen !== "new" && Object.values(errors).some(Boolean) && (
            <p className="mt-2 text-xs font-semibold text-berry">{Object.values(errors).find(Boolean)} <button type="button" className="underline" onClick={() => setChosen("new")}>Edit the details</button></p>
          )}
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
          <div className="grid gap-2 sm:grid-cols-2" role="radiogroup" aria-label="Payment method">
            <PayChoice selected={method === "COD"} onSelect={() => setMethod("COD")} icon={<Banknote size={22} />} title="Cash on delivery" text="Check your items, then pay the rider" />
            <PayChoice
              selected={method === "ONLINE"}
              onSelect={() => setMethod("ONLINE")}
              disabled={!payOptions?.online}
              icon={<CreditCard size={22} />}
              title="bKash, Nagad or card"
              text={payOptions?.online ? "Pay now on the secure SSLCommerz page" : "Not available right now"}
            />
          </div>
          {method === "ONLINE" && payOptions && (
            <p className="mt-3 rounded-xl bg-cream px-4 py-3 text-xs text-ink-soft">
              After you place the order you&rsquo;ll pay on SSLCommerz. Your items and slot are held for {payOptions.holdMinutes} minutes; if the payment doesn&rsquo;t go through you can try again or switch to cash.
            </p>
          )}
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
            <Lock size={16} /> {placing ? (method === "ONLINE" ? "Opening payment…" : "Placing order…") : `${method === "ONLINE" ? "Place order & pay" : "Place order"}${quote ? ` · ${taka(quote.total)}` : ""}`}
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

function fromSaved(a: SavedAddress) {
  return { name: a.name, phone: a.phone, address: a.address, areaId: String(a.areaId), note: a.note ?? "" };
}

function PayChoice({ selected, onSelect, disabled = false, icon, title, text }: { selected: boolean; onSelect: () => void; disabled?: boolean; icon: React.ReactNode; title: string; text: string }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      onClick={onSelect}
      className={`flex items-center gap-3 rounded-xl border-2 p-3 text-left transition ${selected ? "border-tangerine bg-tangerine-soft" : "border-line hover:border-ink/40"} disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:border-line`}
    >
      <span className={selected ? "text-tangerine" : "text-ink-soft"}>{icon}</span>
      <span><span className="block text-sm font-bold">{title}</span><span className="block text-xs text-muted">{text}</span></span>
    </button>
  );
}
