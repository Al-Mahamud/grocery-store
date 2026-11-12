import Link from "next/link";
import { site } from "@/config/site";

export function Logo({ light = false }: { light?: boolean }) {
  return (
    <Link href="/" aria-label={`${site.name} home`} className="inline-flex items-center gap-1.5">
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-tangerine font-display text-lg font-extrabold text-white">h</span>
      <span className={`font-display text-2xl font-extrabold tracking-tight ${light ? "text-white" : "text-ink"}`}>
        haat<span className="text-tangerine">.</span>
      </span>
    </Link>
  );
}
