# Testing Paymob Payments — Developer Setup Guide

This guide walks through everything needed to test the Paymob payment integration locally, from fixing known blockers to confirming a webhook lands correctly.

---

## 0. Prerequisites

- Docker + Docker Compose installed
- A Paymob merchant account (free to create): https://portal.paymob.com
- ngrok installed and authenticated (see Step 4)

---

## 1. Fix the two code blockers

These must be fixed before the app will even start or before webhooks will work. If someone else already fixed these, skip to Step 2.

### 1a. Missing `updated_at` column

`PaymentTransaction.java` has an `updatedAt` field mapped to a `updated_at` column, but the schema doesn't have it yet. With `hibernate.ddl-auto: validate` in `application.yml`, the app will refuse to start until this exists.

Add a new migration file — **do not edit `V2` if it has already run**:

```sql
-- src/main/resources/db/migration/V3__add_updated_at.sql
ALTER TABLE payment_transactions ADD COLUMN updated_at TIMESTAMP;
```

### 1b. Enum comparison bug in webhook handler

In `PaymentServiceImpl.handleWebhook`, this line silently no-ops on every webhook because it compares an enum to a `String`:

```java
// Before (broken):
if (!"PENDING".equals(tx.getStatus())) {
    return;
}
```

Fix:

```java
import static com.careerpilot.backend.entity.ENUMs.PaymentStatus.PENDING;
// ...
if (tx.getStatus() != PENDING) {
    return;
}
```

Without this fix, every transaction stays `PENDING` forever and nothing throws an error — it just looks like the webhook isn't doing anything.

---

## 2. Fix `application.yml`

The `paymob:` block must sit at the **top level** of the file (same indentation as `spring:`, `security:`, `app:`) — not nested under `spring:`. `@ConfigurationProperties(prefix = "paymob")` only looks for a top-level `paymob` key.

```yaml
paymob:
  base-url: ${PAYMOB_BASE_URL:https://accept.paymob.com}
  secret-key: ${PAYMOB_SECRET_KEY}
  public-key: ${PAYMOB_PUBLIC_KEY}
  hmac-secret: ${PAYMOB_HMAC_SECRET}
  notification-url: ${PAYMOB_NOTIFICATION_URL}
  redirection-url: ${PAYMOB_REDIRECTION_URL:http://localhost:5173/payment/result}
  integration-ids:
    card: ${PAYMOB_INTEGRATION_ID_CARD}
```

**Note:** only list an integration method (`card`, `wallet`, etc.) in `integration-ids` once you actually have a real numeric ID for it. Don't give an unused method an empty-string default (e.g. `${PAYMOB_INTEGRATION_ID_WALLET:-}`) — that field binds to `Integer` and Spring will crash trying to parse an empty string. Leave unused methods out of the map entirely.

---

## 3. Add environment variables to `docker-compose.yml`

Under the `backend` service's `environment:` block:

```yaml
      PAYMOB_BASE_URL: ${PAYMOB_BASE_URL:-https://accept.paymob.com}
      PAYMOB_SECRET_KEY: ${PAYMOB_SECRET_KEY:?err}
      PAYMOB_PUBLIC_KEY: ${PAYMOB_PUBLIC_KEY:?err}
      PAYMOB_HMAC_SECRET: ${PAYMOB_HMAC_SECRET:?err}
      PAYMOB_NOTIFICATION_URL: ${PAYMOB_NOTIFICATION_URL:?err}
      PAYMOB_REDIRECTION_URL: ${PAYMOB_REDIRECTION_URL:-http://localhost:5173/payment/result}
      PAYMOB_INTEGRATION_ID_CARD: ${PAYMOB_INTEGRATION_ID_CARD:?err}
```

Add a `PAYMOB_INTEGRATION_ID_WALLET` line (and its `application.yml` counterpart) only once wallet support is actually needed.

---

## 4. Install and authenticate ngrok

Paymob needs to reach your local machine over the public internet to deliver webhooks. ngrok creates a temporary public URL that tunnels to `localhost:8080`.

**Install (pick one):**
```powershell
choco install ngrok
# or
winget install ngrok.ngrok
# or download manually from https://ngrok.com/download
```

**Authenticate (one-time, required):**
1. Sign up at https://dashboard.ngrok.com/signup
2. Copy your token from https://dashboard.ngrok.com/get-started/your-authtoken
3. Run:
   ```powershell
   ngrok config add-authtoken YOUR_TOKEN_HERE
   ```

---

## 5. Get test credentials from the Paymob dashboard

1. Log into https://portal.paymob.com (create a merchant account if needed).
2. Turn on **Test Mode** (toggle, top-left of the dashboard). This gives you test-only credentials — no real money moves.
3. **Secret key / Public key** — Settings → Developers / API Keys (test-mode versions).
4. **HMAC secret** — same Developers area, near webhook/callback settings.
5. **Integration ID (card)** — Settings → Payment Integrations. Test mode auto-provisions a card integration; open it and copy the numeric ID from the URL/page (e.g. `Integration #5778473` → `5778473`).
6. **Test card numbers** — see Paymob's docs "Test Credentials" page for numbers that reliably simulate success/decline.

> Do **not** edit the "Webhook URL" / "Redirect URL" fields shown on the integration detail page in the dashboard — those are Paymob's own internal defaults for that integration, unrelated to your app's `PAYMOB_NOTIFICATION_URL` / `PAYMOB_REDIRECTION_URL`. Your app sends its own callback URLs dynamically with each payment intention request.

---

## 6. Fill in `.env`

Create/update the root `.env` file (same one with `DB_PASSWORD`, `JWT_SECRET`, etc.):

```
PAYMOB_BASE_URL=https://accept.paymob.com
PAYMOB_SECRET_KEY=<from dashboard>
PAYMOB_PUBLIC_KEY=<from dashboard>
PAYMOB_HMAC_SECRET=<from dashboard>
PAYMOB_REDIRECTION_URL=http://localhost:5173/payment/result
PAYMOB_INTEGRATION_ID_CARD=<from dashboard>
PAYMOB_NOTIFICATION_URL=   # filled in next step
```

---

## 7. Start ngrok and set the notification URL

```powershell
ngrok http 8080
```

Copy the forwarding URL it prints, e.g.:
```
Forwarding   https://a1b2c3d4.ngrok-free.app -> http://localhost:8080
```

Set in `.env`:
```
PAYMOB_NOTIFICATION_URL=https://a1b2c3d4.ngrok-free.app/api/payments/webhook/paymob
```

> Free ngrok plans generate a new subdomain every restart — update this value (and restart the backend) each time you restart ngrok.

---

## 8. Run the app

```powershell
docker compose up --build
```

Flyway applies migrations automatically on boot (`flyway.enabled: true`).

---

## 9. Test the full flow

1. **Register/log in** a test user via `/api/v1/auth/**` to get a Bearer token.
2. **Initiate a payment:**
   ```
   POST /api/payments/initiate
   Authorization: Bearer <token>
   Content-Type: application/json

   {
     "amount": 10,
     "currency": "EGP",
     "method": "card"
   }
   ```
   Response includes a `checkoutUrl`.
3. **Open `checkoutUrl`** in a browser and complete payment using a Paymob test card number.
4. **Watch the webhook arrive** — open `http://127.0.0.1:4040` (ngrok's local inspector) to see the request hit your backend in real time. Confirms the HMAC check passes and the payload shape matches what `PaymobPaymentProvider` expects.
5. **Check the database:**
   ```bash
   docker exec -it career-pilot-db psql -U career_pilot -d career_pilot -c \
     "select id, status, provider_transaction_id, merchant_order_id from payment_transactions order by id desc limit 5;"
   ```
   Confirm the row flipped from `PENDING` to `CONFIRMED`.
6. **Test a declined test card too** — not just a successful one. This is the path where the enum bug (Step 1b) would have silently hidden a failure, just less obviously than the success path.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| App won't start, Hibernate validation error | Missing `updated_at` column — see Step 1a |
| Webhook hits ngrok inspector but DB never updates | Enum bug (Step 1b) not fixed, or HMAC check failing |
| `PaymobConfig` fields are all `null` at runtime | `paymob:` block still nested under `spring:` in `application.yml` |
| YAML parse error on boot | Leftover `.properties`-style lines (`KEY=value`) in `application.yml` — must be `key: value` |
| `NoSuchBeanDefinitionException: No qualifying bean of type RestTemplate` | Add a `RestTemplate` `@Bean` in `ApplicationConfiguration.java` |
| `docker compose up` fails immediately, unrelated to Paymob | Check for other `:?err` env vars (e.g. `GOOGLE_CLIENT_ID`, `MAIL_USERNAME`) that may be unset in `.env` |
| Webhook never reaches backend at all | `PAYMOB_NOTIFICATION_URL` uses a stale ngrok URL from a previous session — restart ngrok and update `.env` |