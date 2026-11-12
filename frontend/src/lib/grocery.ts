import type { Day, Pack } from "./types";
import { taka } from "./money";

export function unitPriceText(pack: Pack): string | null {
  return pack.unitPrice ? `${taka(pack.unitPrice.price)}/${pack.unitPrice.per}` : null;
}

export function percentOffPack(pack: Pack): number | null {
  return pack.compareAtPrice ? Math.round(((pack.compareAtPrice - pack.price) / pack.compareAtPrice) * 100) : null;
}

/** The first slot that can still be booked, for "Next delivery: Today 6–8 pm". */
export function nextSlot(days: Day[]): { day: string; label: string } | null {
  for (const day of days) {
    const slot = day.slots.find((s) => s.available);
    if (slot) return { day: day.label, label: slot.label };
  }
  return null;
}
