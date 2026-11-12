import type { OrderView } from "@/lib/types";

const styles: Record<OrderView["status"], { label: string; className: string }> = {
  PENDING_PAYMENT: { label: "Awaiting payment", className: "bg-cream-deep text-ink-soft" },
  PLACED: { label: "Placed", className: "bg-sun-soft text-ink" },
  PACKING: { label: "Packing", className: "bg-tangerine-soft text-tangerine-dark" },
  OUT_FOR_DELIVERY: { label: "On the way", className: "bg-tangerine text-white" },
  DELIVERED: { label: "Delivered", className: "bg-leaf-soft text-leaf" },
  CANCELLED: { label: "Cancelled", className: "bg-berry/10 text-berry" },
};

export function StatusPill({ status }: { status: OrderView["status"] }) {
  const s = styles[status];
  return <span className={`inline-flex rounded-lg px-2.5 py-1 text-xs font-bold ${s.className}`}>{s.label}</span>;
}
