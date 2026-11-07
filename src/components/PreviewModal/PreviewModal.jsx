"use client";

import "./PreviewModal.css";
import { useEffect, useState } from "react";
import { apiFetch, csrfHeaders } from "@/lib/api";
import { describeResource } from "@/lib/fileInfo";

export default function PreviewModal({ resource, onClose }) {
  const info = describeResource(resource);
  const [summary, setSummary] = useState(resource.summary || "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  async function summarize() {
    setBusy(true);
    setError("");
    try {
      const res = await apiFetch(`/api/resources/${resource.id}/summary`, {
        method: "POST",
        headers: await csrfHeaders(),
      });
      setSummary(res.summary);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="pv-overlay" onClick={onClose}>
      <div className="pv-card" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <div className="pv-head">
          <h3>
            {resource.subjectName} - {resource.title}
          </h3>
          <a href={resource.fileUrl} target="_blank" rel="noopener noreferrer">
            Open in new tab
          </a>
          <button type="button" onClick={onClose}>
            Close
          </button>
        </div>

        <div className="pv-body">
          {info.type === "image" ? (
            <img src={info.embed} alt={resource.title} />
          ) : info.embed ? (
            <iframe src={info.embed} title={`Preview of ${resource.title}`} />
          ) : (
            <div className="pv-none">
              <p>This {info.label} resource can&apos;t be previewed inside the page.</p>
              <p>
                <a href={resource.fileUrl} target="_blank" rel="noopener noreferrer">
                  Open it in a new tab
                </a>
              </p>
            </div>
          )}
        </div>

        <div className="pv-ai">
          {info.canSummarize ? (
            <button type="button" onClick={summarize} disabled={busy}>
              {busy ? "Reading the document..." : summary ? "Summary (saved)" : "Summarize with AI"}
            </button>
          ) : (
            <small>AI summaries work for PDF / TXT files uploaded to D&apos;Archive.</small>
          )}
          {error && <p className="pv-err">{error}</p>}
          {summary && <pre>{summary}</pre>}
        </div>
      </div>
    </div>
  );
}
