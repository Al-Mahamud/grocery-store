export type UnitPrice = { price: number; per: "kg" | "L" | "pc" };

export type Pack = {
  id: number;
  label: string;
  amount: number;
  unit: "G" | "KG" | "ML" | "L" | "PCS";
  price: number;
  compareAtPrice: number | null;
  unitPrice: UnitPrice | null;
  stock: number;
  maxPerOrder: number;
};

export type ProductCard = {
  id: number;
  slug: string;
  name: string;
  localName: string | null;
  brand: string | null;
  category: string;
  image: string | null;
  organic: boolean;
  local: boolean;
  fresh: boolean;
  weighed: boolean;
  packs: Pack[];
  soldOut: boolean;
};

export type CategoryNode = {
  id: number;
  name: string;
  slug: string;
  icon: string;
  color: string;
  image: string | null;
  productCount: number;
  children: CategoryNode[];
};

export type FacetValue = { value: string; label: string; count: number; selected: boolean };

export type ProductPage = {
  items: ProductCard[];
  page: number;
  pageSize: number;
  total: number;
  facets: { brands: FacetValue[]; organic: number; local: number; deals: number };
};

export type ProductDetail = Omit<ProductCard, "category" | "soldOut"> & {
  breadcrumb: { name: string; slug: string }[];
  description: string;
  storage: string | null;
  origin: string | null;
};

export type Suggestion = { slug: string; name: string; localName: string | null; image: string | null; price: number; packLabel: string };

export type Home = {
  categories: CategoryNode[];
  deals: ProductCard[];
  bestSellers: ProductCard[];
  fresh: ProductCard[];
  pantry: ProductCard[];
};

export type Area = { id: number; name: string; slug: string };
export type Slot = { start: string; end: string; label: string; available: boolean; remaining: number };
export type Day = { date: string; label: string; slots: Slot[] };

export type CartLine = {
  packId: number;
  productId: number;
  slug: string;
  name: string;
  packLabel: string;
  sku: string;
  image: string | null;
  unitPrice: number;
  compareAtPrice: number | null;
  quantity: number;
  stock: number;
  maxPerOrder: number;
  lineTotal: number;
  problem: "UNAVAILABLE" | "OUT_OF_STOCK" | "NOT_ENOUGH_STOCK" | null;
};

export type Cart = {
  items: CartLine[];
  itemCount: number;
  subtotal: number;
  savings: number;
  discount: number;
  coupon: { code: string; description: string; discount: number; freeShipping: boolean } | null;
  couponProblem: string | null;
  total: number;
  deliveryFee: number;
  freeDeliveryOver: number;
  remainingForFreeDelivery: number;
  minimumOrder: number;
  remainingForMinimum: number;
  canCheckout: boolean;
};

export type Quote = { subtotal: number; discount: number; deliveryFee: number; total: number; couponCode: string | null; problem: string | null };

export type OrderView = {
  number: string;
  status: "PENDING_PAYMENT" | "PLACED" | "PACKING" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELLED";
  paymentMethod: "COD" | "ONLINE";
  paymentStatus: "UNPAID" | "PAID" | "REFUNDED";
  placedAt: string;
  name: string;
  phone: string;
  email: string | null;
  address: string;
  area: string;
  note: string | null;
  slot: { date: string; start: string; end: string };
  substitution: "CALL" | "REPLACE" | "REMOVE";
  items: { slug: string; name: string; packLabel: string; image: string | null; unitPrice: number; quantity: number; lineTotal: number }[];
  subtotal: number;
  discount: number;
  deliveryFee: number;
  total: number;
  couponCode: string | null;
  events: { status: string; note: string | null; at: string }[];
};
