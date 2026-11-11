# Haat — online grocery shop: frontend and backend plan

Haat (হাট, "village market") is a working name for an online grocery shop in Dhaka: fresh
vegetables, fruit, fish and meat, staples, dairy, snacks, drinks and household goods, delivered in a
time slot the customer chooses. The name, colours and contact details each live in one file.

## 1. Goals

- Fast, friendly shopping on a phone: search as you type, add to bag straight from the grid, change
  quantities without leaving the page.
- Clear prices: every pack shows its price per kg, litre or piece, and deals show the saving.
- Predictable delivery: choose a delivery area and a two-hour slot; full slots are not offered.
- The owner manages products, stock, prices, slots, areas and orders in an admin area.

Out of scope for v1: marketplace sellers, subscriptions ("every Friday"), live rider tracking, app.

## 2. What makes a grocery shop different

| Topic | Decision |
|---|---|
| Packs | A product comes in packs (500 g, 1 kg, 5 kg; 1 L, 2 L; 12 pcs). Each pack has its own price and stock |
| Unit price | Each pack stores amount + unit (g, kg, ml, l, pcs) so the site shows ৳/kg, ৳/L or ৳/pc |
| Weighed produce | Vegetables, fish and meat are sold by approximate weight; the final weight can differ slightly (shown in copy) |
| Quick add | Product cards have an Add button that turns into − 1 + ; the bag is shared by every page |
| Limits | Each pack can have a "max per order" (e.g. 5 bottles of oil during shortages) |
| Delivery areas | Only listed Dhaka areas are served; checkout asks for the area from that list |
| Slots | Two-hour slots (8–10 am … 8–10 pm) for today and the next two days; a slot closes 2 hours before it starts or when it reaches its capacity |
| Fees | Minimum order ৳300; delivery ৳49; free over ৳1,500 |
| Out of stock at packing | Customer chooses: call me / replace with similar / remove the item |
| Badges | Organic, Local, Deal, Fresh today |

## 3. Architecture

```text
Browser ──► Next.js (frontend, :3000) ──► Spring Boot API (backend, :8080) ──► PostgreSQL
            server-renders pages            /api/v1/** JSON                     Flyway migrations
            forwards /api and /media        session cookie + CSRF               images on disk
```

Java 17, Spring Boot 4.1, Spring JDBC, PostgreSQL 18, embedded PostgreSQL for tests and local runs
(no Docker). Next.js 16, React 19, Tailwind CSS 4, TypeScript.

## 4. Backend plan

### 4.1 Packages

| Package | Owns |
|---|---|
| `catalog` | categories (two levels, with icons), products, packs, search, suggestions, filters |
| `delivery` | served areas, slot rules and slot availability |
| `checkout` | bag (cookie), coupons, minimum order, delivery fee, placing orders with a slot |
| `order` | orders, items, status history, tracking |
| `customer` | accounts, sign-in, saved addresses, "buy again" list (phase 4) |
| `payment` | SSLCommerz and a fake local gateway (phase 5) |
| `admin` | products, stock, slots, areas, orders, picking list (phase 6) |
| `shared` | errors, security, settings, phone numbers, clock |

### 4.2 Data model

```text
categories(id, parent_id, name, slug, icon, color, position, image_url)
products(id, name, local_name, slug, category_id, brand, description, storage, origin,
         organic, local, fresh, weighed, image_url, status, featured, sold_count, created_at, search)
packs(id, product_id, label, amount, unit[G|KG|ML|L|PCS], sku, price, compare_at_price,
      stock, max_per_order, position)
delivery_areas(id, name, slug, active)
carts, cart_items(cart_id, pack_id, quantity), coupons
orders(..., area_id, area_name, slot_date, slot_start, slot_end, substitution, subtotal,
       discount, delivery_fee, total, payment_method, status ...)
order_items(..., product_name, pack_label, unit_price, quantity)
order_events(order_id, status, note, actor, created_at)
```

Slot times and capacity come from settings (`store.slots`), so no table is needed in v1; the
number of orders already booked for a date and start time decides whether a slot is full.

### 4.3 Order statuses

```text
PENDING_PAYMENT ─paid─► PLACED ─► PACKING ─► OUT_FOR_DELIVERY ─► DELIVERED
        └─expired─► CANCELLED ◄── (PLACED or PACKING) — stock goes back
Cash-on-delivery orders start at PLACED.
```

### 4.4 API (under `/api/v1`)

- `GET /home`, `GET /categories`, `GET /categories/{slug}`
- `GET /products?category=&q=&brand=&organic=&local=&deal=&sort=&page=` — cards plus facets
- `GET /products/{slug}`, `GET /products/{slug}/related`, `GET /search/suggest?q=`
- `GET /delivery/areas`, `GET /delivery/slots`
- `GET/POST/PATCH/DELETE /cart…`, `POST/DELETE /cart/coupon`
- `POST /checkout/quote`, `POST /checkout` (area, slot, substitution, address, payment)
- `GET /orders/{number}?token=`, `GET /orders/track?number=&phone=`
- Phases 4–7: `/auth/*`, `/account/*` (addresses, buy again), `/payments/*`, `/admin/*`

## 5. Frontend plan

### 5.1 Look and feel

- Fresh and bright but tidy: white product cards on a warm cream background, deep indigo ink,
  tangerine for actions, a fresh green for "added" and "in stock", sunny yellow for deals.
- Type: Bricolage Grotesque for headings (friendly, modern), DM Sans for everything else.
- Dense but calm grids (2 columns on phones, up to 6 on wide screens), square product images on
  white, medium-rounded corners.
- Always-visible essentials: search bar, delivery area, bag total.

### 5.2 Pages

| Route | Content |
|---|---|
| `/` | promo tiles, category icon grid, deals, best sellers, fresh produce, how delivery works |
| `/c/[slug]` | category sidebar, sub-category chips, filters (brand, organic, local, deals), sort, grid |
| `/search?q=` | same grid; the header shows suggestions while typing |
| `/deals` | everything on offer |
| `/p/[slug]` | image, packs with unit prices, quantity, storage and origin, related |
| bag drawer, `/cart` | quantities, minimum-order and free-delivery progress, coupon |
| `/checkout` | contact, area and address, slot picker, substitution choice, payment, summary |
| `/order/[number]`, `/track` | thank-you with slot and timeline; guest tracking |
| `/account/*` | phase 4 (orders, addresses, buy again) |
| `/admin/*` | phase 6 (products, stock, orders, picking list, slots, areas) |

## 6. Delivery phases

| # | Phase | Contents | Status |
|---|---|---|---|
| 0 | Plan and skeleton | this plan, both projects, README | |
| 1 | Catalog backend | schema, packs with unit prices, filters, suggestions, home, demo groceries, tests | |
| 2 | Storefront | design system, header with live search, home, category pages, product page | |
| 3 | Bag and checkout | quick add, bag, coupons, minimum order, areas, slots, substitutions, COD orders, tracking | |
| 4 | Accounts | register/sign in, bag merge, saved addresses, order history, buy again | |
| 5 | Online payment | SSLCommerz with a fake local gateway, payment expiry | |
| 6 | Admin | dashboard, products and packs, stock, orders and picking list, slots and areas, coupons | |
| 7 | Notifications and SEO | order SMS (confirmed, out for delivery), sitemap, metadata | |
| 8 | Launch readiness | Docker, Caddy, backups, CI, real payment credentials | |
