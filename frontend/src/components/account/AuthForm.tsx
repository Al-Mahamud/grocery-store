"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Eye, EyeOff, History, MapPin, RotateCcw } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { Profile } from "@/lib/types";
import { Field } from "@/components/ui/Field";
import { useCart } from "@/components/cart/CartProvider";
import { useAccount } from "./AccountProvider";

type Mode = "login" | "register";

/** Only paths inside the shop, so a crafted link can't send people elsewhere after sign-in. */
export function safeNext(next: string | undefined, fallback = "/account"): string {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : fallback;
}

export function AuthForm({ mode, next }: { mode: Mode; next?: string }) {
  const router = useRouter();
  const { setMe } = useAccount();
  const { refresh } = useCart();
  const [form, setForm] = useState({ name: "", phone: "", email: "", password: "" });
  const [show, setShow] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const register = mode === "register";
  const target = safeNext(next);
  const other = `${register ? "/login" : "/register"}${next ? `?next=${encodeURIComponent(next)}` : ""}`;

  const set = (key: keyof typeof form, value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: "" }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setFormError(null);
    try {
      const body = register ? form : { phone: form.phone, password: form.password };
      const { customer } = await request<{ customer: Profile }>("POST", register ? "/auth/register" : "/auth/login", body);
      setMe(customer);
      await refresh();
      router.replace(target);
      router.refresh();
    } catch (err) {
      setBusy(false);
      if (err instanceof ApiError) {
        setErrors(err.fields);
        setFormError(err.message);
      } else {
        setFormError("Something went wrong. Please try again.");
      }
    }
  };

  const perks = [
    { icon: <RotateCcw size={18} />, title: "Buy again in a tap", text: "Your usual groceries, one list" },
    { icon: <MapPin size={18} />, title: "Saved addresses", text: "Home, office, parents' place" },
    { icon: <History size={18} />, title: "Every order in one place", text: "Slots, totals and status" },
  ];

  return (
    <div className="container-page max-w-5xl py-6 md:py-12">
      <div className="grid overflow-hidden rounded-3xl bg-white md:grid-cols-5">
        <aside className="relative overflow-hidden bg-ink p-6 text-white md:col-span-2 md:p-9">
          <div aria-hidden className="absolute -right-20 -top-24 h-40 w-40 rounded-full bg-tangerine/90 md:-right-16 md:-top-16 md:h-48 md:w-48" />
          <div aria-hidden className="absolute -bottom-24 -right-12 hidden h-44 w-44 rounded-full bg-leaf/80 md:block" />
          <div className="relative">
            <p className="text-xs font-bold uppercase tracking-widest text-sun">{register ? "New here?" : "Welcome back"}</p>
            <h1 className="mt-2 font-display text-3xl font-extrabold leading-tight tracking-tight md:text-4xl">
              {register ? "Make your weekly shop quicker." : "Sign in to shop faster."}
            </h1>
            <ul className="mt-6 hidden space-y-4 md:block">
              {perks.map((p) => (
                <li key={p.title} className="flex gap-3">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white/10 text-sun">{p.icon}</span>
                  <span><span className="block text-sm font-bold">{p.title}</span><span className="block text-xs text-white/65">{p.text}</span></span>
                </li>
              ))}
            </ul>
          </div>
        </aside>

        <form onSubmit={submit} noValidate className="space-y-4 p-6 md:col-span-3 md:p-10">
          <div>
            <h2 className="font-display text-2xl font-bold">{register ? "Create your account" : "Sign in"}</h2>
            <p className="mt-1 text-sm text-muted">
              {register ? "Already have one? " : "New to Haat? "}
              <Link href={other} className="font-bold text-tangerine hover:text-tangerine-dark">{register ? "Sign in" : "Create an account"}</Link>
            </p>
          </div>
          {register && (
            <Field label="Full name" error={errors.name}>
              <input autoComplete="name" value={form.name} onChange={(e) => set("name", e.target.value)} />
            </Field>
          )}
          <Field label="Mobile number" error={errors.phone} hint={register ? "You'll sign in with this number." : undefined}>
            <input type="tel" inputMode="tel" autoComplete="tel" placeholder="01XXXXXXXXX" value={form.phone} onChange={(e) => set("phone", e.target.value)} />
          </Field>
          {register && (
            <Field label="Email (optional)" error={errors.email}>
              <input type="email" autoComplete="email" value={form.email} onChange={(e) => set("email", e.target.value)} />
            </Field>
          )}
          <div className="relative">
            <Field label="Password" error={errors.password} hint={register ? "At least 8 characters." : undefined}>
              <input
                type={show ? "text" : "password"}
                autoComplete={register ? "new-password" : "current-password"}
                value={form.password}
                onChange={(e) => set("password", e.target.value)}
                className="pr-12!"
              />
            </Field>
            <button type="button" onClick={() => setShow((s) => !s)} aria-label={show ? "Hide password" : "Show password"} className="absolute right-2 top-[1.65rem] grid h-9 w-9 place-items-center rounded-lg text-muted hover:bg-cream hover:text-ink">
              {show ? <EyeOff size={17} /> : <Eye size={17} />}
            </button>
          </div>
          {formError && <p role="alert" className="rounded-xl bg-berry/10 p-3 text-sm font-semibold text-berry">{formError}</p>}
          <button type="submit" disabled={busy} className="flex h-13 w-full items-center justify-center rounded-2xl bg-tangerine py-3.5 font-bold text-white transition hover:bg-tangerine-dark disabled:opacity-60">
            {busy ? (register ? "Creating account…" : "Signing in…") : register ? "Create account" : "Sign in"}
          </button>
          <p className="text-center text-xs text-muted">Your bag comes with you when you sign in.</p>
        </form>
      </div>
    </div>
  );
}
