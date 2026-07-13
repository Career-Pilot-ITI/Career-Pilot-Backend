# Career Pilot Backend

A Spring Boot backend for the Career Pilot platform — handles auth, rate limiting, and WhatsApp OTP delivery.

## What's inside

- **JWT auth** — login, register, refresh tokens, logout
- **Phone OTP** — verify your number via WhatsApp (WireWeb) or simulated console logging
- **Email verification** — old-school email code verification + password reset
- **Rate limiting** — Bucket4j on Redis, protects your endpoints from abuse
- **Refresh tokens** — stored in Redis, rotated on use, revoked on logout
- **PostgreSQL** — Flyway migrations, no auto-DDL in production
- **Docker** — everything runs in containers

## Quick start

### 1. Clone and enter

```bash
git clone git@github.com:Career-Pilot-ITI/Career-Pilot-Backend.git
cd Career-Pilot-Backend
```

### 2. Set up environment

Copy the example env and fill in the blanks:

```bash
cp .env.example .env
```

Minimal `.env` to get running (no email, no OAuth):

```env
DB_PASSWORD=changeme
JWT_SECRET=JfDznjRadS1ckJhWUKVy2KPCvLEUyrpNPYAW7cZAwDw=
OTP_PROVIDER=simulated
```

| Variable | Required | Default | What it does |
|---|---|---|---|
| `DB_PASSWORD` | ✅ | — | PostgreSQL password |
| `JWT_SECRET` | ✅ | — | 256-bit Base64 key for signing JWTs |
| `JWT_TIME` | ❌ | `3600000` (1h) | Token expiry in ms |
| `OTP_PROVIDER` | ❌ | `simulated` | `simulated` (logs to console) or `wireweb` |
| `REDIS_PASSWORD` | ❌ | empty | Redis password |
| `MAIL_USERNAME` | ❌ | — | Gmail SMTP (for email verification) |
| `MAIL_PASSWORD` | ❌ | — | Gmail app password |

### 3. Start everything

```bash
docker compose up --build
```

This starts:
- **PostgreSQL 15** on port `5433`
- **Redis 7** on port `6379`
- **Backend** on port `8080`

Wait a few seconds for the backend to pass its health check, then:

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## Testing the OTP flow (simulated)

No WhatsApp needed. OTPs print to the container logs.

```bash
# 1. Send OTP
curl -X POST http://localhost:8080/api/v1/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+201234567890"}'

# 2. Grab the code from logs
docker compose logs backend | grep "OTP for"

# 3. Verify (auto-registers if new number)
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+201234567890", "code": "482916"}'
```

Response comes back with a JWT:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000,
  "refreshToken": "cdf98f59-2053-4817-82b5-6086b07bd8df"
}
```

### Rate limiting

`/send-otp` is limited to **3 requests per phone number per minute**. Hit the limit and you get:

```json
{
  "message": "You have exceeded the rate limit. Please try again later."
}
```

Status: **429 Too Many Requests**

## API endpoints

| Method | Path | Auth | What it does |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | No | Login with email/phone/username + password |
| `POST` | `/api/v1/auth/register` | No | Register with email, phone, username, password |
| `PUT` | `/api/v1/auth/verify` | No | Verify email with code |
| `GET` | `/api/v1/auth/resend-code` | No | Resend verification email |
| `GET` | `/api/v1/auth/reset-password-request` | No | Request password reset email |
| `PUT` | `/api/v1/auth/reset-password` | No | Reset password with code |
| `POST` | `/api/v1/auth/refresh` | No | Exchange refresh token for new JWT |
| `POST` | `/api/v1/auth/logout` | Bearer | Blacklist token + revoke refresh tokens |
| `POST` | `/api/v1/auth/send-otp` | No | Send OTP to phone (rate-limited) |
| `POST` | `/api/v1/auth/verify-otp` | No | Verify OTP → login or auto-register |

## WhatsApp OTP (WireWeb)

Want real WhatsApp messages instead of console logs?

1. Go to [app.wireweb.co.in](https://app.wireweb.co.in)
2. Scan the QR with your phone (WhatsApp → Linked Devices)
3. Copy your **API key** and **session ID**
4. Set in `.env`:

```env
OTP_PROVIDER=wireweb
WIREWEB_API_KEY=wire_your_api_key_here
WIREWEB_SESSION_ID=ws_your_session_id_here
```

5. Rebuild and restart:

```bash
docker compose build backend && docker compose up -d backend
```

Now every OTP sends as a WhatsApp message.

## About

Built with Java 17 + Spring Boot 3.4, PostgreSQL 15, Redis 7, and Flyway. Runs in Docker.
