import Link from "next/link";
import { BadgePercent } from "lucide-react";
import { api } from "@/lib/api";
import { site } from "@/config/site";
import { CategoryIcon } from "@/components/ui/CategoryIcon";
import { Logo } from "./Logo";
import { SearchBox } from "./SearchBox";
import { BagButton } from "./BagButton";
import { DeliveryChip } from "./DeliveryChip";
import { MobileMenu } from "./MobileMenu";
import { AccountButton } from "@/components/account/AccountButton";

export async function Header() {
  const [categories, days] = await Promise.all([api.categories(), api.slots()]);
  return (
    <header className="sticky top-0 z-40 border-b border-line bg-white/95 backdrop-blur">
      <div className="bg-ink text-white">
        <p className="container-page py-1.5 text-center text-xs">
          Free delivery over <strong className="text-sun">৳{site.freeDeliveryOver.toLocaleString("en-IN")}</strong><span className="hidden sm:inline"> · Minimum order ৳{site.minimumOrder} · Cash on delivery</span>
        </p>
      </div>
      <div className="container-page flex items-center gap-3 py-3 md:gap-5">
        <MobileMenu categories={categories} />
        <Logo />
        <DeliveryChip days={days} />
        <SearchBox className="hidden flex-1 md:block" />
        <div className="ml-auto flex items-center gap-1.5 md:ml-0">
          <AccountButton />
          <BagButton />
        </div>
      </div>
      <div className="container-page pb-3 md:hidden"><SearchBox /></div>
      <nav aria-label="Categories" className="hidden border-t border-line/70 md:block">
        <ul className="container-page no-scrollbar flex items-center gap-1 overflow-x-auto py-2">
          <li>
            <Link href="/deals" className="inline-flex items-center gap-1.5 rounded-xl bg-sun-soft px-3 py-2 text-sm font-bold text-ink hover:bg-sun"><BadgePercent size={17} /> Deals</Link>
          </li>
          {categories.flatMap((top) => top.children).map((c) => (
            <li key={c.id}>
              <Link href={`/c/${c.slug}`} className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-xl px-3 py-2 text-sm font-semibold text-ink-soft hover:bg-cream hover:text-ink">
                <span className="text-tangerine"><CategoryIcon name={c.icon} size={16} /></span> {c.name}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
    </header>
  );
}
