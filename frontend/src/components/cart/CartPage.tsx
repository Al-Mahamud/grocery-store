"use client";

import Link from "next/link";
import { ArrowRight, ShoppingBasket } from "lucide-react";
import { taka } from "@/lib/money";
import { useCart } from "./CartProvider";
import { CartLines } from "./CartLines";
import { CouponForm } from "./CouponForm";
import { Progress } from "./Progress";

export function CartPage() {
  const { cart } = useCart();
  if (!cart) return <div className="container-page min-h-[50vh] py-10"><div className="h-10 w-48 animate-pulse rounded-xl bg-white" /></div>;
  if (cart.items.length === 0) {
    return (
      <div className="container-page grid min-h-[55vh] place-items-center text-center">
        <div>
          <span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-tangerine-soft text-tangerine"><ShoppingBasket size={30} /></span>
          <h1 className="mt-5 font-display text-4xl font-extrabold">Your bag is empty</h1>
          <Link href="/" className="mt-6 inline-block rounded-xl bg-tangerine px-7 py-3.5 text-sm font-bold text-white">Start shopping</Link>
        </div>
      </div>
    );
  }
  return (
    <div className="container-page py-6 md:py-10">
      <h1 className="font-display text-3xl font-extrabold md:text-4xl">Your bag <span className="text-muted">· {cart.itemCount} items</span></h1>
      <div className="mt-6 grid gap-5 lg:grid-cols-12">
        <div className="space-y-3 rounded-3xl bg-white p-4 md:p-6 lg:col-span-8">
          <Progress cart={cart} />
          <CartLines lines={cart.items} />
        </div>
        <aside className="lg:col-span-4">
          <div className="space-y-4 rounded-3xl bg-white p-5 md:p-6 lg:sticky lg:top-40">
            <h2 className="font-display text-xl font-bold">Order summary</h2>
            <CouponForm cart={cart} />
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between"><dt className="text-muted">Items</dt><dd className="tabular-nums">{taka(cart.subtotal)}</dd></div>
              {cart.discount > 0 && <div className="flex justify-between text-leaf"><dt>Promo</dt><dd className="tabular-nums">−{taka(cart.discount)}</dd></div>}
              <div className="flex justify-between"><dt className="text-muted">Delivery</dt><dd className="tabular-nums">{cart.deliveryFee === 0 ? "Free" : taka(cart.deliveryFee)}</dd></div>
              {cart.savings + cart.discount > 0 && <div className="flex justify-between font-semibold text-leaf"><dt>Total savings</dt><dd className="tabular-nums">{taka(cart.savings + cart.discount)}</dd></div>}
            </dl>
            <div className="flex items-baseline justify-between border-t border-line pt-3">
              <span className="font-bold">Total</span>
              <span className="font-display text-2xl font-extrabold tabular-nums">{taka(cart.total + cart.deliveryFee)}</span>
            </div>
            {cart.canCheckout ? (
              <Link href="/checkout" className="flex h-14 items-center justify-center gap-2 rounded-2xl bg-tangerine font-bold text-white hover:bg-tangerine-dark">Choose delivery slot <ArrowRight size={18} /></Link>
            ) : (
              <p role="alert" className="rounded-xl bg-tangerine-soft p-3 text-sm font-semibold text-tangerine-dark">
                {cart.remainingForMinimum > 0 ? `Add ${taka(cart.remainingForMinimum)} more to reach the ${taka(cart.minimumOrder)} minimum order.` : "Some items need your attention."}
              </p>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
