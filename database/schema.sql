-- =====================================================================
-- The D'Archive  |  PostgreSQL schema (Neon compatible)
-- Safe to re-run: every statement is idempotent.
--   psql "$DATABASE_URL" -f database/schema.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Shared trigger: keep updated_at fresh
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- users  (created automatically on first Google sign-in)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  google_sub    TEXT        NOT NULL UNIQUE,           -- Google's stable user id ("sub" claim)
  email         TEXT        NOT NULL UNIQUE,
  full_name     TEXT,
  picture_url   TEXT,
  role          TEXT        NOT NULL DEFAULT 'student',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT users_role_chk  CHECK (role IN ('student', 'contributor', 'admin')),
  CONSTRAINT users_email_chk CHECK (email = lower(email) AND email LIKE '%@bmsce.ac.in')
);

-- ---------------------------------------------------------------------
-- subjects
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subjects (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code          TEXT        NOT NULL UNIQUE,           -- e.g. 'CS3PCDSA'
  name          TEXT        NOT NULL,
  semester      SMALLINT,
  display_order INTEGER     NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT subjects_semester_chk CHECK (semester IS NULL OR semester BETWEEN 1 AND 8)
);

-- ---------------------------------------------------------------------
-- resources  (notes / PYQs / lab work; "Archive" = is_archived)
-- file_url is a link (Google Drive, S3, R2...). Files themselves are
-- NOT stored in Postgres.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resources (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  subject_id    BIGINT      NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  kind          TEXT        NOT NULL,
  title         TEXT        NOT NULL,                  -- e.g. 'Unit 1'
  description   TEXT,
  unit_number   SMALLINT,
  academic_year TEXT,                                  -- e.g. '2024-25' (useful for PYQs)
  file_url      TEXT        NOT NULL,
  preview_url   TEXT,
  is_archived   BOOLEAN     NOT NULL DEFAULT FALSE,
  uploaded_by   BIGINT      REFERENCES users(id) ON DELETE SET NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT resources_kind_chk CHECK (kind IN ('note', 'pyq', 'lab')),
  CONSTRAINT resources_unique   UNIQUE (subject_id, kind, title, is_archived)
);

CREATE INDEX IF NOT EXISTS idx_resources_listing
  ON resources (kind, is_archived, subject_id, unit_number);

DROP TRIGGER IF EXISTS trg_resources_updated_at ON resources;
CREATE TRIGGER trg_resources_updated_at
  BEFORE UPDATE ON resources
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- posts  (Community Forum)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  author_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       TEXT,
  body        TEXT        NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT posts_body_chk  CHECK (char_length(btrim(body)) BETWEEN 1 AND 5000),
  CONSTRAINT posts_title_chk CHECK (title IS NULL OR char_length(title) <= 150)
);

CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_author     ON posts (author_id);

DROP TRIGGER IF EXISTS trg_posts_updated_at ON posts;
CREATE TRIGGER trg_posts_updated_at
  BEFORE UPDATE ON posts
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- comments  (replies under forum posts)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  post_id     BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  author_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body        TEXT        NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT comments_body_chk CHECK (char_length(btrim(body)) BETWEEN 1 AND 2000)
);

CREATE INDEX IF NOT EXISTS idx_comments_post ON comments (post_id, created_at);

-- ---------------------------------------------------------------------
-- stored_files  (uploaded PDFs etc. live in Postgres so redeploys never lose them)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stored_files (
  name         TEXT PRIMARY KEY,                       -- '<uuid>.<ext>'
  content_type TEXT        NOT NULL,
  size_bytes   INTEGER     NOT NULL,
  data         BYTEA       NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- AI summary cache on resources (safe on existing databases)
-- ---------------------------------------------------------------------
ALTER TABLE resources ADD COLUMN IF NOT EXISTS summary TEXT;

-- ---------------------------------------------------------------------
-- memos  (private quick notes, optional AI summary)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS memos (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       TEXT,
  body        TEXT        NOT NULL,
  summary     TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT memos_body_chk  CHECK (char_length(btrim(body)) BETWEEN 1 AND 10000),
  CONSTRAINT memos_title_chk CHECK (title IS NULL OR char_length(title) <= 120)
);

CREATE INDEX IF NOT EXISTS idx_memos_user ON memos (user_id, updated_at DESC);

DROP TRIGGER IF EXISTS trg_memos_updated_at ON memos;
CREATE TRIGGER trg_memos_updated_at
  BEFORE UPDATE ON memos
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
