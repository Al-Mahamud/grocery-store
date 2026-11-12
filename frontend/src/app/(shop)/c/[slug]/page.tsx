import type { Metadata } from "next";
import { Listing } from "@/components/shop/Listing";
import type { SearchParams } from "@/lib/listing";
import { api } from "@/lib/api";

type Props = { params: Promise<{ slug: string }>; searchParams: Promise<SearchParams> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const category = await api.category((await params).slug);
  const names = category.children.slice(0, 4).map((c) => c.name.toLowerCase()).join(", ");
  return {
    title: category.name,
    description: `Buy ${category.name.toLowerCase()}${names ? ` — ${names} and more` : ""} online in Dhaka. ${category.productCount} products, delivered in a two-hour slot you choose.`,
    alternates: { canonical: `/c/${category.slug}` },
  };
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const { slug } = await params;
  const [category, tree] = await Promise.all([api.category(slug), api.categories()]);
  const top = tree.find((t) => t.slug === slug || t.children.some((c) => c.slug === slug)) ?? category;
  return (
    <Listing
      scope={{ category: slug }}
      searchParams={await searchParams}
      title={category.name}
      subtitle={`${category.productCount} products${top.slug !== slug ? ` in ${top.name}` : ""}`}
      activeSlug={slug}
      chips={[{ name: `All ${top.name.toLowerCase()}`, href: `/c/${top.slug}`, active: top.slug === slug }, ...top.children.map((c) => ({ name: c.name, href: `/c/${c.slug}`, active: c.slug === slug, icon: c.icon }))]}
    />
  );
}
