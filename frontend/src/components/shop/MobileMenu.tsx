"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { BadgePercent, Menu, X } from "lucide-react";
import type { CategoryNode } from "@/lib/types";
import { Drawer } from "@/components/ui/Drawer";
import { CategoryIcon } from "@/components/ui/CategoryIcon";

export function MobileMenu({ categories }: { categories: CategoryNode[] }) {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  // eslint-disable-next-line react-hooks/set-state-in-effect -- close after navigating.
  useEffect(() => setOpen(false), [pathname]);
  return (
    <>
      <button type="button" aria-label="Open categories" onClick={() => setOpen(true)} className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-cream md:hidden"><Menu size={21} /></button>
      <Drawer open={open} onClose={() => setOpen(false)} side="left" label="Categories">
        <div className="flex h-16 items-center justify-between px-5">
          <span className="font-display text-xl font-bold">Shop by category</span>
          <button type="button" aria-label="Close" onClick={() => setOpen(false)} className="grid h-10 w-10 place-items-center rounded-xl bg-cream"><X size={18} /></button>
        </div>
        <nav className="flex-1 space-y-5 overflow-y-auto px-4 pb-6">
          <Link href="/deals" className="flex items-center gap-3 rounded-2xl bg-sun-soft p-3 font-bold"><BadgePercent size={20} /> Today&rsquo;s deals</Link>
          {categories.map((top) => (
            <div key={top.id}>
              <p className="mb-2 px-1 text-xs font-bold uppercase tracking-wide text-muted">{top.name}</p>
              <div className="grid grid-cols-2 gap-2">
                {top.children.map((c) => (
                  <Link key={c.id} href={`/c/${c.slug}`} className="flex items-center gap-2 rounded-2xl p-2.5 text-sm font-semibold" style={{ backgroundColor: c.color }}>
                    <span className="text-ink-soft"><CategoryIcon name={c.icon} size={18} /></span>{c.name}
                  </Link>
                ))}
              </div>
            </div>
          ))}
        </nav>
      </Drawer>
    </>
  );
}
