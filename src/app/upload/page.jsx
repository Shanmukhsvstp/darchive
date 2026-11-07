"use client";

import "./page.css";
import Link from "next/link";
import { useEffect, useState } from "react";
import { apiFetch, csrfHeaders, API } from "@/lib/api";
import { useMe } from "@/lib/useMe";

const KIND_PAGE = { note: "/notes", pyq: "/pyqs", lab: "/labs" };

export default function UploadPage() {
  const me = useMe();
  const [subjects, setSubjects] = useState([]);
  const [msg, setMsg] = useState({ type: "", text: "" });
  const [busy, setBusy] = useState(false);

  const [kind, setKind] = useState("note");
  const [source, setSource] = useState("file"); // "file" | "link"
  const [subjectId, setSubjectId] = useState("");

  const [subMsg, setSubMsg] = useState({ type: "", text: "" });

  const loadSubjects = () =>
    apiFetch("/api/subjects")
      .then((s) => {
        setSubjects(s);
        setSubjectId((cur) => cur || (s[0] ? String(s[0].id) : ""));
      })
      .catch((e) => setMsg({ type: "error", text: e.message }));

  useEffect(() => {
    loadSubjects();
  }, []);

  async function submitResource(e) {
    e.preventDefault();
    const form = e.currentTarget;
    const data = new FormData(form);
    data.set("subjectId", subjectId);
    data.set("kind", kind);
    data.set("archived", data.get("archived") ? "true" : "false");
    if (source === "file") data.delete("fileUrl");
    else data.delete("file");
    if (kind !== "note") data.delete("unitNumber");
    if (kind !== "pyq") data.delete("academicYear");

    setBusy(true);
    setMsg({ type: "", text: "" });
    try {
      // Do not set Content-Type: the browser adds the multipart boundary itself.
      const res = await fetch(`${API}/api/resources`, {
        method: "POST",
        credentials: "include",
        headers: await csrfHeaders(),
        body: data,
      });
      if (res.status === 401) {
        window.location.href = "/auth";
        return;
      }
      if (!res.ok) {
        let reason = `Upload failed (${res.status})`;
        try {
          const j = await res.json();
          if (j.message) reason = j.message;
        } catch {}
        throw new Error(reason);
      }
      form.reset();
      setMsg({ type: "ok", text: "Uploaded! It now shows on the " + KIND_PAGE[kind].slice(1) + " page." });
    } catch (err) {
      setMsg({ type: "error", text: err.message });
    } finally {
      setBusy(false);
    }
  }

  async function addSubject(e) {
    e.preventDefault();
    const form = e.currentTarget;
    const f = new FormData(form);
    try {
      await apiFetch("/api/subjects", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({
          code: f.get("code"),
          name: f.get("name"),
          semester: f.get("semester") ? Number(f.get("semester")) : null,
        }),
      });
      form.reset();
      setSubMsg({ type: "ok", text: "Subject added." });
      loadSubjects();
    } catch (err) {
      setSubMsg({ type: "error", text: err.message });
    }
  }

  return (
    <div className="upload-root">
      <div className="upload-card">
        <h2>Upload study material</h2>
        <form onSubmit={submitResource}>
          <label htmlFor="kind">Type</label>
          <select id="kind" value={kind} onChange={(e) => setKind(e.target.value)}>
            <option value="note">Notes / PDFs</option>
            <option value="pyq">Previous year question paper</option>
            <option value="lab">Lab work</option>
          </select>

          <label htmlFor="subject">Subject</label>
          <select id="subject" value={subjectId} onChange={(e) => setSubjectId(e.target.value)} required>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.code})
              </option>
            ))}
          </select>

          <label htmlFor="title">Title</label>
          <input id="title" name="title" type="text" maxLength={120} required
            placeholder={kind === "note" ? "Unit 4" : kind === "pyq" ? "SEE 2024-25" : "Lab Manual"} />

          <label htmlFor="description">Description (optional)</label>
          <textarea id="description" name="description" rows={3} maxLength={500} />

          {kind === "note" && (
            <>
              <label htmlFor="unitNumber">Unit number (optional)</label>
              <input id="unitNumber" name="unitNumber" type="number" min={1} max={20} />
            </>
          )}
          {kind === "pyq" && (
            <>
              <label htmlFor="academicYear">Academic year</label>
              <input id="academicYear" name="academicYear" type="text" placeholder="2024-25" maxLength={20} />
            </>
          )}

          <div className="row">
            <label>
              <input type="radio" checked={source === "file"} onChange={() => setSource("file")} /> Upload a file
            </label>
            <label>
              <input type="radio" checked={source === "link"} onChange={() => setSource("link")} /> Share a link
            </label>
          </div>

          {source === "file" ? (
            <>
              <label htmlFor="file">File (pdf, ppt, doc, png, jpg, zip, txt - max 10 MB)</label>
              <input id="file" name="file" type="file" required
                accept=".pdf,.ppt,.pptx,.doc,.docx,.png,.jpg,.jpeg,.zip,.txt" />
            </>
          ) : (
            <>
              <label htmlFor="fileUrl">Link (Google Drive, etc.)</label>
              <input id="fileUrl" name="fileUrl" type="url" required placeholder="https://drive.google.com/..." />
            </>
          )}

          <div className="row">
            <input id="archived" name="archived" type="checkbox" />
            <label htmlFor="archived">Put in Archive (old syllabus / outdated)</label>
          </div>

          <button type="submit" disabled={busy || !subjectId}>
            {busy ? "Uploading..." : "Upload"}
          </button>
          {msg.text && <p className={msg.type === "ok" ? "upload-msg-ok" : "upload-msg-error"}>{msg.text}</p>}
        </form>
        <p style={{ marginTop: "1rem" }}>
          <Link href="/">Back to forum</Link>
        </p>
      </div>

      {me?.role === "admin" && (
        <div className="upload-card">
          <h2>Admin: add a subject</h2>
          <form onSubmit={addSubject}>
            <label htmlFor="code">Subject code</label>
            <input id="code" name="code" type="text" maxLength={20} required placeholder="CS3PCDSA" />
            <label htmlFor="name">Name</label>
            <input id="name" name="name" type="text" maxLength={120} required placeholder="Data Structures" />
            <label htmlFor="semester">Semester (1-8)</label>
            <input id="semester" name="semester" type="number" min={1} max={8} />
            <button type="submit">Add subject</button>
            {subMsg.text && (
              <p className={subMsg.type === "ok" ? "upload-msg-ok" : "upload-msg-error"}>{subMsg.text}</p>
            )}
          </form>
        </div>
      )}
    </div>
  );
}
