import type { OrderView } from "./types";

export type Staff = { id: number; name: string; phone: string };
export type OrderStatus = OrderView["status"];

export type AdminSlot = { date: string; start: string; end: string };

export type AdminRow = {
  number: string;
  status: OrderStatus;
  paymentMethod: "COD" | "ONLINE";
  paymentStatus: "UNPAID" | "PAID" | "REFUNDED";
  placedAt: string;
  name: string;
  phone: string;
  area: string;
  slot: AdminSlot;
  itemCount: number;
  total: number;
};

export type AdminOrderPage = { items: AdminRow[]; page: number; pageSize: number; total: number; counts: Record<string, number> };

export type AdminAttempt = { tranId: string; amount: number; status: string; cardType: string | null; bankTranId: string | null; at: string };

export type AdminOrderDetail = {
  order: OrderView;
  next: OrderStatus[];
  attempts: AdminAttempt[];
  refundedAt: string | null;
  refundReference: string | null;
  refundDue: boolean;
  customerId: number | null;
  customerOrders: number;
};

export type BoardSlot = { start: string; end: string; label: string; booked: number; capacity: number; custom: boolean; note: string | null; past: boolean };
export type BoardDay = { date: string; label: string; slots: BoardSlot[] };

export type Dashboard = {
  ordersToday: number;
  revenueToday: number;
  averageBasket: number;
  open: Record<string, number>;
  refundsDue: number;
  lowStock: number;
  week: { date: string; orders: number; revenue: number }[];
  slots: BoardDay[];
  recent: AdminRow[];
};

export type Picking = {
  date: string;
  start: string;
  end: string;
  orders: {
    number: string;
    status: OrderStatus;
    name: string;
    phone: string;
    area: string;
    address: string;
    note: string | null;
    substitution: "CALL" | "REPLACE" | "REMOVE";
    paymentMethod: "COD" | "ONLINE";
    paymentStatus: "UNPAID" | "PAID" | "REFUNDED";
    total: number;
    items: { name: string; packLabel: string; quantity: number }[];
  }[];
  lines: { category: string; name: string; packLabel: string; sku: string; image: string | null; quantity: number; orders: string[] }[];
};

export type Unit = "G" | "KG" | "ML" | "L" | "PCS";
export type ProductStatus = "DRAFT" | "ACTIVE" | "ARCHIVED";

export type AdminPack = { id: number | null; label: string; amount: number | string; unit: Unit; sku: string; price: number | string; compareAtPrice: number | string | null; stock: number | string; maxPerOrder: number | string };

export type AdminProduct = {
  id: number;
  name: string;
  localName: string | null;
  slug: string;
  categoryId: number;
  category: string;
  brand: string | null;
  description: string;
  storage: string | null;
  origin: string | null;
  organic: boolean;
  local: boolean;
  fresh: boolean;
  weighed: boolean;
  featured: boolean;
  status: ProductStatus;
  image: string | null;
  soldCount: number;
  packs: AdminPack[];
};

export type AdminProductRow = { id: number; name: string; localName: string | null; slug: string; image: string | null; category: string; brand: string | null; status: ProductStatus; packCount: number; fromPrice: number; stock: number; low: boolean; soldCount: number };
export type AdminProductPage = { items: AdminProductRow[]; page: number; pageSize: number; total: number };

export type StockRow = { packId: number; productId: number; product: string; image: string | null; label: string; sku: string; price: number; stock: number; maxPerOrder: number; status: ProductStatus };

export type Coupon = { id: number; code: string; description: string; kind: "PERCENT" | "FIXED" | "FREE_SHIPPING"; value: number; maxDiscount: number | null; minSubtotal: number; startsAt: string | null; endsAt: string | null; usageLimit: number | null; usedCount: number; active: boolean };

export type AdminArea = { id: number; name: string; slug: string; active: boolean; orders: number };
