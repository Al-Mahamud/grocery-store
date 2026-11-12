"use client";

import { ShoppingBasket } from "lucide-react";
import { useCart } from "@/components/cart/CartProvider";
import { taka } from "@/lib/money";

export function BagButton() {
  const { cart, setOpen } = useCart();
  const count = cart?.itemCount ?? 0;
  return (
    <button type="button" onClick={() => setOpen(true)} aria-label={`Bag: ${count} items, ${taka(cart?.total ?? 0)}`} className="inline-flex h-12 shrink-0 items-center gap-2.5 rounded-2xl bg-tangerine pl-3 pr-4 font-bold text-white shadow-[0_8px_20px_-8px_rgba(255,107,53,0.7)] transition hover:bg-tangerine-dark">
      <span className="relative">
        <ShoppingBasket size={22} strokeWidth={2} />
        {count > 0 && <span key={count} className="absolute -right-2 -top-2 grid h-5 min-w-5 animate-pop place-items-center rounded-full bg-ink px-1 text-[0.625rem]">{count}</span>}
      </span>
      <span className="hidden text-sm tabular-nums sm:inline">{count > 0 ? taka(cart?.total ?? 0) : "Bag"}</span>
    </button>
  );
}
