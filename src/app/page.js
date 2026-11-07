"use client";

import "./home.css";
import Link from "next/link";
import { useEffect, useState } from "react";
import Comments from "@/components/Comments/Comments";
import { useMe } from "@/lib/useMe";
import { API, apiFetch, csrfHeaders } from "@/lib/api";

export default function Home() {
  const me = useMe();
  const isAdmin = me?.role === "admin";
  const [posts, setPosts] = useState([]);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [error, setError] = useState("");
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    apiFetch("/api/posts")
      .then(setPosts)
      .catch((e) => setError(e.message));
  }, []);

  async function handlePost(e) {
    e.preventDefault();
    if (!body.trim()) return;
    setPosting(true);
    setError("");
    try {
      const created = await apiFetch("/api/posts", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(await csrfHeaders()) },
        body: JSON.stringify({ title, body }),
      });
      setPosts((prev) => [created, ...prev]);
      setTitle("");
      setBody("");
    } catch (err) {
      setError(err.message);
    } finally {
      setPosting(false);
    }
  }

  async function handleDelete(id) {
    try {
      await apiFetch(`/api/posts/${id}`, { method: "DELETE", headers: await csrfHeaders() });
      setPosts((prev) => prev.filter((p) => p.id !== id));
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleLogout() {
    const response = await fetch(`${API}/api/auth/logout`, {
      method: "POST",
      credentials: "include",
      headers: await csrfHeaders(),
    });
    if (!response.ok) throw new Error(`Logout failed: ${response.status}`);
    window.location.href = "/auth";
  }

  return (
    <div>
      <div className="flex-container">
        <main>
          <div>
            <h2 id="Com_for">Community Forum</h2>

            <br />

            <form onSubmit={handlePost}>
              <p>
                <label htmlFor="post">This better be an important thing</label>

                <br />

                <input
                  type="text"
                  name="title"
                  maxLength={150}
                  placeholder="Title (optional)"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />

                <br />

                <textarea
                  name="post"
                  id="post"
                  cols="30"
                  rows="10"
                  maxLength={5000}
                  placeholder="Write it here"
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                ></textarea>

                <br />

                <button type="submit" className="p_b" disabled={posting}>
                  {posting ? "Posting..." : "Post"}
                </button>
              </p>
              {error && <p role="alert">{error}</p>}
            </form>
          </div>

          {posts.map((post) => (
            <article key={post.id}>
              {post.title && <h3>{post.title}</h3>}
              <section>
                <p>{post.body}</p>
                <p>
                  <small>
                    {post.authorName} · {new Date(post.createdAt).toLocaleString()}
                  </small>
                </p>
                {(post.mine || isAdmin) && (
                  <button type="button" onClick={() => handleDelete(post.id)}>
                    Delete
                  </button>
                )}
                <Comments postId={post.id} initialCount={post.commentCount} isAdmin={isAdmin} />
              </section>
            </article>
          ))}
        </main>
        <aside>
          <h2 id="Res">Resources</h2>
          <nav>
            <ul>
              <li>
                <article>
                  <section></section>
                  <section>
                    <Link href="/notes">PDFs and Notes</Link>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section></section>
                  <section>
                    <Link href="/pyqs">PYQs</Link>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section></section>
                  <section>
                    <Link href="/labs">Lab Work</Link>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section></section>
                  <section>
                    <Link href="/upload">Upload Material</Link>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section></section>
                  <section>
                    <Link href="/memos">My Memos</Link>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section></section>
                  <section>
                    <button onClick={handleLogout}>Logout</button>
                  </section>
                </article>
              </li>
            </ul>
          </nav>
        </aside>
      </div>
    </div>
  );
}
