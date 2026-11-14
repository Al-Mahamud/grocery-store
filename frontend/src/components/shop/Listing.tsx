import Link from "next/link";
import { SearchX } from "lucide-react";
import { api } from "@/lib/api";
import { productQuery, type SearchParams } from "@/lib/listing";
import type { CategoryNode } from "@/lib/types";
import { CategoryIcon } from "@/components/ui/CategoryIcon";
import { ActiveFilters, FilterPanel, MobileFilters, SortSelect } from "./Filters";
import { LoadMoreGrid } from "./LoadMoreGrid";

/** Grid page shared by categories, search and deals: category sidebar, filters and products. */
export async function Listing({
  scope,
  searchParams,
  title,
  subtitle,
  activeSlug,
  chips,
  hideDeal = false,
}: {
  scope: Record<string, string>;
  searchParams: SearchParams;
  title: React.ReactNode;
  subtitle?: string;
  activeSlug?: string;
  chips?: { name: string; href: string; active: boolean; icon?: string }[];
  hideDeal?: boolean;
}) {
  const query = productQuery(scope, searchParams);
  // Filters or a search the shopper chose (sorting doesn't narrow anything).
  const narrowed = Object.keys(searchParams).some((k) => k !== "sort" && k !== "page") || "q" in scope;
  const [page, tree] = await Promise.all([api.products(query), api.categories()]);
  return (
    <div className="container-page grid gap-6 pt-5 lg:grid-cols-[16rem_1fr] xl:gap-8">
      <aside className="hidden lg:block">
        <div className="no-scrollbar sticky top-36 max-h-[calc(100dvh-10rem)] space-y-4 overflow-y-auto pb-6">
          <CategoryNav tree={tree} activeSlug={activeSlug} />
          <FilterPanel facets={page.facets} hideDeal={hideDeal} />
        </div>
      </aside>
      <div className="min-w-0">
        <div className="rounded-3xl bg-white p-5 md:p-6">
          <h1 className="font-display text-3xl font-extrabold tracking-tight md:text-4xl">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
          {chips && chips.length > 0 && (
            <div className="no-scrollbar -mx-1 mt-4 flex gap-2 overflow-x-auto px-1">
              {chips.map((c) => (
                <Link key={c.href} href={c.href} aria-current={c.active ? "page" : undefined} className={`inline-flex shrink-0 items-center gap-1.5 rounded-xl px-3.5 py-2 text-sm font-semibold transition ${c.active ? "bg-ink text-white" : "bg-cream hover:bg-cream-deep"}`}>
                  {c.icon && <CategoryIcon name={c.icon} size={15} />}{c.name}
                </Link>
              ))}
            </div>
          )}
        </div>
        <div className="sticky top-[9.5rem] z-20 -mx-4 flex items-center justify-between gap-3 bg-cream/95 px-4 py-3 backdrop-blur md:static md:mx-0 md:px-0">
          <div className="flex items-center gap-3">
            <MobileFilters facets={page.facets} total={page.total} hideDeal={hideDeal} />
            <p className="text-sm text-muted"><strong className="text-ink">{page.total}</strong> products</p>
          </div>
          <SortSelect />
        </div>
        <ActiveFilters />
        {page.items.length === 0 ? (
          <div className="grid place-items-center rounded-3xl bg-white px-6 py-20 text-center">
            <SearchX size={32} className="text-muted" />
            <p className="mt-3 font-display text-2xl font-bold">{narrowed ? "Nothing found" : "Nothing here yet"}</p>
            <p className="mt-1 text-sm text-muted">{narrowed ? "Try another word or remove a filter." : "New products are on their way. Have a look in the other aisles."}</p>
          </div>
        ) : (
          <LoadMoreGrid key={query.toString()} initial={page} query={query.toString()} />
        )}
      </div>
    </div>
  );
}

function CategoryNav({ tree, activeSlug }: { tree: CategoryNode[]; activeSlug?: string }) {
  return (
    <nav aria-label="Categories" className="rounded-2xl bg-white p-3">
      {tree.map((top) => (
        <div key={top.id} className="mb-2 last:mb-0">
          <Link href={`/c/${top.slug}`} className={`block rounded-lg px-2 py-1.5 text-xs font-bold uppercase tracking-wide ${top.slug === activeSlug ? "text-tangerine" : "text-muted hover:text-ink"}`}>{top.name}</Link>
          <ul>
            {top.children.map((c) => (
              <li key={c.id}>
                <Link href={`/c/${c.slug}`} aria-current={c.slug === activeSlug ? "page" : undefined} className={`flex items-center gap-2 rounded-xl px-2 py-1.5 text-sm transition ${c.slug === activeSlug ? "bg-tangerine-soft font-bold text-tangerine-dark" : "hover:bg-cream"}`}>
                  <span className="grid h-7 w-7 place-items-center rounded-lg" style={{ backgroundColor: c.color }}><CategoryIcon name={c.icon} size={15} /></span>
                  <span className="flex-1">{c.name}</span>
                  <span className="text-xs text-muted">{c.productCount}</span>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}
