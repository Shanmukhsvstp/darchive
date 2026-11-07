"use client";
import './page.css';
import { useEffect, useState } from "react";
import { API } from "@/lib/api";

export default function Page() {
  const [rejected, setRejected] = useState(false);
  useEffect(() => {
    setRejected(new URLSearchParams(window.location.search).get("error") === "not_allowed");
  }, []);

  return (
    <div className="login-page">
      <main className="login-card">
        <div className="login-icon">✦</div>

        <h2>Sign in to continue</h2>

        <p className="description">
          Access your student portal using your college Google account.
        </p>

        <button type="button" className="google-btn"
          onClick={() => {
            window.location.href = `${API}/oauth2/authorization/google`;
          }}>
          <svg
            className="google-logo"
            viewBox="0 0 48 48"
            aria-hidden="true"
          >
            <path fill="#EA4335" d="M24 9.5c3.5 0 6.7 1.2 9.2 3.6l6.8-6.8C35.9 2.4 30.5 0 24 0 14.6 0 6.5 5.4 2.6 13.2l8 6.2C12.4 13.7 17.7 9.5 24 9.5Z" transform="translate(0 4)" />
            <path fill="#4285F4" d="M47 24.5c0-1.6-.2-3.1-.4-4.5H24v9h12.9c-.6 3-2.3 5.5-4.8 7.2l7.7 6C44.4 38 47 31.9 47 24.5Z" />
            <path fill="#FBBC05" d="M10.5 28.6a14.4 14.4 0 0 1 0-9.2l-8-6.2a23.9 23.9 0 0 0 0 21.6l8-6.2Z" transform="translate(0 4)" />
            <path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.7-6c-2.2 1.5-4.9 2.3-8.2 2.3-6.3 0-11.6-4.2-13.5-9.9l-8 6.2C6.5 42.6 14.6 48 24 48Z" transform="translate(0 -4)" />
          </svg>

          <span>Continue with Google</span>
        </button>

        {rejected && (
          <p role="alert" className="description">
            That account is not allowed. Please use your @bmsce.ac.in Google account.
          </p>
        )}

        <div className="divider">
          <span>SECURE STUDENT ACCESS</span>
        </div>

        <p className="college-note">
          Sign in with your <strong>@bmsce.ac.in</strong> account
        </p>
      </main>

      <footer className="login-footer">
        Student Portal <span>✦</span> Secure Access
      </footer>
    </div>
  );
}