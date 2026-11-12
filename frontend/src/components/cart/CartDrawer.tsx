"use client";

import Link from "next/link";
import { ShoppingBasket, X } from "lucide-react";
import { Drawer } from "@/components/ui/Drawer";
import { taka } from "@/lib/money";
import { useCart } from "./CartProvider";
import { CartLines } from "./CartLines";
import { Progress } from "./Progress";

export function CartDrawer() {
  const { cart, open, setOpen } = useCart();
  const empty = !cart || cart.items.length === 0;
  return (
    <Drawer open={open} onClose={() => setOpen(false)} label="Your bag">
      <div className="flex h-16 shrink-0 items-center justify-between border-b border-line px-5">
        <p className="font-display text-xl font-bold">Your bag {cart && cart.itemCount > 0 && <span className="text-muted">· {cart.itemCount} items</span>}</p>
        <button type="button" aria-label="Close bag" onClick={() => setOpen(false)} className="grid h-10 w-10 place-items-center rounded-xl bg-cream"><X size={18} /></button>
      </div>
      {empty ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-3 px-8 text-center">
          <span className="grid h-16 w-16 place-items-center rounded-2xl bg-tangerine-soft text-tangerine"><ShoppingBasket size={30} /></span>
          <p className="font-display text-2xl font-bold">Your bag is empty</p>
          <p className="text-sm text-muted">Fill it with fresh vegetables, staples and more.</p>
          <Link href="/c/vegetables" onClick={() => setOpen(false)} className="mt-2 rounded-xl bg-tangerine px-6 py-3 text-sm font-bold text-white">Start with vegetables</Link>
        </div>
      ) : (
        <>
          <div className="space-y-3 px-5 pt-4"><Progress cart={cart} /></div>
          <div className="flex-1 overflow-y-auto px-5"><CartLines lines={cart.items} /></div>
          <div className="shrink-0 space-y-2 border-t border-line bg-cream/60 p-5 text-sm">
            {cart.savings > 0 && <div className="flex justify-between font-semibold text-leaf"><span>You save</span><span className="tabular-nums">{taka(cart.savings + cart.discount)}</span></div>}
            <div className="flex justify-between"><span className="text-muted">Items</span><span className="tabular-nums">{taka(cart.subtotal)}</span></div>
            {cart.discount > 0 && <div className="flex justify-between text-leaf"><span>Promo ({cart.coupon?.code})</span><span className="tabular-nums">−{taka(cart.discount)}</span></div>}
            <div className="flex justify-between"><span className="text-muted">Delivery</span><span className="tabular-nums">{cart.deliveryFee === 0 ? "Free" : taka(cart.deliveryFee)}</span></div>
            <Link
              href={cart.canCheckout ? "/checkout" : "/cart"}
              onClick={() => setOpen(false)}
              className={`mt-2 flex h-14 items-center justify-between rounded-2xl px-5 font-bold text-white transition ${cart.canCheckout ? "bg-tangerine hover:bg-tangerine-dark" : "bg-muted"}`}
            >
              <span>{cart.canCheckout ? "Checkout" : "Review bag"}</span>
              <span className="tabular-nums">{taka(cart.total + cart.deliveryFee)}</span>
            </Link>
          </div>
        </>
      )}
    </Drawer>
  );
}
