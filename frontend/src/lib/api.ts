import "server-only";
import { notFound } from "next/navigation";
import type { CategoryNode, Day, Home, OrderView, ProductCard, ProductDetail, ProductPage } from "./types";

const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

async function get<T>(path: string, revalidate = 30): Promise<T> {
  const response = await fetch(`${backend}/api/v1${path}`, { next: { revalidate } });
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error(`GET ${path} failed with ${response.status}`);
  return (await response.json()) as T;
}

export const api = {
  home: () => get<Home>("/home"),
  categories: () => get<CategoryNode[]>("/categories", 300),
  category: (slug: string) => get<CategoryNode>(`/categories/${encodeURIComponent(slug)}`),
  products: (query: URLSearchParams) => get<ProductPage>(`/products?${query}`),
  product: (slug: string) => get<ProductDetail>(`/products/${encodeURIComponent(slug)}`),
  related: (slug: string) => get<ProductCard[]>(`/products/${encodeURIComponent(slug)}/related`),
  /** Slots fill up, so they are read fresh. */
  slots: async (): Promise<Day[]> => {
    const response = await fetch(`${backend}/api/v1/delivery/slots`, { cache: "no-store" });
    return response.ok ? ((await response.json()) as Day[]) : [];
  },
  order: async (number: string, token: string): Promise<OrderView> => {
    const response = await fetch(`${backend}/api/v1/orders/${encodeURIComponent(number)}?token=${encodeURIComponent(token)}`, { cache: "no-store" });
    if (response.status === 404) notFound();
    if (!response.ok) throw new Error(`Order lookup failed with ${response.status}`);
    return (await response.json()) as OrderView;
  },
};
