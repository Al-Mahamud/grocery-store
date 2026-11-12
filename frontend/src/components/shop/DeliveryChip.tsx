import { Clock, MapPin } from "lucide-react";
import type { Day } from "@/lib/types";
import { nextSlot } from "@/lib/grocery";

export function DeliveryChip({ days }: { days: Day[] }) {
  const next = nextSlot(days);
  return (
    <div className="hidden min-w-0 items-center gap-2.5 rounded-2xl bg-leaf-soft px-3.5 py-2 lg:flex">
      <span className="grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-leaf text-white"><Clock size={16} /></span>
      <div className="min-w-0 leading-tight">
        <p className="text-[0.6875rem] font-semibold uppercase tracking-wide text-leaf">Next delivery</p>
        <p className="truncate text-sm font-bold">{next ? `${next.day}, ${next.label}` : "Slots open soon"}</p>
      </div>
      <span className="ml-1 hidden items-center gap-1 border-l border-leaf/20 pl-3 text-xs font-semibold text-ink-soft xl:flex"><MapPin size={13} /> Dhaka</span>
    </div>
  );
}
