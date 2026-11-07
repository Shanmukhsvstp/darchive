-- =====================================================================
-- The D'Archive  |  Sample data (mirrors the placeholder UI)
-- Re-runnable. Replace the placeholder subjects/URLs with real ones.
--   psql "$DATABASE_URL" -f database/seed.sql
-- =====================================================================

BEGIN;

INSERT INTO subjects (code, name, semester, display_order) VALUES
  ('SUBJ1', 'Subject 1', 3, 1),
  ('SUBJ2', 'Subject 2', 3, 2),
  ('SUBJ3', 'Subject 3', 3, 3)
ON CONFLICT (code) DO NOTHING;

-- Notes: Units 1-3 for each subject
INSERT INTO resources (subject_id, kind, title, description, unit_number, file_url, preview_url)
SELECT s.id, 'note', 'Unit ' || u, 'Some information about the resource', u,
       'https://example.com/notes/' || lower(s.code) || '-unit' || u || '.pdf',
       'https://placehold.co/300x200.png'
FROM subjects s CROSS JOIN generate_series(1, 3) AS u
WHERE s.code IN ('SUBJ1', 'SUBJ2', 'SUBJ3')
ON CONFLICT DO NOTHING;

-- Archive: older material (shown in the "Archive" section)
INSERT INTO resources (subject_id, kind, title, description, unit_number, file_url, preview_url, is_archived)
SELECT s.id, 'note', 'Unit ' || u, 'Some information about the resource', u,
       'https://example.com/archive/' || lower(s.code) || '-unit' || u || '.pdf',
       'https://placehold.co/300x200.png', TRUE
FROM subjects s CROSS JOIN generate_series(1, 3) AS u
WHERE s.code = 'SUBJ1'
ON CONFLICT DO NOTHING;

-- Sample PYQs and lab work so those links have data later
INSERT INTO resources (subject_id, kind, title, description, academic_year, file_url)
SELECT id, 'pyq', 'Semester End Exam 2024-25', 'Previous year question paper', '2024-25',
       'https://example.com/pyq/' || lower(code) || '-2024-25.pdf'
FROM subjects
ON CONFLICT DO NOTHING;

INSERT INTO resources (subject_id, kind, title, description, file_url)
SELECT id, 'lab', 'Lab Manual', 'Lab programs and viva questions',
       'https://example.com/lab/' || lower(code) || '-manual.pdf'
FROM subjects
ON CONFLICT DO NOTHING;

-- A welcome post from a system account
INSERT INTO users (google_sub, email, full_name, role)
VALUES ('system-seed', 'darchive@bmsce.ac.in', 'The D''Archive', 'admin')
ON CONFLICT (google_sub) DO NOTHING;

INSERT INTO posts (author_id, title, body)
SELECT u.id, 'Welcome to the Community Forum',
       'Ask doubts, share tips and keep it useful. Notes and PYQs live under Resources.'
FROM users u
WHERE u.google_sub = 'system-seed'
  AND NOT EXISTS (SELECT 1 FROM posts p WHERE p.author_id = u.id);

COMMIT;
