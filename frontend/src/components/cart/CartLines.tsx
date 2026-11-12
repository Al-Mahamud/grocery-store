"use client";

import Link from "next/link";
import { Trash2 } from "lucide-react";
import type { CartLine } from "@/lib/types";
import { taka } from "@/lib/money";
import { useCart } from "./CartProvider";
import { QuantityControl } from "./QuantityControl";

const problems = { UNAVAILABLE: "No longer sold — please remove.", OUT_OF_STOCK: "Sold out — please remove.", NOT_ENOUGH_STOCK: "Fewer left — lower the quantity." } as const;

/* eslint-disable @next/next/no-img-element */
export function CartLines({ lines }: { lines: CartLine[] }) {
  const { setQuantity, setOpen } = useCart();
  return (
    <ul className="divide-y divide-line">
      {lines.map((line) => (
        <li key={line.packId} className="flex gap-3 py-3">
          <Link href={`/p/${line.slug}`} onClick={() => setOpen(false)} className="h-16 w-16 shrink-0 overflow-hidden rounded-xl border border-line bg-white">
            {line.image && <img src={line.image} alt="" className="h-full w-full object-contain" />}
          </Link>
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <Link href={`/p/${line.slug}`} onClick={() => setOpen(false)} className="line-clamp-1 text-sm font-semibold hover:text-tangerine">{line.name}</Link>
                <p className="text-xs text-muted">{line.packLabel} · {taka(line.unitPrice)} each</p>
              </div>
              <button type="button" aria-label={`Remove ${line.name}`} onClick={() => setQuantity(line.packId, 0)} className="grid h-7 w-7 shrink-0 place-items-center rounded-lg text-muted hover:bg-cream hover:text-berry"><Trash2 size={14} /></button>
            </div>
            <div className="mt-2 flex items-center justify-between gap-3">
              <div className="w-28">
                <QuantityControl pack={{ id: line.packId, label: line.packLabel, amount: 1, unit: "PCS", price: line.unitPrice, compareAtPrice: line.compareAtPrice, unitPrice: null, stock: line.stock, maxPerOrder: line.maxPerOrder }} name={line.name} />
              </div>
              <div className="text-right">
                <p className="text-sm font-bold tabular-nums">{taka(line.lineTotal)}</p>
                {line.compareAtPrice && <p className="text-[0.6875rem] font-semibold text-leaf">Save {taka((line.compareAtPrice - line.unitPrice) * line.quantity)}</p>}
              </div>
            </div>
            {line.problem && <p role="alert" className="mt-1 text-xs font-semibold text-berry">{problems[line.problem]}</p>}
          </div>
        </li>
      ))}
    </ul>
  );
}
