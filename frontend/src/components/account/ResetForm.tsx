"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { KeyRound } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import type { Profile } from "@/lib/types";
import { Field } from "@/components/ui/Field";
import { useCart } from "@/components/cart/CartProvider";
import { useAccount } from "./AccountProvider";

/** Forgot password: a code texted to the account's number, then a new password. */
export function ResetForm() {
  const router = useRouter();
  const { setMe } = useAccount();
  const { refresh } = useCart();
  const [phone, setPhone] = useState("");
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const fail = (err: unknown) => {
    if (err instanceof ApiError) {
      setErrors(err.fields);
      setFormError(Object.keys(err.fields).length ? null : err.message);
    } else setFormError("Something went wrong. Please try again.");
  };

  return (
    <div className="container-page grid max-w-md place-items-center py-10 md:py-16">
      <form
        noValidate
        className="w-full space-y-4 rounded-3xl bg-white p-6 md:p-8"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setFormError(null);
          try {
            if (!sentTo) {
              const r = await request<{ sentTo: string }>("POST", "/auth/codes", { phone, purpose: "RESET" });
              setSentTo(r.sentTo);
            } else {
              const { customer } = await request<{ customer: Profile }>("POST", "/auth/password-reset", { phone, code, newPassword: password });
              setMe(customer);
              await refresh();
              router.replace("/account");
            }
          } catch (err) {
            fail(err);
          } finally {
            setBusy(false);
          }
        }}
      >
        <span className="grid h-12 w-12 place-items-center rounded-2xl bg-sun-soft"><KeyRound size={22} /></span>
        <div>
          <h1 className="font-display text-2xl font-bold">Forgot your password?</h1>
          <p className="mt-1 text-sm text-muted">{sentTo ? `If ${sentTo} has an account, we've texted it a 6-digit code.` : "We'll text a code to your account's mobile number."}</p>
        </div>
        <Field label="Mobile number" error={errors.phone}>
          <input type="tel" inputMode="tel" autoComplete="tel" placeholder="01XXXXXXXXX" value={phone} readOnly={!!sentTo} onChange={(e) => setPhone(e.target.value)} />
        </Field>
        {sentTo && (
          <>
            <Field label="Code" error={errors.code}>
              <input inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))} className="text-center font-display text-2xl tracking-[0.5em]" autoFocus />
            </Field>
            <Field label="New password" error={errors.newPassword} hint="At least 8 characters. Other devices will be signed out.">
              <input type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </Field>
          </>
        )}
        {formError && <p role="alert" className="rounded-xl bg-berry/10 p-3 text-sm font-semibold text-berry">{formError}</p>}
        <button type="submit" disabled={busy} className="w-full rounded-2xl bg-tangerine py-3.5 font-bold text-white hover:bg-tangerine-dark disabled:opacity-60">
          {busy ? "Please wait…" : sentTo ? "Set new password" : "Text me a code"}
        </button>
        <p className="text-center text-sm"><Link href="/login" className="font-bold text-ink-soft hover:text-tangerine">Back to sign in</Link></p>
      </form>
    </div>
  );
}
