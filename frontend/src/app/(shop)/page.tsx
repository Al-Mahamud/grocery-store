import Link from "next/link";
import { ArrowRight, BadgePercent, Clock, Leaf, ShieldCheck, Truck } from "lucide-react";
import { api } from "@/lib/api";
import { site } from "@/config/site";
import { nextSlot } from "@/lib/grocery";
import { taka } from "@/lib/money";
import type { ProductCard } from "@/lib/types";
import { CategoryIcon } from "@/components/ui/CategoryIcon";
import { ProductRail } from "@/components/shop/ProductRail";
import { ProductGrid } from "@/components/shop/ProductGrid";
import { SectionHeading } from "@/components/shop/SectionHeading";

/* eslint-disable @next/next/no-img-element */
export default async function HomePage() {
  const [home, days] = await Promise.all([api.home(), api.slots()]);
  const next = nextSlot(days);
  const subcategories = home.categories.flatMap((top) => top.children);
  const veg = home.fresh.filter((p) => p.category === "Vegetables").slice(0, 4);

  return (
    <div className="container-page space-y-12 pt-4 md:space-y-16 md:pt-6">
      <section className="grid gap-3 lg:grid-cols-3">
        <div className="relative overflow-hidden rounded-3xl bg-leaf p-6 text-white md:p-10 lg:col-span-2 lg:min-h-[22rem]">
          <div className="relative z-10 max-w-md">
            <p className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1 text-xs font-bold"><Clock size={13} /> {next ? `Next slot: ${next.day}, ${next.label}` : "Book a delivery slot"}</p>
            <h1 className="mt-4 font-display text-4xl font-extrabold leading-[1.02] tracking-tight md:text-6xl">Fresh groceries, in the slot you choose.</h1>
            <p className="mt-4 max-w-sm text-white/85">Vegetables picked yesterday, fish from the river, and every staple — delivered across Dhaka in two-hour windows.</p>
            <div className="mt-7 flex flex-wrap gap-2.5">
              <Link href="/c/fresh" className="inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3.5 text-sm font-bold text-leaf hover:bg-sun hover:text-ink">Shop fresh <ArrowRight size={16} /></Link>
              <Link href="/deals" className="inline-flex items-center gap-2 rounded-xl bg-white/15 px-5 py-3.5 text-sm font-bold hover:bg-white/25">Today&rsquo;s deals</Link>
            </div>
          </div>
          <div className="pointer-events-none absolute -bottom-6 -right-6 hidden w-[44%] max-w-md grid-cols-2 gap-3 md:grid lg:right-6 lg:bottom-auto lg:top-1/2 lg:-translate-y-1/2">
            {veg.map((p, i) => (
              <div key={p.id} className={`aspect-square overflow-hidden rounded-3xl bg-white shadow-2xl ${i % 2 ? "translate-y-6" : ""}`} style={{ rotate: `${(i % 2 ? 4 : -4)}deg` }}>
                {p.image && <img src={p.image} alt="" className="h-full w-full object-contain" />}
              </div>
            ))}
          </div>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
          <Link href="/deals" className="group relative overflow-hidden rounded-3xl bg-sun p-6">
            <BadgePercent size={28} />
            <p className="mt-6 font-display text-3xl font-extrabold leading-none">Up to 15% off</p>
            <p className="mt-1 text-sm font-semibold text-ink-soft">on staples, dairy and snacks this week</p>
            <ArrowRight className="absolute bottom-6 right-6 transition-transform group-hover:translate-x-1" />
          </Link>
          <div className="relative overflow-hidden rounded-3xl bg-ink p-6 text-white">
            <Truck size={28} className="text-sun" />
            <p className="mt-6 font-display text-3xl font-extrabold leading-none">Free delivery</p>
            <p className="mt-1 text-sm text-white/70">on orders over {taka(site.freeDeliveryOver)}. Otherwise just {taka(site.deliveryFee)}.</p>
          </div>
        </div>
      </section>

      <section aria-labelledby="categories">
        <SectionHeading title={<span id="categories">Shop by category</span>} />
        <ul className="grid grid-cols-3 gap-2.5 sm:grid-cols-5 lg:grid-cols-8 xl:grid-cols-[repeat(15,minmax(0,1fr))]">
          {subcategories.map((c) => (
            <li key={c.id}>
              <Link href={`/c/${c.slug}`} className="group flex h-full flex-col items-center gap-2 rounded-2xl p-2 text-center transition hover:bg-white">
                <span className="relative grid aspect-square w-full place-items-center overflow-hidden rounded-2xl transition group-hover:-translate-y-0.5" style={{ backgroundColor: c.color }}>
                  {c.image ? <img src={c.image} alt="" className="h-[85%] w-[85%] object-contain mix-blend-multiply" /> : <CategoryIcon name={c.icon} size={28} />}
                </span>
                <span className="text-xs font-semibold leading-tight">{c.name}</span>
              </Link>
            </li>
          ))}
        </ul>
      </section>

      <Rail title="Today's deals" note="Prices drop while stocks last" href="/deals" products={home.deals} />
      <Rail title={<>Fresh today <Leaf className="inline text-leaf" size={22} /></>} note="Arrives at our hub every morning" href="/c/fresh" products={home.fresh} />

      <section>
        <SectionHeading title="Best sellers" note="What Dhaka is buying this week" />
        <ProductGrid products={home.bestSellers} />
      </section>

      <Rail title="Pantry staples" note="Rice, oil, dal and spices" href="/c/pantry" products={home.pantry} />

      <section className="grid gap-3 md:grid-cols-3">
        {[
          { icon: Clock, title: "Pick a two-hour slot", text: "Today, tomorrow or the day after — from 8 am to 10 pm." },
          { icon: Leaf, title: "We pick it fresh", text: "Our team packs your order just before the van leaves, cold items in cool bags." },
          { icon: ShieldCheck, title: "Check, then pay", text: "Pay cash at the door. Anything not right? The rider takes it back on the spot." },
        ].map(({ icon: Icon, title, text }, i) => (
          <div key={title} className="flex gap-4 rounded-3xl bg-white p-6">
            <span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-tangerine-soft text-tangerine"><Icon size={22} /></span>
            <div>
              <p className="text-xs font-bold text-muted">Step {i + 1}</p>
              <p className="font-display text-xl font-bold">{title}</p>
              <p className="mt-1 text-sm text-muted">{text}</p>
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}

function Rail({ title, note, href, products }: { title: React.ReactNode; note: string; href: string; products: ProductCard[] }) {
  if (products.length === 0) return null;
  return (
    <section>
      <SectionHeading title={title} note={note} href={href} />
      <ProductRail products={products} />
    </section>
  );
}
