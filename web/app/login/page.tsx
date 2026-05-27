"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { supabase } from "@/lib/supabase";
import { UI } from "@/lib/strings";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const { error: authError } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (authError) {
      setError(
        authError.code === "invalid_credentials"
          ? UI.auth.errorInvalid
          : UI.auth.errorFallback
      );
      setLoading(false);
      return;
    }

    router.push("/");
    router.refresh();
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="brand">
          <div className="brand-mark">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
              <path d="M5 19c0-8 6-14 14-14 0 8-6 14-14 14Z" />
              <path d="M5 19l8-8" />
            </svg>
          </div>
          <div className="brand-name">
            <div className="brand-title">{UI.brandTitle}</div>
            <div className="brand-sub">{UI.brandSub}</div>
          </div>
        </div>

        <h1 className="login-title">{UI.auth.loginTitle}</h1>
        <p className="login-subtitle">{UI.auth.loginSubtitle}</p>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-field">
            <label htmlFor="email">{UI.auth.emailLabel}</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={UI.auth.emailPlaceholder}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-field">
            <label htmlFor="password">{UI.auth.passwordLabel}</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
          </div>

          {error && <p className="login-error">{error}</p>}

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? UI.auth.loadingBtn : UI.auth.submitBtn}
          </button>
        </form>
      </div>
    </div>
  );
}
