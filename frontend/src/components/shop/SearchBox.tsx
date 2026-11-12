"use client";

import { useEffect, useId, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Search, X } from "lucide-react";
import type { Suggestion } from "@/lib/types";
import { taka } from "@/lib/money";
import { request } from "@/lib/client";

/* eslint-disable @next/next/no-img-element */
/** Search field that suggests products while typing; Enter shows all results. */
export function SearchBox({ className = "" }: { className?: string }) {
  const [q, setQ] = useState("");
  const [items, setItems] = useState<Suggestion[]>([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const router = useRouter();
  const listId = useId();
  const box = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const text = q.trim();
    if (text.length < 2) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- clear suggestions for short input.
      setItems([]);
      return;
    }
    const timer = setTimeout(() => {
      request<Suggestion[]>("GET", `/search/suggest?q=${encodeURIComponent(text)}`).then(setItems).catch(() => setItems([]));
    }, 150);
    return () => clearTimeout(timer);
  }, [q]);

  useEffect(() => {
    const close = (e: MouseEvent) => box.current && !box.current.contains(e.target as Node) && setOpen(false);
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  const go = (path: string) => {
    setOpen(false);
    setActive(-1);
    router.push(path);
  };

  return (
    <div ref={box} className={`relative ${className}`}>
      <form
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          const picked = items[active];
          if (picked) go(`/p/${picked.slug}`);
          else if (q.trim()) go(`/search?q=${encodeURIComponent(q.trim())}`);
        }}
        className="flex h-12 items-center gap-2 rounded-2xl border-2 border-transparent bg-cream px-4 transition focus-within:border-tangerine focus-within:bg-white"
      >
        <Search size={19} className="shrink-0 text-muted" />
        <input
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setOpen(true);
            setActive(-1);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") { e.preventDefault(); setActive((a) => Math.min(a + 1, items.length - 1)); }
            if (e.key === "ArrowUp") { e.preventDefault(); setActive((a) => Math.max(a - 1, -1)); }
            if (e.key === "Escape") setOpen(false);
          }}
          placeholder="Search rice, eggs, ডিম…"
          aria-label="Search products"
          role="combobox"
          aria-expanded={open && items.length > 0}
          aria-controls={listId}
          aria-activedescendant={active >= 0 ? `${listId}-${active}` : undefined}
          className="min-w-0 flex-1 bg-transparent text-[0.9375rem] outline-none placeholder:text-muted"
        />
        {q && (
          <button type="button" aria-label="Clear search" onClick={() => { setQ(""); setItems([]); }} className="grid h-7 w-7 place-items-center rounded-full text-muted hover:bg-cream-deep">
            <X size={15} />
          </button>
        )}
      </form>
      {open && items.length > 0 && (
        <ul id={listId} role="listbox" className="absolute inset-x-0 top-full z-50 mt-2 overflow-hidden rounded-2xl border border-line bg-white p-1.5 shadow-2xl">
          {items.map((s, i) => (
            <li key={s.slug} id={`${listId}-${i}`} role="option" aria-selected={i === active}>
              <button type="button" onMouseEnter={() => setActive(i)} onClick={() => go(`/p/${s.slug}`)} className={`flex w-full items-center gap-3 rounded-xl p-2 text-left ${i === active ? "bg-cream" : ""}`}>
                <span className="h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-cream">{s.image && <img src={s.image} alt="" className="h-full w-full object-contain" />}</span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold">{s.name}</span>
                  <span className="block truncate text-xs text-muted">{s.localName && <span className="font-bangla">{s.localName} · </span>}{s.packLabel}</span>
                </span>
                <span className="text-sm font-bold tabular-nums">{taka(s.price)}</span>
              </button>
            </li>
          ))}
          <li>
            <button type="button" onClick={() => go(`/search?q=${encodeURIComponent(q.trim())}`)} className="w-full rounded-xl p-2.5 text-left text-sm font-bold text-tangerine hover:bg-cream">
              See all results for &ldquo;{q.trim()}&rdquo;
            </button>
          </li>
        </ul>
      )}
    </div>
  );
}
