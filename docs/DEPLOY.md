# Haat — putting the shop online

Everything runs on one Linux server with Docker: PostgreSQL, the API, the website, and Caddy,
which gets the HTTPS certificate and is the only part reachable from the internet
(`deploy/compose.yaml`). Every push to `main` runs the tests (`.github/workflows/ci.yml`), and
the **Deploy drill** workflow builds this exact setup on a throwaway machine, signs in, backs up,
wipes and restores it (`.github/workflows/deploy-drill.yml`).

## 1. What you need

| What | Notes |
|---|---|
| A server | Ubuntu with Docker and the compose plugin; 2 CPUs and 4 GB memory is plenty to start |
| A domain | An `A` record pointing at the server, e.g. `haat.example` |
| SSL Wireless account | An API token and an approved sender id (SID). Without texts nobody can sign up |
| SSLCommerz account | Store id and password. Optional: without it the shop is cash on delivery only |
| Off-site storage (recommended) | Any [rclone](https://rclone.org) remote for copies of the backups |

## 2. Before the first build

1. **Contact details and wording.** `frontend/src/config/site.ts` still has placeholder contact
   details (`+880 1700-000000`, `hello@haat.example`). The information pages in
   `frontend/src/config/pages.ts` are drafts: check every promise, and have Terms and Privacy read
   by a lawyer.
2. **Fees and slots.** The real rules are in `backend/src/main/resources/application.yml`
   (`store.delivery`, `store.slots`). `site.ts` repeats the fees for display only; keep them equal.
3. **Settings.** `cp deploy/.env.example deploy/.env` and fill it in (it holds passwords: keep it
   private, it is never committed):
   - `DOMAIN` — the shop's address, without `https://`.
   - `POSTGRES_PASSWORD` — long and random: `openssl rand -base64 30`.
   - `SMS_DRIVER=sslwireless`, `SSLWIRELESS_API_TOKEN`, `SSLWIRELESS_SID`.
   - `SSLCOMMERZ_STORE_ID`, `SSLCOMMERZ_STORE_PASSWORD`, and `SSLCOMMERZ_BASE_URL`: the sandbox
     until SSLCommerz approves the live store, then `https://securepay.sslcommerz.com`.
   - `ADMIN_PHONE`, `ADMIN_PASSWORD` (10+ characters) — the first staff account.

The API refuses to start with unsafe settings (an `http://` address, texts that only go to the log,
an empty database password) and says which one to fix.

## 3. First start

```sh
git clone https://github.com/Al-Mahamud/grocery-store.git /srv/haat && cd /srv/haat
cp deploy/.env.example deploy/.env   # fill it in
docker compose -f deploy/compose.yaml --env-file deploy/.env up -d --build
docker compose -f deploy/compose.yaml --env-file deploy/.env ps   # all "healthy" after a minute or two
```

Then, at `https://<DOMAIN>/admin`:

1. Sign in with `ADMIN_PHONE` / `ADMIN_PASSWORD`, change the password under **Team → My password**,
   then delete the two `ADMIN_` lines from `deploy/.env`.
2. **Team**: add everyone who packs or answers the phone, each with their own login.
3. **Categories** and **Delivery**: a new shop starts with the usual grocery aisles and 16 Dhaka
   areas; rename, add or switch off as needed. Set slot capacity for days with fewer riders.
4. **Products**: add products with their packs, prices, stock and a JPEG/PNG/WebP picture. Set
   each to **On sale** when ready.
5. **Coupons**: none exist until you make them.

## 4. Online payment

In the SSLCommerz merchant panel, set the IPN address to
`https://<DOMAIN>/api/v1/payments/sslcommerz/ipn`. Before switching to the live address, make one
real sandbox payment end to end (place an order, pay, see it confirmed in the admin). A payment
that arrives after an order was cancelled shows as **Refund due**; refund it in the SSLCommerz panel
and record the reference on the order.

## 5. A test (staging) server

Same steps with `STAGING=true` and `SMS_DRIVER=log`: texts are only written to the log
(`docker compose ... logs backend | grep SMS` shows sign-up codes), and the sandbox is fine. Never
let customers use a staging server.

## 6. Backups

`scripts/backup.sh` saves the database and the product pictures to `./backups`, checks the dump
is readable, keeps 14 days, and copies to `BACKUP_REMOTE` if set. Run it every night:

```sh
# crontab -e on the server (03:15 server time)
15 3 * * * cd /srv/haat && BACKUP_REMOTE=offsite:haat-backups scripts/backup.sh >> backups/backup.log 2>&1
```

To restore (the shop is down while it runs, and the current data is replaced):

```sh
scripts/restore.sh backups/db-<time>.dump backups/storage-<time>.tar.gz
```

The Deploy drill workflow does a full backup-wipe-restore every Monday; run it from the Actions tab
after changing anything under `deploy/` or `scripts/`.

## 7. Updates

```sh
cd /srv/haat && scripts/backup.sh && git pull \
  && docker compose -f deploy/compose.yaml --env-file deploy/.env up -d --build
```

Database changes (Flyway migrations) run by themselves when the API starts. Merge Dependabot's
weekly update proposals only when CI is green.

## 8. Keeping an eye on it

- `docker compose -f deploy/compose.yaml --env-file deploy/.env ps` — every service healthy.
- `... logs --tail=200 backend` — the API logs one JSON line per event.
- The admin **Text messages** page shows texts that failed, with a Retry button.
- An outside uptime check on `https://<DOMAIN>/robots.txt` tells you when the site is down.

## 9. Not covered yet

- SSL Wireless was built from their published API and has not been tried with a real account:
  send yourself a sign-up code before launch.
- Refunds are recorded by hand (reference from the SSLCommerz panel), not sent through their API.
- One server: no automatic failover. Backups are the safety net; practise a restore.
