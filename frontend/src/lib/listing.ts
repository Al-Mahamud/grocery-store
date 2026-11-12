export const FILTER_KEYS = ["q", "brand", "organic", "local", "deal", "sort"] as const;

export type SearchParams = Record<string, string | string[] | undefined>;

export function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export function productQuery(scope: Record<string, string>, params: SearchParams, pageSize = 30): URLSearchParams {
  const query = new URLSearchParams(scope);
  for (const key of FILTER_KEYS) {
    const value = first(params[key]);
    if (value && !query.has(key)) query.set(key, value);
  }
  query.set("pageSize", String(pageSize));
  return query;
}

export const SORTS = [
  { value: "recommended", label: "Recommended" },
  { value: "popular", label: "Best selling" },
  { value: "discount", label: "Biggest discount" },
  { value: "price_asc", label: "Price: low to high" },
  { value: "price_desc", label: "Price: high to low" },
] as const;
