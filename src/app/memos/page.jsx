"use client";

import "./page.css";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { apiFetch, csrfHeaders } from "@/lib/api";

function MemoCard({ memo, onChange, onDelete }) {
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(memo.title || "");
  const [body, setBody] = useState(memo.body);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");

  async function save() {
    setBusy("save");
    setError("");
    try {
      const updated = await apiFetch(`/api/memos/${memo.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({ title, body }),
      });
      onChange(updated);
      setEditing(false);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy("");
    }
  }

  async function summarize() {
    setBusy("sum");
    setError("");
    try {
      const res = await apiFetch(`/api/memos/${memo.id}/summarize`, {
        method: "POST",
        headers: await csrfHeaders(),
      });
      onChange({ ...memo, summary: res.summary });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy("");
    }
  }

  async function remove() {
    if (!confirm("Delete this memo?")) return;
    try {
      await apiFetch(`/api/memos/${memo.id}`, { method: "DELETE", headers: await csrfHeaders() });
      onDelete(memo.id);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <article className="memo-box">
      {editing ? (
        <>
          <input type="text" value={title} maxLength={120} placeholder="Title" onChange={(e) => setTitle(e.target.value)} />
          <textarea rows={8} value={body} maxLength={10000} onChange={(e) => setBody(e.target.value)} />
          <button type="button" onClick={save} disabled={busy === "save" || !body.trim()}>
            Save
          </button>
          <button type="button" onClick={() => { setEditing(false); setTitle(memo.title || ""); setBody(memo.body); }}>
            Cancel
          </button>
        </>
      ) : (
        <>
          <h3>{memo.title || "Untitled memo"}</h3>
          <div className="memo-meta">{new Date(memo.updatedAt).toLocaleString()}</div>
          <p className="memo-body">{memo.body}</p>
          {memo.summary && (
            <div className="memo-summary">
              <strong>AI summary</strong>
              {"\n"}
              {memo.summary}
            </div>
          )}
          <button type="button" onClick={() => setEditing(true)}>Edit</button>
          <button type="button" onClick={summarize} disabled={busy === "sum"}>
            {busy === "sum" ? "Summarizing..." : memo.summary ? "Re-summarize" : "Summarize with AI"}
          </button>
          <button type="button" onClick={remove}>Delete</button>
        </>
      )}
      {error && <p className="memo-err">{error}</p>}
    </article>
  );
}

export default function MemosPage() {
  const [memos, setMemos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    apiFetch("/api/memos")
      .then(setMemos)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  async function add(e) {
    e.preventDefault();
    if (!body.trim()) return;
    setError("");
    try {
      const m = await apiFetch("/api/memos", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({ title, body }),
      });
      setMemos((prev) => [m, ...prev]);
      setTitle("");
      setBody("");
    } catch (err) {
      setError(err.message);
    }
  }

  const shown = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return memos;
    return memos.filter((m) => `${m.title || ""} ${m.body}`.toLowerCase().includes(q));
  }, [memos, query]);

  return (
    <div className="memos-root">
      <div className="memos-wrap">
        <section className="memo-box">
          <h2>My Memos</h2>
          <p className="memo-meta">Private to you. Use &quot;Summarize with AI&quot; to turn long memos into key points.</p>
          <form onSubmit={add}>
            <input type="text" value={title} maxLength={120} placeholder="Title (optional)" onChange={(e) => setTitle(e.target.value)} />
            <textarea rows={5} value={body} maxLength={10000} placeholder="Write a memo..." onChange={(e) => setBody(e.target.value)} />
            <button type="submit" disabled={!body.trim()}>Add memo</button>
            <Link className="btn" href="/" style={{ textDecoration: "none" }}>Back to forum</Link>
          </form>
          {error && <p className="memo-err">{error}</p>}
        </section>

        <input className="memo-box" type="text" value={query} placeholder="Search memos..." onChange={(e) => setQuery(e.target.value)} />

        {loading && <p>Loading...</p>}
        {!loading && shown.length === 0 && <p>{memos.length ? "No memos match your search." : "No memos yet."}</p>}

        <div className="memo-grid">
          {shown.map((m) => (
            <MemoCard
              key={m.id}
              memo={m}
              onChange={(u) => setMemos((prev) => prev.map((x) => (x.id === u.id ? u : x)))}
              onDelete={(id) => setMemos((prev) => prev.filter((x) => x.id !== id))}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
