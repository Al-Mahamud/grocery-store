import Link from "next/link";
import { ChevronRight } from "lucide-react";

export function SectionHeading({ title, note, href, linkLabel = "See all" }: { title: React.ReactNode; note?: string; href?: string; linkLabel?: string }) {
  return (
    <div className="mb-4 flex items-end justify-between gap-4 md:mb-5">
      <div>
        <h2 className="font-display text-2xl font-bold tracking-tight md:text-3xl">{title}</h2>
        {note && <p className="mt-0.5 text-sm text-muted">{note}</p>}
      </div>
      {href && (
        <Link href={href} className="inline-flex shrink-0 items-center gap-0.5 text-sm font-bold text-tangerine hover:text-tangerine-dark">
          {linkLabel} <ChevronRight size={16} />
        </Link>
      )}
    </div>
  );
}
