"use client";

import { useState } from "react";

type ChallengeResult = {
  ingredient: string;
  challenge_text: string;
  difficulty?: "easy" | "medium" | "hard";
};

type Props = {
  ingredients: string[];
};

// Stub: wire up to POST /api/challenge to generate a cooking challenge via Claude API
export default function ChallengeModePanel({ ingredients }: Props) {
  const [selected, setSelected] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ChallengeResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleGenerate() {
    if (!selected) return;
    setLoading(true);
    setError(null);
    // TODO: call POST /api/challenge with { ingredient: selected }
    // const res = await fetch("/api/challenge", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ ingredient: selected }) });
    // const data = await res.json();
    // if (res.ok) setResult(data); else setError(data.error);
    setLoading(false);
    setResult({ ingredient: selected, challenge_text: "Stub uitdaging: komt binnenkort!", difficulty: "medium" });
  }

  return (
    <div className="card challenge-panel">
      <h2 className="panel-title">Kookuitdaging</h2>
      <label className="field-label" htmlFor="ingredient-select">
        Kies een ingrediënt
      </label>
      <select
        id="ingredient-select"
        className="field-select"
        value={selected}
        onChange={(e) => setSelected(e.target.value)}
      >
        <option value="">— selecteer —</option>
        {ingredients.map((ing) => (
          <option key={ing} value={ing}>{ing}</option>
        ))}
      </select>
      <button
        className="btn btn--primary"
        onClick={handleGenerate}
        disabled={!selected || loading}
      >
        {loading ? "Genereren…" : "Genereer uitdaging"}
      </button>
      {error && <p className="error-text">{error}</p>}
      {result && (
        <div className="challenge-result">
          <p className="challenge-text">{result.challenge_text}</p>
          {result.difficulty && (
            <span className="challenge-difficulty">{result.difficulty}</span>
          )}
        </div>
      )}
    </div>
  );
}
