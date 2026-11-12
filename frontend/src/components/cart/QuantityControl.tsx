"use client";

import { Minus, Plus } from "lucide-react";
import type { Pack } from "@/lib/types";
import { useCart } from "./CartProvider";

/** "Add" until the pack is in the bag, then a − n + stepper. */
export function QuantityControl({ pack, name, size = "md", disabled = false }: { pack: Pack; name: string; size?: "md" | "lg"; disabled?: boolean }) {
  const { quantityOf, setQuantity } = useCart();
  const quantity = quantityOf(pack.id);
  const limit = Math.min(pack.maxPerOrder, pack.stock);
  const height = size === "lg" ? "h-14 text-base" : "h-10 text-sm";

  if (pack.stock === 0 || disabled) {
    return <div className={`${height} grid w-full place-items-center rounded-xl bg-cream text-muted`}>Out of stock</div>;
  }
  if (quantity === 0) {
    return (
      <button
        type="button"
        onClick={() => setQuantity(pack.id, 1)}
        aria-label={`Add ${name} (${pack.label}) to bag`}
        className={`${height} inline-flex w-full items-center justify-center gap-1.5 rounded-xl border-2 border-tangerine bg-white font-bold text-tangerine transition hover:bg-tangerine hover:text-white active:scale-[0.98]`}
      >
        <Plus size={size === "lg" ? 20 : 16} strokeWidth={2.6} /> Add
      </button>
    );
  }
  return (
    <div className={`${height} flex w-full items-center justify-between rounded-xl bg-leaf font-bold text-white`} role="group" aria-label={`${name} quantity`}>
      <button type="button" aria-label="One less" onClick={() => setQuantity(pack.id, quantity - 1)} className="grid h-full w-11 place-items-center rounded-l-xl hover:bg-black/10">
        <Minus size={16} strokeWidth={2.8} />
      </button>
      <span key={quantity} className="animate-pop tabular-nums" aria-live="polite">{quantity}</span>
      <button type="button" aria-label="One more" disabled={quantity >= limit} onClick={() => setQuantity(pack.id, quantity + 1)} className="grid h-full w-11 place-items-center rounded-r-xl hover:bg-black/10 disabled:opacity-40">
        <Plus size={16} strokeWidth={2.8} />
      </button>
    </div>
  );
}
