# Deploy The D'Archive (public link, free tiers)

Architecture: **Vercel** (Next.js frontend, the public link) -> proxies `/api`, `/oauth2`, `/login/oauth2` ->
**Render** (Spring Boot backend, Docker) -> **Neon** (PostgreSQL, also stores uploaded files).
The browser only ever sees the Vercel URL, so cookies, PDF previews and Google login all work without CORS tricks.

## 0. Prerequisites
- Neon database already created with `database/schema.sql` run (re-run it: it's idempotent and now adds memos,
  stored files and the summary column). Optionally `database/seed.sql`.
- A GitHub account. Push this project to a new repo:
  ```bash
  git init && git add . && git commit -m "D'Archive"
  git branch -M main
  git remote add origin https://github.com/<you>/darchive.git
  git push -u origin main
  ```
- An Anthropic API key (https://console.anthropic.com -> API keys) for the chatbot and summaries.
  Without it everything else works; AI endpoints answer "AI is not configured".

## 1. Backend on Render
1. https://render.com -> **New -> Blueprint** -> pick your repo (it reads `render.yaml`).
   (Or **New -> Web Service**, runtime Docker, root directory `backend`.)
2. Fill the prompted env vars: `NEON_JDBC_URL`, `NEON_DB_USER`, `NEON_DB_PASSWORD`, `GOOGLE_CLIENT_ID`,
   `GOOGLE_CLIENT_SECRET`, `ANTHROPIC_API_KEY`, `ADMIN_EMAILS`. Leave `FRONTEND_URL` empty for now.
3. Deploy. Note the URL, e.g. `https://darchive-backend.onrender.com`.

## 2. Frontend on Vercel
1. https://vercel.com -> **Add New -> Project** -> import the same repo (root directory = repo root).
2. Environment variable: `BACKEND_URL` = your Render URL (no trailing slash). It must be set **before** the build,
   because rewrites are baked in at build time.
3. Deploy. Your public link is the Vercel URL, e.g. `https://darchive.vercel.app`.

## 3. Connect them
1. Render -> backend service -> Environment -> set `FRONTEND_URL` = your Vercel URL (no trailing slash) -> save (it redeploys).
2. Google Cloud Console -> Credentials -> your OAuth client:
   - Authorized redirect URI: `https://<your-vercel-url>/login/oauth2/code/google`
     (keep `http://localhost:3000/login/oauth2/code/google` for local development)
   - Authorized JavaScript origin: `https://<your-vercel-url>`
3. OAuth consent screen -> **Publish app** (move from *Testing* to *In production*). While in Testing only listed
   test users can sign in and sessions expire quickly. Basic sign-in scopes (openid/email/profile) need no Google review.

## 4. Test
Open the Vercel URL -> Continue with Google -> forum loads -> upload a PDF -> Preview -> Summarize -> Ask AI.

## Things to know
- **Who can log in:** only `@bmsce.ac.in` Google accounts (by design). Anyone else with the link sees the login page.
  Change `ALLOWED_EMAIL_DOMAIN` on Render to allow another domain.
- **Cold starts:** Render's free service sleeps after ~15 min idle (first load ~30-60 s), and Neon also sleeps. Paid
  tiers or a free uptime pinger on `/api/auth/csrf` avoid this.
- **Upload size:** capped at 10 MB. Vercel may reject large request bodies passing through its proxy (documented
  limit for functions is 4.5 MB; external rewrites may behave differently). Test with an ~8 MB PDF. If it fails,
  use "Share a link" for big files or host the frontend somewhere without that limit.
- **AI cost:** limited to 10 calls/min and 150/day per user, and summaries are cached per resource.
  The default model is `claude-haiku-5-5` (cheap, fast); set `ANTHROPIC_MODEL` to change it.
- **Neon free tier** is 0.5 GB; uploaded files live in the database, so watch usage.
