import type { MetadataRoute } from "next";
import { api } from "@/lib/api";
import { site } from "@/config/site";
import { pageSlugs } from "@/config/pages";

export const revalidate = 3600;

/** Every page worth finding: home, deals, categories, products and the information pages. */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const data = await api.sitemap().catch(() => ({ categories: [], products: [] }));
  return [
    { url: site.url, changeFrequency: "daily", priority: 1 },
    { url: `${site.url}/deals`, changeFrequency: "daily", priority: 0.8 },
    ...data.categories.map((slug) => ({ url: `${site.url}/c/${slug}`, changeFrequency: "daily" as const, priority: 0.7 })),
    ...data.products.map((p) => ({ url: `${site.url}/p/${p.slug}`, lastModified: new Date(p.updatedAt), changeFrequency: "weekly" as const, priority: 0.6 })),
    ...pageSlugs.map((slug) => ({ url: `${site.url}/pages/${slug}`, changeFrequency: "monthly" as const, priority: 0.3 })),
  ];
}
