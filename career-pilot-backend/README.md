# Career Pilot Backend

## Quick start

```bash
cp .env.example .env
# edit .env — at minimum set DB_PASSWORD and JWT_SECRET
docker compose up --build
```

Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## WhatsApp OTP (WireWeb)

By default OTPs are logged to console (`OTP_PROVIDER=simulated`).

For real WhatsApp messages:

1. Go to [app.wireweb.co.in](https://app.wireweb.co.in)
2. create a new session 
3. Scan the QR with WhatsApp (Linked Devices)
4. Copy your API key and session ID
5. Add to `.env`:

```env
OTP_PROVIDER=wireweb
WIREWEB_API_KEY=wire_your_key_here
WIREWEB_SESSION_ID=ws_your_session_here
```

5. Rebuild:

```bash
docker compose build backend && docker compose up -d backend
```

## LLM Configuration

Provider is selected via Maven profile. Only one starter is on the classpath at a time; the unused config is safely ignored.

**OpenAI / Gemini (default):**
```bash
./mvnw spring-boot:run
```

```env
LLM_API_KEY=your-api-key
LLM_MODEL=gemini-3.1-flash-lite
OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
```

For OpenAI cloud or local OpenAI-compatible servers (LM Studio, LocalAI, vLLM):
```env
OPENAI_BASE_URL=https://api.openai.com
LLM_MODEL=gpt-3.5-turbo
```
or
```env
OPENAI_BASE_URL=http://localhost:1234/v1
LLM_API_KEY=not-needed
```

**Ollama (opt-in):**
```bash
./mvnw -P ollama spring-boot:run
```

```env
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3
```

## `.env` reference

| Variable | Required | Default | What it does |
---|---|---|---|---|
| `DB_PASSWORD` | ✅ | — | PostgreSQL password |
| `JWT_SECRET` | ✅ | — | 256-bit Base64 key (run `python3 -c "import secrets,base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"`) |
| `OTP_PROVIDER` | ❌ | `simulated` | `simulated` or `wireweb` |
| `WIREWEB_API_KEY` | ❌ | — | WireWeb API key |
| `WIREWEB_SESSION_ID` | ❌ | — | WireWeb session ID |
| `LLM_API_KEY` | ✅ | — | API key (required for OpenAI/Gemini) |
| `LLM_MODEL` | ❌ | `gemini-3.1-flash-lite` | Model name |
| `LLM_TEMPERATURE` | ❌ | `0.4` | Response creativity |
| `OPENAI_BASE_URL` | ❌ | `https://generativelanguage.googleapis.com/v1beta/openai` | OpenAI-compatible endpoint (Gemini by default) |
| `OLLAMA_BASE_URL` | ❌ | `http://localhost:11434` | Ollama endpoint |
| `OLLAMA_MODEL` | ❌ | `llama3` | Ollama model |
