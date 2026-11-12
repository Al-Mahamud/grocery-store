import Link from "next/link";
import { Phone } from "lucide-react";
import { site } from "@/config/site";
import { Logo } from "./Logo";

const columns = [
  { title: "Shop", links: [["Vegetables", "/c/vegetables"], ["Fish & meat", "/c/fish-meat"], ["Rice & flour", "/c/rice-flour"], ["Milk & dairy", "/c/milk-dairy"], ["Deals", "/deals"]] },
  { title: "Help", links: [["Track your order", "/track"], ["Delivery areas & slots", "/pages/delivery"], ["Returns & refunds", "/pages/returns"], ["Contact", "/pages/contact"]] },
  { title: "Company", links: [["About us", "/pages/about"], ["Careers", "/pages/careers"], ["Privacy", "/pages/privacy"], ["Terms", "/pages/terms"]] },
] as const;

export function Footer() {
  return (
    <footer className="mt-16 bg-ink text-white md:mt-24">
      <div className="container-page grid gap-10 py-12 md:grid-cols-12 md:py-16">
        <div className="md:col-span-4">
          <Logo light />
          <p className="mt-4 max-w-xs text-sm text-white/65">{site.description}</p>
          <p className="mt-6 inline-flex items-center gap-2 rounded-xl bg-white/10 px-4 py-2.5 text-sm font-semibold"><Phone size={15} /> {site.phone} · 8 am – 10 pm</p>
        </div>
        <div className="grid grid-cols-2 gap-8 sm:grid-cols-3 md:col-span-7 md:col-start-6">
          {columns.map((col) => (
            <div key={col.title}>
              <p className="mb-4 text-xs font-bold uppercase tracking-wider text-white/45">{col.title}</p>
              <ul className="space-y-2.5 text-sm">
                {col.links.map(([label, href]) => <li key={href}><Link href={href} className="text-white/80 hover:text-sun">{label}</Link></li>)}
              </ul>
            </div>
          ))}
        </div>
      </div>
      <div className="border-t border-white/10">
        <div className="container-page flex flex-col gap-3 py-6 text-xs text-white/50 sm:flex-row sm:justify-between">
          <p>© {new Date().getFullYear()} {site.name}. {site.address}.</p>
          <p className="flex flex-wrap gap-2">{["Cash on delivery", "bKash", "Nagad", "Cards"].map((m) => <span key={m} className="rounded-md border border-white/15 px-2 py-0.5">{m}</span>)}</p>
        </div>
      </div>
    </footer>
  );
}
