import type { Metadata } from "next";
import { Listing } from "@/components/shop/Listing";
import type { SearchParams } from "@/lib/listing";

export const metadata: Metadata = { title: "Deals" };

export default async function DealsPage({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const params = await searchParams;
  return <Listing scope={{ deal: "true", sort: (Array.isArray(params.sort) ? params.sort[0] : params.sort) ?? "discount" }} searchParams={params} title="Today's deals" subtitle="Lower prices on everyday favourites, while stocks last." hideDeal />;
}
