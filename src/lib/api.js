// Empty = same origin. next.config.mjs proxies /api, /oauth2 and /login/oauth2 to the Spring Boot backend.
export const API = "";

/** Fetch JSON from the backend with the session cookie; sends the user to /auth on 401. */
export async function apiFetch(path, options = {}) {
  const res = await fetch(`${API}${path}`, { credentials: "include", ...options });
  if (res.status === 401) {
    window.location.href = "/auth";
    throw new Error("Not signed in");
  }
  if (!res.ok) {
    let reason = `Request failed: ${res.status}`;
    try {
      const body = await res.json();
      if (body.message) reason = body.message;
    } catch {}
    throw new Error(reason);
  }
  return res.status === 204 ? null : res.json();
}

/** Headers carrying the CSRF token required for POST/DELETE. */
export async function csrfHeaders() {
  const res = await fetch(`${API}/api/auth/csrf`, { credentials: "include" });
  if (!res.ok) throw new Error("Failed to obtain CSRF token");
  const csrf = await res.json();
  return { [csrf.headerName]: csrf.token };
}
