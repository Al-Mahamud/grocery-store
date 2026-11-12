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
| 0 | Plan and skeleton | this plan, both projects, README | done |
| 1 | Catalog backend | schema, packs with unit prices, filters, suggestions, home, demo groceries, tests | done |
| 2 | Storefront | design system, header with live search, home, category pages, product page | done |
| 3 | Bag and checkout | quick add, bag, coupons, minimum order, areas, slots, substitutions, COD orders, tracking | done |
| 4 | Accounts | register/sign in, bag merge, saved addresses, order history, buy again | done |
| 5 | Online payment | SSLCommerz with a fake local gateway, payment expiry | done |
| 6 | Admin | dashboard, products and packs, stock, orders and picking list, slots and areas, coupons | |
| 7 | Notifications and SEO | order SMS (confirmed, out for delivery), sitemap, metadata | |
| 8 | Launch readiness | Docker, Caddy, backups, CI, real payment credentials | |

### Notes from building

- Order numbers look like `HT10001`. Guests open their thank-you page with a random token in the
  link; tracking needs the order number plus the phone number used.
- Booking a slot takes a database advisory lock per slot, so two shoppers can't both take the last
  place. A slot closes 2 hours before it starts (`store.slots.cutoff`).
- The − / + buttons change the number on screen at once and send the exact quantity to the server
  (`PUT /cart/items/{packId}`); if the server refuses (stock, per-order limit), the bag snaps back
  and a short message explains why.
- Search ranks name matches above products that only mention the word in their description;
  Bangla names are matched with a plain "contains" search.
- The home page is built at `next build` time and refreshed every 30 s, so the backend must be
  reachable during a production build (phase 8).
- The footer `/pages/*` links lead nowhere until phase 7.

### Notes from phase 4 (accounts)

- Customers sign in with their mobile number and a password (BCrypt). Sessions live in PostgreSQL
  (Spring Session); the session id changes at sign-in. After 6 wrong passwords for one number (or 30
  from one address) in 15 minutes, sign-in pauses. The counter is in memory, which is fine for one
  server; behind a proxy it must read the real client address (phase 8).
- **Phone numbers are not verified yet.** Anyone can register any number, so guest orders placed
  earlier with that number are deliberately *not* linked to the new account. Phase 7 adds an SMS
  code at sign-up; after that, past guest orders can be linked.
- One bag per customer, shared by every device they sign in on. At sign-in the guest bag joins it:
  if both hold the same pack the larger quantity wins (capped at the per-order limit), and a coupon
  carries over if the account's bag had none. Signing out leaves the bag with the account.
- Changing the password signs out every other device.
- Saved addresses: up to 10, one default (the first one saved). Checkout picks the default and can
  save a new address ("Save this address to my account").
- "Buy again" lists products from past orders (cancelled ones excluded), most often bought first,
  with the pack bought most recently pre-selected. "Order again" raises each pack in the bag to the
  earlier quantity within stock and limits, and lists anything sold out.
- The phone menu, filters and bag drawers render into `<body>`: the sticky header's blur made them
  only as tall as the header before.

### Notes from phase 5 (online payment)

- An online order is saved as PENDING_PAYMENT and holds its stock and slot for 30 minutes
  (`store.payment.expiry`). The shopper goes to SSLCommerz's page (bKash, Nagad, Rocket, cards).
- Nothing the gateway or the browser sends is believed by itself. Every callback is stored in
  `gateway_callbacks`; a payment counts only when SSLCommerz's Validation API confirms the `val_id`
  for the same `tran_id`, in BDT, for the exact amount. Confirming twice changes nothing.
- Three ways a payment is noticed: the IPN, the browser coming back (checked at once, so the shopper
  doesn't wait), and a job every minute that asks the gateway about attempts waiting over 2 minutes.
  The same job cancels orders whose time ran out and gives back stock, sales counts and coupon use.
- Each try is a new attempt (`HT10001-1`, `-2` ...; at most 5). After a failed or cancelled payment
  the order page offers "Pay now" again or "Pay cash on delivery instead".
- Money that arrives after an order was cancelled is recorded (payment PAID, order stays cancelled,
  an event says "Refund it") for the admin to refund in phase 6.
- Settings: `SSLCOMMERZ_STORE_ID`, `SSLCOMMERZ_STORE_PASSWORD`, `SSLCOMMERZ_BASE_URL` (sandbox by
  default; `https://securepay.sslcommerz.com` for live) and `WEB_URL`, the public shop address the
  gateway posts back to. Online payment is hidden until the credentials are set.
- Local runs (`spring-boot:test-run`) use a fake gateway from the test sources with a "TEST GATEWAY
  · NOT REAL MONEY" page; it can pay with bKash/Nagad/card, fail or cancel. It never ships.
- Callback paths skip the CSRF check, which is safe because they are verified with the gateway instead.
