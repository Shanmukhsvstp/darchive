"use client";

import "@/app/notes/page.css";
import { useCallback, useEffect, useState } from "react";
import { apiFetch, csrfHeaders } from "@/lib/api";
import { describeResource } from "@/lib/fileInfo";
import PreviewModal from "@/components/PreviewModal/PreviewModal";
import { useMe } from "@/lib/useMe";
import Link from "next/link";


function Thumb({ r }) {
  const info = describeResource(r);
  const [failed, setFailed] = useState(false);
  if (info.thumb && !failed) {
    return <img src={info.thumb} alt={`${r.subjectName} ${r.title}`} onError={() => setFailed(true)} />;
  }
  return (
    <div className="thumb-icon" aria-label={`${info.label} file`}>
      <span>{info.label}</span>
    </div>
  );
}

function ResourceCard({ r, canDelete, onDelete, onPreview }) {
  return (
    <article className="RCard">
      <h3>{r.title}</h3>
      <section>
        <button type="button" className="thumb-btn" onClick={() => onPreview(r)} title="Preview">
          <Thumb r={r} />
        </button>
      </section>
      <section>
        <p>{r.description || "No description yet"}</p>
        {r.academicYear && <p>Year: {r.academicYear}</p>}
        <p>
          <small>Shared by {r.uploaderName || "unknown"}</small>
        </p>
        <button type="button" onClick={() => onPreview(r)}>
          Preview
        </button>
        {" · "}
        <a href={r.fileUrl} target="_blank" rel="noopener noreferrer">
          Open
        </a>
        {canDelete && (
          <>
            {" · "}
            <button type="button" onClick={() => onDelete(r)}>
              Delete
            </button>
          </>
        )}
      </section>
    </article>
  );
}

/** Lists resources of one kind ("note" | "pyq" | "lab") grouped by subject, plus an Archive section. */
export default function ResourceBrowser({ kind, heading, extra }) {
  const me = useMe();
  const [subjects, setSubjects] = useState([]);
  const [resources, setResources] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [preview, setPreview] = useState(null);

  const load = useCallback(async () => {
    try {
      const [s, r] = await Promise.all([
        apiFetch("/api/subjects"),
        apiFetch(`/api/resources?kind=${kind}`),
      ]);
      setSubjects(s);
      setResources(r);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [kind]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleDelete(r) {
    if (!confirm(`Delete "${r.subjectName} - ${r.title}"?`)) return;
    try {
      await apiFetch(`/api/resources/${r.id}`, { method: "DELETE", headers: await csrfHeaders() });
      setResources((prev) => prev.filter((x) => x.id !== r.id));
    } catch (e) {
      setError(e.message);
    }
  }

  const canDelete = (r) => r.mine || me?.role === "admin";
  const current = resources.filter((r) => !r.archived);
  const archived = resources.filter((r) => r.archived);
  const subjectsWithItems = subjects.filter((s) => current.some((r) => r.subjectId === s.id));

  return (
    <div className="notes-root">
      <div className="flex-container">
        <main>
          <h1>{heading}</h1>
          {extra}
          {error && <p role="alert">{error}</p>}
          {loading && <p>Loading...</p>}
          {!loading && resources.length === 0 && !error && (
            <p>
              Nothing here yet. <Link href="/upload">Be the first to upload.</Link>
            </p>
          )}

          {subjectsWithItems.map((subject) => (
            <article key={subject.id} id={`subj${subject.id}`} className="subs">
              <h2>{subject.name}</h2>
              <br />
              <section className="flex-container Notesere">
                {current
                  .filter((r) => r.subjectId === subject.id)
                  .map((r) => (
                    <ResourceCard key={r.id} r={r} canDelete={canDelete(r)} onDelete={handleDelete} onPreview={setPreview} />
                  ))}
              </section>
            </article>
          ))}

          {archived.length > 0 && (
            <article id="arch" className="subs">
              <h2>Archive</h2>
              <br />
              <section className="flex-container Notesere">
                {archived.map((r) => (
                  <ResourceCard key={r.id} r={r} canDelete={canDelete(r)} onDelete={handleDelete} onPreview={setPreview} />
                ))}
              </section>
            </article>
          )}
        </main>

        <aside>
          <nav>
            <ul>
              <li>
                <Link href="/">Forum</Link>
              </li>
              <li>
                <Link href="/upload">Upload</Link>
              </li>
              {subjectsWithItems.map((s) => (
                <li key={s.id}>
                  <a href={`#subj${s.id}`}>{s.name}</a>
                </li>
              ))}
              {archived.length > 0 && (
                <li>
                  <a href="#arch">Archive</a>
                </li>
              )}
            </ul>
          </nav>
        </aside>
      </div>
      {preview && <PreviewModal resource={preview} onClose={() => setPreview(null)} />}
    </div>
  );
}
