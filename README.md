# The D'Archive

Student portal for BMSCE: Google sign-in (`@bmsce.ac.in` only), Community Forum with comments, Notes / PYQs / Lab Work
with in-page **previews**, uploads and Archive, an **AI study chatbot**, **AI summaries** for PDFs, and private
**quick memos** with AI summaries. PYQ page also links to NoteGo and ExNote.

| Part | Tech | Port |
|---|---|---|
| Frontend (`/`) | Next.js (proxies `/api`, `/oauth2` to the backend) | 3000 |
| Backend (`/backend`) | Spring Boot 4, JDBC, Spring Security (Google OAuth2), Anthropic API | 8080 |
| Database | Neon PostgreSQL (`/database`) - also stores uploaded files | - |

**Deploying publicly?** See `DEPLOY.md`.

## Run locally

### 1. Install once
Node.js 20+, JDK 21, Gradle (or open `backend/` in IntelliJ IDEA).

### 2. Database (Neon)
Neon console -> SQL Editor -> run `database/schema.sql`, then (optional) `database/seed.sql`. Re-running the schema is safe.
Copy host / db / user / password from **Connect** (use the `-pooler` host).

### 3. Google login
Google Cloud Console -> OAuth consent screen (External, add yourself as a test user) -> Credentials -> OAuth client (Web):
- Authorized redirect URI: **`http://localhost:3000/login/oauth2/code/google`**
  (note: port **3000** - login goes through the frontend proxy now, not 8080)

### 4. AI key (optional but recommended)
https://console.anthropic.com -> API keys -> create a key.

### 5. Configure the backend
```bash
cd backend
cp .env.example .env     # Windows: copy .env.example .env
```
Fill `NEON_JDBC_URL`, `NEON_DB_USER`, `NEON_DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`,
`ADMIN_EMAILS` (your college email -> becomes admin), `ANTHROPIC_API_KEY`.

### 6. Start
```bash
# terminal 1
cd backend && gradle bootRun
# terminal 2 (project root)
npm install && npm run dev
```
Open http://localhost:3000 and sign in.

## Features
- **Forum**: posts, comments, delete own (admins any).
- **Notes / PYQs / Lab Work**: grouped by subject + Archive. **Preview** opens PDFs, images and Google Drive links
  in a pop-up. **Summarize with AI** works on PDFs/TXT uploaded here (cached after first run).
- **PYQs** page links out to https://notego.in/ and https://exnote.vercel.app/.
- **Upload** (`/upload`): file (<=10 MB; pdf, ppt, doc, png, jpg, zip, txt) or link. Admins can add subjects.
- **Ask AI** (floating button): study chatbot, plain-text answers, keeps the conversation while you browse.
- **Memo** (floating button) + **My Memos** (`/memos`): quick private notes, search, edit, AI summary.

## API
| Method | Path |
|---|---|
| GET | `/api/auth/csrf` |
| POST | `/api/auth/logout` |
| GET | `/api/me` |
| GET/POST/DELETE | `/api/posts`, `/api/posts/{id}` |
| GET/POST | `/api/posts/{id}/comments`; DELETE `/api/comments/{id}` |
| GET/POST | `/api/subjects` (POST admin) |
| GET/POST/DELETE | `/api/resources?kind=note\|pyq\|lab`, `/api/resources/{id}` |
| POST | `/api/resources/{id}/summary` |
| GET | `/api/files/{name}` |
| POST | `/api/chat` |
| GET/POST/PUT/DELETE | `/api/memos`, `/api/memos/{id}`; POST `/api/memos/{id}/summarize` |

## Troubleshooting
- `Could not resolve placeholder 'NEON_JDBC_URL'` -> `backend/.env` missing, or Gradle run outside `backend/`.
- `redirect_uri_mismatch` -> Google redirect URI must be exactly `http://localhost:3000/login/oauth2/code/google`.
- "AI is not configured" -> set `ANTHROPIC_API_KEY` in `backend/.env` and restart the backend.
- PDF preview blank -> some browsers (mostly phones) can't show PDFs in a pop-up; use "Open in new tab".
- Upload 409 -> same subject already has a resource with that title.
- First request slow -> Neon is waking from sleep.
