"use client";

import { useState } from "react";
import { apiFetch, csrfHeaders } from "@/lib/api";

export default function Comments({ postId, initialCount, isAdmin }) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [count, setCount] = useState(initialCount);
  const [text, setText] = useState("");
  const [error, setError] = useState("");

  async function toggle() {
    const next = !open;
    setOpen(next);
    if (next) {
      try {
        const list = await apiFetch(`/api/posts/${postId}/comments`);
        setItems(list);
        setCount(list.length);
      } catch (e) {
        setError(e.message);
      }
    }
  }

  async function add(e) {
    e.preventDefault();
    if (!text.trim()) return;
    try {
      const c = await apiFetch(`/api/posts/${postId}/comments`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({ body: text }),
      });
      setItems((prev) => [...prev, c]);
      setCount((n) => n + 1);
      setText("");
    } catch (err) {
      setError(err.message);
    }
  }

  async function remove(id) {
    try {
      await apiFetch(`/api/comments/${id}`, { method: "DELETE", headers: await csrfHeaders() });
      setItems((prev) => prev.filter((c) => c.id !== id));
      setCount((n) => n - 1);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <button type="button" onClick={toggle}>
        {open ? "Hide comments" : `Comments (${count})`}
      </button>
      {open && (
        <div>
          {items.map((c) => (
            <p key={c.id}>
              <strong>{c.authorName}</strong>: {c.body}{" "}
              <small>{new Date(c.createdAt).toLocaleString()}</small>
              {(c.mine || isAdmin) && (
                <>
                  {" "}
                  <button type="button" onClick={() => remove(c.id)}>
                    x
                  </button>
                </>
              )}
            </p>
          ))}
          <form onSubmit={add}>
            <input
              type="text"
              value={text}
              maxLength={2000}
              placeholder="Write a comment"
              onChange={(e) => setText(e.target.value)}
            />{" "}
            <button type="submit">Reply</button>
          </form>
          {error && <p role="alert">{error}</p>}
        </div>
      )}
    </div>
  );
}
