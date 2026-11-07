"use client";

import "./FloatingTools.css";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { apiFetch, csrfHeaders } from "@/lib/api";

const GREETING = {
  role: "assistant",
  content:
    "Hi! I'm your Study Buddy. Ask me to explain a topic, solve a problem, or plan your revision.",
};

function Chat({ onClose }) {
  const [messages, setMessages] = useState([GREETING]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, busy]);

  async function send(e) {
    e?.preventDefault();
    const text = input.trim();
    if (!text || busy) return;
    const next = [...messages, { role: "user", content: text }];
    setMessages(next);
    setInput("");
    setBusy(true);
    setError("");
    try {
      const res = await apiFetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({ messages: next.filter((m) => m !== GREETING) }),
      });
      setMessages([...next, { role: "assistant", content: res.reply }]);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="ft-panel" role="dialog" aria-label="AI study assistant">
      <header>
        <span>Study Buddy (AI)</span>
        <button type="button" onClick={onClose} aria-label="Close chat">
          x
        </button>
      </header>
      <div className="ft-msgs">
        {messages.map((m, i) => (
          <div key={i} className={`ft-msg ${m.role}`}>
            {m.content}
          </div>
        ))}
        {busy && <div className="ft-msg assistant">Thinking...</div>}
        {error && <div className="ft-msg error">{error}</div>}
        <div ref={endRef} />
      </div>
      <form className="ft-form" onSubmit={send}>
        <textarea
          rows={2}
          value={input}
          maxLength={4000}
          placeholder="Ask anything about your subjects..."
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) send(e);
          }}
        />
        <button className="go" type="submit" disabled={busy || !input.trim()}>
          Send
        </button>
      </form>
    </div>
  );
}

function QuickMemo({ onClose }) {
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [status, setStatus] = useState({ type: "", text: "" });
  const [busy, setBusy] = useState(false);

  async function save(e) {
    e.preventDefault();
    if (!body.trim()) return;
    setBusy(true);
    setStatus({ type: "", text: "" });
    try {
      await apiFetch("/api/memos", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({ title, body }),
      });
      setTitle("");
      setBody("");
      setStatus({ type: "ok", text: "Saved to your memos" });
    } catch (err) {
      setStatus({ type: "err", text: err.message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="ft-panel" role="dialog" aria-label="Quick memo">
      <header>
        <span>Quick memo</span>
        <button type="button" onClick={onClose} aria-label="Close memo">
          x
        </button>
      </header>
      <form className="ft-memo" onSubmit={save}>
        <input type="text" value={title} maxLength={120} placeholder="Title (optional)"
          onChange={(e) => setTitle(e.target.value)} />
        <textarea rows={6} value={body} maxLength={10000} placeholder="Jot it down..."
          onChange={(e) => setBody(e.target.value)} autoFocus />
        <div>
          <button className="go" type="submit" disabled={busy || !body.trim()}>
            {busy ? "Saving..." : "Save memo"}
          </button>{" "}
          <Link href="/memos" onClick={onClose}>
            All memos
          </Link>
        </div>
        {status.text && <span className={status.type === "ok" ? "ft-ok" : "ft-err"}>{status.text}</span>}
      </form>
    </div>
  );
}

/** Floating AI chat + quick memo, available on every page except the login page. */
export default function FloatingTools() {
  const pathname = usePathname();
  const [open, setOpen] = useState(null); // "chat" | "memo" | null
  if (pathname === "/auth") return null;

  return (
    <div className="ft-dock">
      {/* Both panels stay mounted (hidden) so the chat history survives opening/closing */}
      <div style={{ display: open === "chat" ? "block" : "none" }}>
        <Chat onClose={() => setOpen(null)} />
      </div>
      {open === "memo" && <QuickMemo onClose={() => setOpen(null)} />}
      <div className="ft-fab-row">
        <button type="button" className={`ft-fab ${open === "memo" ? "on" : ""}`}
          onClick={() => setOpen(open === "memo" ? null : "memo")}>
          Memo
        </button>
        <button type="button" className={`ft-fab ${open === "chat" ? "on" : ""}`}
          onClick={() => setOpen(open === "chat" ? null : "chat")}>
          Ask AI
        </button>
      </div>
    </div>
  );
}
