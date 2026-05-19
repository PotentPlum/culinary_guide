"use client";

import { useRef, useState } from "react";
import type { ChallengeResponse } from "@/lib/types";
import { UI } from "@/lib/strings";

type Phase = "idle" | "loading" | "error" | "done";

function difficultyLabel(d: ChallengeResponse["difficulty"]): string {
  if (d === "Beginner") return UI.challenge.difficultyBeginner;
  if (d === "Intermediate") return UI.challenge.difficultyIntermediate;
  return UI.challenge.difficultyAdvanced;
}

export default function ChallengePage() {
  const [ingredient, setIngredient] = useState("");
  const [phase, setPhase] = useState<Phase>("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [challenge, setChallenge] = useState<ChallengeResponse | null>(null);
  const [activeIngredient, setActiveIngredient] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  async function fetchChallenge(ing: string) {
    setPhase("loading");
    setErrorMsg("");

    try {
      const res = await fetch("/api/challenge", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ingredient: ing }),
      });
      const data = (await res.json()) as ChallengeResponse & { error?: string };

      if (!res.ok) {
        setPhase("error");
        setErrorMsg(data.error ?? UI.challenge.errorFallback);
        return;
      }

      setChallenge(data);
      setActiveIngredient(ing);
      setPhase("done");
    } catch {
      setPhase("error");
      setErrorMsg(UI.challenge.errorNetwork);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = ingredient.trim();
    if (!trimmed) return;
    await fetchChallenge(trimmed);
  }

  function handleTryAnother() {
    fetchChallenge(activeIngredient);
  }

  function handleClear() {
    setPhase("idle");
    setChallenge(null);
    setErrorMsg("");
    setIngredient("");
    setActiveIngredient("");
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  return (
    <div className="page challenge-page">
      <header className="page-hero">
        <h1 className="hero-title">
          Kook<em>uitdaging</em>
        </h1>
        <p className="hero-sub">{UI.challenge.pageSubtitle}</p>
      </header>

      <form className="challenge-form" onSubmit={handleSubmit}>
        <div className="challenge-form-row">
          <input
            ref={inputRef}
            type="text"
            className="challenge-input"
            value={ingredient}
            onChange={(e) => setIngredient(e.target.value)}
            placeholder={UI.challenge.inputPlaceholder}
            aria-label={UI.challenge.inputLabel}
            disabled={phase === "loading"}
            autoFocus
          />
          <button
            type="submit"
            className="challenge-submit-btn"
            disabled={phase === "loading" || !ingredient.trim()}
          >
            {phase === "loading" ? UI.challenge.loadingBtn : UI.challenge.submitBtn}
          </button>
        </div>
      </form>

      {phase === "loading" && (
        <div className="challenge-state-loading" aria-live="polite">
          <div className="recipe-spinner" aria-hidden="true" />
          <span>{UI.challenge.loadingMsg}</span>
        </div>
      )}

      {phase === "error" && (
        <div className="challenge-state-error" role="alert">
          <strong>{UI.recipes.errorTitle}</strong>
          <p>{errorMsg}</p>
          <button className="recipe-retry-btn" onClick={handleClear}>
            {UI.challenge.retryBtn}
          </button>
        </div>
      )}

      {phase === "done" && challenge && (
        <ChallengeCard
          challenge={challenge}
          ingredient={activeIngredient}
          onTryAnother={handleTryAnother}
          onClear={handleClear}
        />
      )}
    </div>
  );
}

function ChallengeCard({
  challenge,
  ingredient,
  onTryAnother,
  onClear,
}: {
  challenge: ChallengeResponse;
  ingredient: string;
  onTryAnother: () => void;
  onClear: () => void;
}) {
  const diffClass =
    challenge.difficulty === "Beginner"
      ? "challenge-badge--beginner"
      : challenge.difficulty === "Intermediate"
      ? "challenge-badge--intermediate"
      : "challenge-badge--advanced";

  return (
    <article className="challenge-card">
      <div className="challenge-card-header">
        <span className="challenge-ingredient-label">{ingredient}</span>
        <div className="challenge-badges">
          <span className={`challenge-badge ${diffClass}`}>
            {difficultyLabel(challenge.difficulty)}
          </span>
        </div>
      </div>

      <p className="challenge-objective">{challenge.challenge}</p>

      <div className="challenge-meta">
        <div className="challenge-meta-item">
          <span className="challenge-meta-label">{UI.challenge.labelSkill}</span>
          <span className="challenge-meta-value">{challenge.skill}</span>
        </div>
        <div className="challenge-meta-item">
          <span className="challenge-meta-label">{UI.challenge.labelTime}</span>
          <span className="challenge-meta-value">
            {challenge.timeEstimate} {UI.challenge.labelMinutes}
          </span>
        </div>
      </div>

      <div className="challenge-why">
        <div className="challenge-why-label">{UI.challenge.labelWhy}</div>
        <p className="challenge-why-text">{challenge.why}</p>
      </div>

      <div className="challenge-actions">
        <button className="challenge-try-btn" onClick={onTryAnother}>
          {UI.challenge.tryAnotherBtn}
        </button>
        <button className="challenge-clear-btn" onClick={onClear}>
          {UI.challenge.clearBtn}
        </button>
      </div>
    </article>
  );
}
