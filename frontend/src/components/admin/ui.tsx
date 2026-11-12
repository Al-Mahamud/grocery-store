import Link from "next/link";

export const btn = {
  primary: "inline-flex items-center justify-center gap-1.5 rounded-xl bg-tangerine px-4 py-2.5 text-sm font-bold text-white transition hover:bg-tangerine-dark disabled:cursor-not-allowed disabled:opacity-50",
  dark: "inline-flex items-center justify-center gap-1.5 rounded-xl bg-ink px-4 py-2.5 text-sm font-bold text-white transition hover:bg-ink-soft disabled:cursor-not-allowed disabled:opacity-50",
  ghost: "inline-flex items-center justify-center gap-1.5 rounded-xl border-2 border-line bg-white px-4 py-2 text-sm font-bold transition hover:border-ink/40 disabled:cursor-not-allowed disabled:opacity-50",
  danger: "inline-flex items-center justify-center gap-1.5 rounded-xl bg-berry px-4 py-2.5 text-sm font-bold text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50",
};

export const input = "w-full rounded-xl border-2 border-line bg-white px-3 py-2 text-sm outline-none transition focus:border-tangerine";

export function PageHeader({ title, note, children }: { title: string; note?: React.ReactNode; children?: React.ReactNode }) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="font-display text-3xl font-extrabold tracking-tight">{title}</h1>
        {note && <p className="mt-0.5 text-sm text-muted">{note}</p>}
      </div>
      {children && <div className="flex flex-wrap gap-2">{children}</div>}
    </div>
  );
}

export function Card({ title, action, className = "", children }: { title?: React.ReactNode; action?: React.ReactNode; className?: string; children: React.ReactNode }) {
  return (
    <section className={`rounded-2xl border border-line bg-white p-4 md:p-5 ${className}`}>
      {(title || action) && (
        <div className="mb-3 flex items-center justify-between gap-3">
          {title && <h2 className="font-display text-lg font-bold">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function Stat({ label, value, note, href, tone = "plain" }: { label: string; value: React.ReactNode; note?: string; href?: string; tone?: "plain" | "warn" | "good" }) {
  const toneClass = tone === "warn" ? "border-tangerine/50 bg-tangerine-soft" : tone === "good" ? "border-leaf/40 bg-leaf-soft" : "border-line bg-white";
  const body = (
    <>
      <p className="text-xs font-bold uppercase tracking-wide text-muted">{label}</p>
      <p className="mt-1 font-display text-3xl font-extrabold tabular-nums">{value}</p>
      {note && <p className="mt-0.5 text-xs text-muted">{note}</p>}
    </>
  );
  return href ? (
    <Link href={href} className={`block rounded-2xl border p-4 transition hover:border-ink/40 ${toneClass}`}>{body}</Link>
  ) : (
    <div className={`rounded-2xl border p-4 ${toneClass}`}>{body}</div>
  );
}

export function Empty({ children }: { children: React.ReactNode }) {
  return <p className="rounded-2xl border border-dashed border-line bg-white px-4 py-10 text-center text-sm text-muted">{children}</p>;
}

export function Loading() {
  return <div className="h-64 animate-pulse rounded-2xl bg-white" aria-busy="true" />;
}

export function PaymentBadge({ method, status }: { method: "COD" | "ONLINE"; status: "UNPAID" | "PAID" | "REFUNDED" }) {
  const text = status === "REFUNDED" ? "Refunded" : status === "PAID" ? (method === "COD" ? "Cash paid" : "Paid online") : method === "COD" ? "Cash due" : "Unpaid";
  const tone = status === "PAID" ? "bg-leaf-soft text-leaf" : status === "REFUNDED" ? "bg-cream-deep text-ink-soft" : method === "COD" ? "bg-cream text-ink-soft" : "bg-berry/10 text-berry";
  return <span className={`inline-flex rounded-lg px-2 py-0.5 text-xs font-bold ${tone}`}>{text}</span>;
}
