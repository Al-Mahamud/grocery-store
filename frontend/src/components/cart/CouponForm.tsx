"use client";

import { useState } from "react";
import { Tag, X } from "lucide-react";
import { ApiError } from "@/lib/client";
import type { Cart } from "@/lib/types";
import { useCart } from "./CartProvider";

export function CouponForm({ cart }: { cart: Cart }) {
  const { applyCoupon, removeCoupon } = useCart();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  if (cart.coupon) {
    return (
      <div className="flex items-center justify-between gap-2 rounded-xl border-2 border-dashed border-leaf/40 bg-leaf-soft px-3 py-2 text-sm">
        <span className="flex min-w-0 items-center gap-2"><Tag size={15} className="shrink-0 text-leaf" /><span className="truncate"><strong>{cart.coupon.code}</strong> · {cart.coupon.description}</span></span>
        <button type="button" aria-label="Remove code" onClick={() => removeCoupon()} className="grid h-7 w-7 shrink-0 place-items-center rounded-lg hover:bg-white"><X size={14} /></button>
      </div>
    );
  }
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (!code.trim()) return;
        setPending(true);
        setError(null);
        try {
          await applyCoupon(code);
          setCode("");
        } catch (err) {
          setError(err instanceof ApiError ? err.message : "Couldn't apply that code.");
        } finally {
          setPending(false);
        }
      }}
    >
      <label htmlFor="coupon" className="sr-only">Promo code</label>
      <div className="flex gap-2">
        <input id="coupon" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="Promo code" autoComplete="off" className="min-w-0 flex-1 rounded-xl border border-line bg-white px-3 py-2.5 text-sm uppercase outline-none placeholder:normal-case focus:border-tangerine" />
        <button type="submit" disabled={pending} className="rounded-xl bg-ink px-4 text-sm font-bold text-white disabled:opacity-50">Apply</button>
      </div>
      {(error || cart.couponProblem) && <p role="alert" className="mt-1.5 text-xs text-berry">{error ?? cart.couponProblem}</p>}
    </form>
  );
}
