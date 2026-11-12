import { PackageCheck, Truck } from "lucide-react";
import type { Cart } from "@/lib/types";
import { taka } from "@/lib/money";

/** Minimum order first; once that is met, progress towards free delivery. */
export function Progress({ cart }: { cart: Cart }) {
  if (cart.remainingForMinimum > 0) {
    const pct = Math.min(100, (cart.subtotal / cart.minimumOrder) * 100);
    return (
      <Bar icon={<PackageCheck size={16} />} pct={pct} tone="tangerine">
        Add <strong>{taka(cart.remainingForMinimum)}</strong> more to reach the {taka(cart.minimumOrder)} minimum
      </Bar>
    );
  }
  if (cart.remainingForFreeDelivery > 0) {
    const pct = Math.min(100, (cart.total / cart.freeDeliveryOver) * 100);
    return (
      <Bar icon={<Truck size={16} />} pct={pct} tone="leaf">
        Add <strong>{taka(cart.remainingForFreeDelivery)}</strong> more for free delivery
      </Bar>
    );
  }
  return <Bar icon={<Truck size={16} />} pct={100} tone="leaf"><strong>Free delivery unlocked!</strong></Bar>;
}

function Bar({ icon, pct, tone, children }: { icon: React.ReactNode; pct: number; tone: "leaf" | "tangerine"; children: React.ReactNode }) {
  return (
    <div className={`rounded-2xl p-3 ${tone === "leaf" ? "bg-leaf-soft" : "bg-tangerine-soft"}`}>
      <p className="flex items-center gap-2 text-xs [&_strong]:font-bold">
        <span className={tone === "leaf" ? "text-leaf" : "text-tangerine"}>{icon}</span>{children}
      </p>
      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white">
        <div className={`h-full rounded-full transition-[width] duration-500 ${tone === "leaf" ? "bg-leaf" : "bg-tangerine"}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
