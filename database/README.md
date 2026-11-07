# Database (Neon / PostgreSQL)

Run `schema.sql` in Neon's SQL Editor (safe to re-run; it also upgrades older databases), then optionally `seed.sql`.

Tables: `users`, `subjects`, `resources` (+ cached AI `summary`), `posts`, `comments`, `stored_files` (uploaded
files as bytea), `memos` (private quick notes + AI summary).

Make someone admin manually: `UPDATE users SET role = 'admin' WHERE email = 'you@bmsce.ac.in';`
(or set `ADMIN_EMAILS` in the backend env).
