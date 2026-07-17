# Testing Paymob Payments — Developer Setup Guide

This guide walks through everything needed to test the Paymob payment integration locally, from creating a Paymob account through to confirming a webhook lands correctly and a transaction shows up in history.

---

## 0. Prerequisites

- Docker + Docker Compose installed
- ngrok installed and authenticated (see Step 6)
- A Paymob merchant account — if you don't have one, Step 1 below walks through creating it

---

## 1. Create a Paymob account and complete onboarding

Even for test-mode-only development, Paymob requires an account and a short onboarding flow before it gives you API credentials.

1. Go to https://portal.paymob.com and click **Sign Up**.
2. Fill in your business/merchant details. For local development this can be minimal — you don't need a fully verified live business to use **Test Mode**.
3. Verify your email/phone if prompted.
4. Once logged in, you'll land on the merchant dashboard. In the **top-left corner**, toggle **Test Mode** on. This is what unlocks test-only credentials and test integrations, without needing live business verification or real payment processing approval.
5. Paymob may prompt you to complete additional onboarding steps (business category, expected transaction volume, etc.) before some sections unlock. For test mode, you can usually reach API keys and integrations without finishing every step — if something is greyed out, check whether an onboarding prompt is blocking it.

You do not need to wait for Paymob's live-account approval/verification process to start integrating — test mode is fully self-serve.

---

## 2. Get your credentials from the dashboard

1. **Test Mode** must be toggled on (top-left) — confirms every credential you copy from here on is a test credential, not a live one.
2. **Secret key / Public key** — go to **Settings → Developers** (or **API Keys**, depending on current dashboard labeling). Copy the **test-mode** versions of both.
3. **HMAC secret** — same Developers area, usually listed alongside webhook/callback settings.
4. **Integration ID (card)** — go to **Settings → Payment Integrations**. Test mode auto-provisions a card integration for you; open it and copy the numeric ID shown in the URL/page header (e.g. `Integration #5778473` → `5778473`).
5. **Test card numbers** — search Paymob's docs for their **"Test Credentials"** / **"Test Cards"** page. It publishes fixed numbers that reliably simulate success and decline outcomes.

> Do **not** edit the "Webhook URL" / "Redirect URL" fields shown on the integration detail page in the dashboard — those are Paymob's own internal defaults for that integration, unrelated to your app's `PAYMOB_NOTIFICATION_URL` / `PAYMOB_REDIRECTION_URL`. Your app sends its own callback URLs dynamically with each payment intention request.

> Menu labels shift around in Paymob's dashboard occasionally — treat "Developers" / "Payment Integrations" as the general area to look in, not an exact click path.

---


## 4. `application.yml`

The `paymob:` block must sit at the **top level** of the file (same indentation as `spring:`, `security:`, `app:`) — not nested under `spring:`. `@ConfigurationProperties(prefix = "paymob")` only binds a top-level `paymob` key.

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

Only list an integration method (`card`, `wallet`, etc.) once you have a real numeric ID for it. Don't give an unused method an empty-string default — that field binds to `Integer` and Spring will crash trying to parse an empty string.

---

## 5. Add environment variables to `docker-compose.yml`

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

> **Run `docker compose` commands from the same directory as `docker-compose.yml` and `.env`.** Running from a subdirectory (e.g. the inner backend project folder) can cause Compose to silently miss your `.env` file, leaving vars like `PAYMOB_SECRET_KEY` empty inside the container even though the app appears to start fine. Verify anytime with:
> ```powershell
> docker exec -it career-pilot-api env | grep PAYMOB_SECRET_KEY
> ```

---

## 6. Install and authenticate a tunnel (ngrok or Cloudflare Tunnel)

Paymob needs to reach your local machine over the public internet to deliver webhooks.

> **Known issue: Paymob does not reliably call back to ngrok's free-tier `*.ngrok-free.app`/`*.ngrok-free.dev` domains.** In testing, payments completed successfully on Paymob's side (confirmed via the redirect page showing `success=true`) but the webhook never arrived at ngrok — not even a failed/rejected attempt showed in ngrok's inspector. Sending the same `notification_url` to `webhook.site` instead confirmed Paymob *does* attempt delivery promptly and correctly — the failure is specific to ngrok's free-tier domain, most likely blocklisted by Paymob's outbound callback service. **Recommendation: use Cloudflare Tunnel instead of ngrok for Paymob webhook testing.**

### Option A (recommended): Cloudflare Tunnel

```powershell
winget install --id Cloudflare.cloudflared
cloudflared tunnel --url http://localhost:7070
```
No signup required for a quick/anonymous tunnel. Copy the printed `https://....trycloudflare.com` URL.

> Like ngrok's free tier, a quick Cloudflare Tunnel generates a new random URL every time you restart it — update `.env` and rebuild each time, same as you would with ngrok.

### Option B: ngrok (works for most testing, but not confirmed reliable for Paymob webhooks specifically)

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

If you use ngrok and webhooks aren't arriving despite everything else (URL, port, backend) checking out, switch to Cloudflare Tunnel before spending more time debugging — this was the actual root cause in a real debugging session, and every other layer checked out fine.

---

## 7. Fill in `.env`

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

## 8. Start ngrok and set the notification URL

```powershell
ngrok http <backend host port>
```

> Check the actual host port your backend is mapped to first — `docker ps` shows the real mapping (e.g. `0.0.0.0:7070->8080/tcp`), which may differ from the container-internal port in `docker-compose.yml`.

Copy the forwarding URL it prints, e.g.:
```
Forwarding   https://a1b2c3d4.ngrok-free.app -> http://localhost:7070
```

Set in `.env`:
```
PAYMOB_NOTIFICATION_URL=https://a1b2c3d4.ngrok-free.app/api/v1/payments/webhook/paymob
```

> Free ngrok plans generate a new subdomain every restart unless you have a reserved static domain — update this value (and restart the backend) each time the ngrok URL changes.

---

## 9. Run the app

```powershell
docker compose up --build
```

Always use `--build` after any code or config change — a plain `up` or `restart` reuses the existing image and will not pick up your edits.

Flyway applies migrations automatically on boot (`flyway.enabled: true`).

---

## 10. Test the full flow

1. **Register/log in** a test user via `/api/v1/auth/**` (or `/api/v1/otp/**`) to get a Bearer token.
2. **Initiate a payment:**
   ```
   POST /api/v1/payments/initiate
   Authorization: Bearer <token>
   Content-Type: application/json

   {
     "amount": 10,
     "currency": "EGP",
     "method": "card",
     "provider": "PAYMOB",
     "purchaseType": "COIN_PACK",
     "coinPackSize": 500
   }
   ```
   (Use `"purchaseType": "SUBSCRIPTION"` with a `"tier"` field instead of `coinPackSize` for a subscription purchase.)

   Response includes a `checkoutUrl`.
3. **Open `checkoutUrl`** in a browser and complete payment using a Paymob test card number.
4. **Watch the webhook arrive** — open `http://127.0.0.1:4040` (ngrok's local inspector) to see the request hit your backend in real time.
5. **Check the database:**
   ```bash
   docker exec -it career-pilot-db psql -U career_pilot -d career_pilot -c \
     "select id, status, provider_transaction_id, merchant_order_id, failure_reason from payment_transactions order by id desc limit 5;"
   ```
   Confirm the row flipped from `PENDING` to `CONFIRMED` (or `FAILED` with a real failure reason for a declined card).
6. **Check transaction history:**
   ```
   GET /api/v1/payments/history?page=0&size=10
   Authorization: Bearer <token>
   ```
   Should return a paginated list of your own transactions only.
7. **Test a declined test card too** — confirms the failure path records a real reason, not just a generic message.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| App won't start, Hibernate validation error | Missing `updated_at` column — see Step 3a |
| Webhook hits ngrok inspector but DB never updates | Enum bug (Step 3b) not fixed, or HMAC check failing |
| `PaymobConfig` fields are all `null` at runtime | `paymob:` block still nested under `spring:` in `application.yml` |
| YAML parse error on boot | Leftover `.properties`-style lines (`KEY=value`) in `application.yml` — must be `key: value` |
| `NoSuchBeanDefinitionException: No qualifying bean of type RestTemplate` | Add a `RestTemplate` `@Bean` in `ApplicationConfiguration.java` |
| `docker compose up` fails immediately, unrelated to Paymob | Check for other `:?err` env vars (e.g. `GOOGLE_CLIENT_ID`, `MAIL_USERNAME`) that may be unset in `.env` |
| Webhook never reaches backend at all | `PAYMOB_NOTIFICATION_URL` uses a stale ngrok URL from a previous session — restart ngrok and update `.env` |
| `env \| grep PAYMOB_SECRET_KEY` returns nothing inside the container | You're likely running `docker compose` from the wrong directory (missing `.env`) — `cd` to the folder containing `docker-compose.yml` and rebuild |
| Paymob returns a blank `401 Unauthorized` on every intention request | Secret key isn't reaching the container (see above), or it's stale/regenerated in the dashboard — recheck both |
| `/history` (or any endpoint returning `PaymentTransaction` directly) returns a huge, repeating JSON blob | Bidirectional JPA relationship (`User` <-> `UserRole`) causing infinite recursion — return a DTO (`PaymentTransactionResponse`) instead of the raw entity |
| Validation errors or webhook rejections return `500` instead of `400`/`401` | Check `@ControllerAdvice` ordering — a catch-all `Exception.class` handler in one class can intercept exceptions meant for a more specific handler in another. Give domain-specific handlers an explicit `@Order(0)` and the true fallback handler `@Order(Ordered.LOWEST_PRECEDENCE)` |
| Payment redirect shows `success=true`/`APPROVED`, DB stays `PENDING`, ngrok inspector shows **zero requests at all** (not even failed ones), URL/port/tunnel all verified correct | Paymob likely isn't calling back to ngrok's free-tier domain at all — confirmed by testing the same `notification_url` against `webhook.site`, which received the callback promptly. Switch to Cloudflare Tunnel (`cloudflared tunnel --url http://localhost:<port>`) instead of ngrok |