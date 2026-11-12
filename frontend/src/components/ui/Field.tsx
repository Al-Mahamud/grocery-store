/** A labelled form control with a hint or an error underneath. Wraps one input, select or textarea. */
export function Field({ label, hint, error, className = "", children }: { label: string; hint?: string; error?: string; className?: string; children: React.ReactNode }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1 block text-xs font-bold text-ink-soft">{label}</span>
      <span className={`block rounded-xl border-2 bg-white transition focus-within:border-tangerine [&>*]:w-full [&>*]:rounded-xl [&>*]:bg-transparent [&>*]:px-3.5 [&>*]:py-2.5 [&>*]:text-sm [&>*]:outline-none ${error ? "border-berry" : "border-line"}`}>{children}</span>
      {error ? <span className="mt-1 block text-xs font-semibold text-berry">{error}</span> : hint ? <span className="mt-1 block text-xs text-muted">{hint}</span> : null}
    </label>
  );
}
