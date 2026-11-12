import type { BoardSlot } from "@/lib/admin-types";

/** A slot's bookings against its capacity, as a small bar. */
export function SlotLoad({ slot }: { slot: BoardSlot }) {
  const full = slot.capacity === 0 ? 1 : Math.min(1, slot.booked / slot.capacity);
  const tone = slot.capacity === 0 ? "bg-muted" : full >= 1 ? "bg-berry" : full >= 0.7 ? "bg-tangerine" : "bg-leaf";
  return (
    <div className={slot.past ? "opacity-45" : ""}>
      <div className="flex items-baseline justify-between text-xs">
        <span className="font-bold">{slot.label}</span>
        <span className="tabular-nums text-muted">{slot.capacity === 0 ? "Closed" : `${slot.booked}/${slot.capacity}`}</span>
      </div>
      <div className="mt-1 h-2 overflow-hidden rounded-full bg-cream-deep">
        <div className={`h-full rounded-full ${tone}`} style={{ width: `${slot.capacity === 0 ? 100 : Math.max(full * 100, slot.booked ? 6 : 0)}%` }} />
      </div>
    </div>
  );
}
