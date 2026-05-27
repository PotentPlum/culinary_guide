"use client";

import { useState } from "react";
import { UI } from "@/lib/strings";

export type AttemptInput = {
  source_type: "challenge" | "recipe";
  source_id: string | null;
  score: number | null;
  techniques: string[];
  cuisines: string[];
  ingredients: string[];
  taste_score: number | null;
  notes: string | null;
};

type Props = {
  sourceType: "challenge" | "recipe";
  sourceId: string | null;
  techniques: string[];
  cuisines: string[];
  ingredients: string[];
  onSubmit: (attempt: AttemptInput) => void;
  onDismiss: () => void;
};

export default function EvaluationOverlay({
  sourceType,
  sourceId,
  techniques,
  cuisines,
  ingredients,
  onSubmit,
  onDismiss,
}: Props) {
  const [score, setScore] = useState<number | null>(null);
  const [tasteScore, setTasteScore] = useState<number | null>(null);
  const [notes, setNotes] = useState("");

  function handleSubmit() {
    onSubmit({
      source_type: sourceType,
      source_id: sourceId,
      score,
      techniques,
      cuisines,
      ingredients,
      taste_score: tasteScore,
      notes: notes.trim() || null,
    });
  }

  function handleBackdropClick(e: React.MouseEvent<HTMLDivElement>) {
    if (e.target === e.currentTarget) {
      onDismiss();
    }
  }

  return (
    <div
      className="eval-overlay-backdrop"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="eval-overlay-title"
    >
      <div className="eval-overlay-modal">
        <h2 id="eval-overlay-title" className="eval-overlay-title">
          {UI.evaluation.title}
        </h2>

        <div className="eval-score-section">
          <p className="eval-score-label">{UI.evaluation.labelScore}</p>
          <div className="eval-score-grid" role="group" aria-label={UI.evaluation.labelScore}>
            {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                type="button"
                className={`eval-score-btn${score === n ? " selected" : ""}`}
                onClick={() => setScore(score === n ? null : n)}
                aria-pressed={score === n}
              >
                {n}
              </button>
            ))}
          </div>
        </div>

        <div className="eval-score-section">
          <p className="eval-score-label">{UI.evaluation.labelTaste}</p>
          <div className="eval-score-grid" role="group" aria-label={UI.evaluation.labelTaste}>
            {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                type="button"
                className={`eval-score-btn${tasteScore === n ? " selected" : ""}`}
                onClick={() => setTasteScore(tasteScore === n ? null : n)}
                aria-pressed={tasteScore === n}
              >
                {n}
              </button>
            ))}
          </div>
        </div>

        <div className="eval-notes-section">
          <label className="eval-score-label" htmlFor="eval-notes">
            {UI.evaluation.labelNotes}
          </label>
          <textarea
            id="eval-notes"
            className="eval-notes-textarea"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
          />
        </div>

        <div className="eval-overlay-actions">
          <button
            type="button"
            className="eval-submit-btn"
            onClick={handleSubmit}
          >
            {UI.evaluation.submitBtn}
          </button>
          <button
            type="button"
            className="eval-skip-btn"
            onClick={onDismiss}
          >
            {UI.evaluation.skipBtn}
          </button>
        </div>
      </div>
    </div>
  );
}
