# Haat — online grocery shop

Groceries for Dhaka, delivered in a two-hour slot: fresh produce, fish and meat, staples, dairy,
snacks, drinks and household goods. See [docs/PLAN.md](docs/PLAN.md) for the full frontend and
backend plan and the delivery phases.

## Layout

```text
backend/    Spring Boot API (Java 17, PostgreSQL, Flyway)
frontend/   Next.js storefront (TypeScript, Tailwind CSS)
docs/       Plan
```

## Backend

Needs JDK 17+. On this machine: `export JAVA_HOME=~/.jdks/jdk-17`.

```bash
cd backend
./mvnw verify                 # build and run all tests (embedded PostgreSQL, no Docker)
./mvnw spring-boot:test-run   # run on :8080 with a fresh database full of demo groceries
```

Delivery fee, free-delivery threshold, minimum order and slot times are in
`backend/src/main/resources/application.yml` under `store.delivery` and `store.slots`.

Demo coupons: `WELCOME15` (15% off over ৳500, up to ৳150), `FRESH50` (৳50 off over ৳800),
`FREEDEL` (free delivery over ৳600), `OLD10` (expired, for testing). Demo brands are made up.
Product pictures are drawn by `backend/src/test/java/.../support/DemoArt.java`.

Admin area: http://localhost:3000/admin, demo staff `01999999999`, password `haat-admin-1`
(tomorrow's 10 am–12 pm slot has four orders to pack). On a real server, set `ADMIN_PHONE` and
`ADMIN_PASSWORD` once to create the first staff account.

Demo account: mobile `01755555555`, password `haat-demo` (Farzana, with a saved address and three
past orders, so "Buy again" and order history have something to show).

## Frontend

Needs Node.js 20.9+. The backend must be running; Next.js forwards `/api/v1` and `/media` to it
(`BACKEND_URL`, default `http://localhost:8080`).

```bash
cd frontend
npm install
npm run dev       # http://localhost:3000
```

Online payment in local runs goes to a fake SSLCommerz page (test sources only, no real money). For
the real sandbox, set `SSLCOMMERZ_STORE_ID`, `SSLCOMMERZ_STORE_PASSWORD` and a public `WEB_URL` the
gateway can reach.
