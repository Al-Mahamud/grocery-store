"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Lock } from "lucide-react";
import { ApiError, request } from "@/lib/client";
import { Field } from "@/components/ui/Field";
import { safeNext } from "@/components/account/AuthForm";

function LoginForm() {
  const next = safeNext(useSearchParams().get("next") ?? undefined, "/admin");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <form
      noValidate
      className="w-full max-w-sm space-y-4 rounded-3xl bg-white p-7 shadow-2xl"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError(null);
        try {
          await request("POST", "/staff/login", { phone, password });
          // A full load, so the admin frame starts fresh with the new sign-in.
          window.location.assign(next.startsWith("/admin") ? next : "/admin");
        } catch (err) {
          setBusy(false);
          setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
        }
      }}
    >
      <div className="flex items-center gap-2">
        <span className="grid h-10 w-10 place-items-center rounded-xl bg-tangerine font-display text-xl font-extrabold text-white">h</span>
        <div>
          <p className="font-display text-xl font-extrabold leading-none">haat<span className="text-tangerine">.</span> admin</p>
          <p className="text-xs text-muted">For shop staff only</p>
        </div>
      </div>
      <Field label="Mobile number"><input type="tel" autoComplete="username" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="01XXXXXXXXX" /></Field>
      <Field label="Password"><input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} /></Field>
      {error && <p role="alert" className="rounded-xl bg-berry/10 p-3 text-sm font-semibold text-berry">{error}</p>}
      <button type="submit" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-ink py-3.5 font-bold text-white disabled:opacity-60"><Lock size={16} /> {busy ? "Signing in…" : "Sign in"}</button>
    </form>
  );
}

export default function AdminLoginPage() {
  return (
    <main className="grid min-h-dvh place-items-center bg-ink bg-[radial-gradient(circle_at_20%_20%,#ff6b3533,transparent_40%),radial-gradient(circle_at_80%_90%,#17a05d33,transparent_40%)] p-4">
      <Suspense>
        <LoginForm />
      </Suspense>
    </main>
  );
}
