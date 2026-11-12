"use client";

import { useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { ProductCard } from "@/lib/types";
import { ProductCardView } from "./ProductCardView";

export function ProductRail({ products }: { products: ProductCard[] }) {
  const track = useRef<HTMLDivElement>(null);
  const scroll = (d: 1 | -1) => track.current?.scrollBy({ left: d * track.current.clientWidth * 0.85, behavior: "smooth" });
  return (
    <div className="relative">
      <div ref={track} className="no-scrollbar -mx-4 flex snap-x gap-2.5 overflow-x-auto scroll-px-4 px-4 md:-mx-6 md:gap-3 md:scroll-px-6 md:px-6 xl:-mx-8 xl:scroll-px-8 xl:px-8">
        {products.map((p) => (
          <div key={p.id} className="w-[46%] shrink-0 snap-start sm:w-[31%] lg:w-[23%] xl:w-[18.5%] 2xl:w-[15.6%]"><ProductCardView product={p} /></div>
        ))}
      </div>
      <button type="button" aria-label="Scroll left" onClick={() => scroll(-1)} className="absolute -left-3 top-1/3 hidden h-10 w-10 place-items-center rounded-full border border-line bg-white shadow-md hover:bg-cream md:grid"><ChevronLeft size={18} /></button>
      <button type="button" aria-label="Scroll right" onClick={() => scroll(1)} className="absolute -right-3 top-1/3 hidden h-10 w-10 place-items-center rounded-full border border-line bg-white shadow-md hover:bg-cream md:grid"><ChevronRight size={18} /></button>
    </div>
  );
}
