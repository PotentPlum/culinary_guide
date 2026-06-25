"use client";

import { useEffect, useRef, useState } from "react";
import type { ChallengeResponse, ChallengeParams, IngredientUnit } from "@/lib/types";
import { UI } from "@/lib/strings";
import { TECHNIQUES } from "@/lib/techniques";
import { getFacet } from "@/lib/facets";
import { apiHeaders } from "@/lib/apiClient";
import EvaluationOverlay, { type AttemptInput } from "@/components/EvaluationOverlay";
import type { ChallengeRecord } from "@/app/api/challenges/list/route";

// ─── Log attempt helper ───────────────────────────────────────────────────────

async function logAttempt(attempt: AttemptInput) {
  try {
    await fetch("/api/profile/log-attempt", {
      method: "POST",
      headers: { "Content-Type": "application/json", ...apiHeaders() },
      body: JSON.stringify(attempt),
    });
  } catch {
    // fail silently — logging is non-blocking
  }
}

type Phase = "idle" | "loading" | "error" | "done";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("nl-NL", { day: "numeric", month: "short", year: "numeric" });
}

async function fetchHistory(): Promise<ChallengeRecord[]> {
  try {
    const res = await fetch("/api/challenges/list", { headers: apiHeaders() });
    if (!res.ok) return [];
    const d = (await res.json()) as { challenges?: ChallengeRecord[] };
    return d.challenges ?? [];
  } catch {
    return [];
  }
}

const DIFFICULTY: Record<ChallengeResponse["difficulty"], { label: string; cls: string }> = {
  Beginner:     { label: UI.challenge.difficultyBeginner,     cls: "challenge-badge--beginner" },
  Intermediate: { label: UI.challenge.difficultyIntermediate, cls: "challenge-badge--intermediate" },
  Advanced:     { label: UI.challenge.difficultyAdvanced,     cls: "challenge-badge--advanced" },
};

const CUISINES = getFacet("cuisine").options.map((o) => o.value);

const UNIT_OPTIONS: { value: IngredientUnit; label: string }[] = [
  { value: "stuks", label: UI.challenge.unitStuks },
  { value: "gram",  label: UI.challenge.unitGram },
  { value: "ml",    label: UI.challenge.unitMl },
  { value: "liter", label: UI.challenge.unitLiter },
];

const TIME_OPTIONS: { value: ChallengeParams["timeLimit"]; label: string }[] = [
  { value: undefined, label: UI.challenge.timeLimitAny },
  { value: "<15",     label: UI.challenge.timeLimit15 },
  { value: "<30",     label: UI.challenge.timeLimit30 },
  { value: "<60",     label: UI.challenge.timeLimit60 },
  { value: ">60",     label: UI.challenge.timeLimitOver60 },
];

type Submitted = { ingredient: string; params: ChallengeParams };

function recordToResponse(c: ChallengeRecord): ChallengeResponse {
  return {
    id: c.id,
    challenge: c.challenge_text,
    skill: c.skill ?? "",
    difficulty: (c.difficulty as ChallengeResponse["difficulty"]) ?? "Beginner",
    timeEstimate: c.time_estimate ?? 0,
    why: c.why ?? "",
    outputType: c.output_type ?? "",
    tags: {},
    taxonomy: { techniques: c.techniques, cuisines: c.cuisines, ingredients: c.ingredients },
    recipe: c.recipe ?? { title: "", servings: 0, ingredients: [], steps: [] },
  };
}

export default function ChallengePage() {
  const [ingredient, setIngredient] = useState("");
  const [phase, setPhase] = useState<Phase>("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [challenge, setChallenge] = useState<ChallengeResponse | null>(null);
  const [submitted, setSubmitted] = useState<Submitted | null>(null);
  const [params, setParams] = useState<ChallengeParams>({});
  const [history, setHistory] = useState<ChallengeRecord[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const historySeq = useRef(0);

  function refreshHistory() {
    const seq = ++historySeq.current;
    fetchHistory().then((h) => { if (historySeq.current === seq) setHistory(h); });
  }

  useEffect(() => {
    refreshHistory();
  }, []);

  async function fetchChallenge(ing: string, p: ChallengeParams) {
    setPhase("loading");
    setErrorMsg("");

    try {
      const res = await fetch("/api/challenge", {
        method: "POST",
        headers: apiHeaders({ "Content-Type": "application/json" }),
        body: JSON.stringify({ ingredient: ing, params: p }),
      });
      const data = (await res.json()) as ChallengeResponse & { error?: string };

      if (!res.ok) {
        setPhase("error");
        setErrorMsg(data.error ?? UI.challenge.errorFallback);
        return;
      }

      setChallenge(data);
      setSubmitted({ ingredient: ing, params: p });
      setPhase("done");
      refreshHistory();
    } catch {
      setPhase("error");
      setErrorMsg(UI.challenge.errorNetwork);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = ingredient.trim();
    if (!trimmed) return;
    await fetchChallenge(trimmed, params);
  }

  function handleTryAnother() {
    if (submitted) fetchChallenge(submitted.ingredient, submitted.params);
  }

  function handleClear() {
    setPhase("idle");
    setChallenge(null);
    setErrorMsg("");
    setIngredient("");
    setSubmitted(null);
    setParams({});
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  function handleOpenHistory(c: ChallengeRecord) {
    setChallenge(recordToResponse(c));
    setSubmitted({ ingredient: c.ingredient, params: (c.params as ChallengeParams) ?? {} });
    setPhase("done");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function setParam<K extends keyof ChallengeParams>(key: K, value: ChallengeParams[K]) {
    setParams((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <div className="page challenge-page">
      <header className="page-hero">
        <div className="hero-eyebrow">
          <span className="hero-loc">{UI.hero.eyebrowLoc}</span>
          <span className="hero-dot">·</span>
          <span>{UI.challenge.eyebrowSub}</span>
        </div>
        <h1 className="hero-title">
          {UI.challenge.pageTitlePre}<em>{UI.challenge.pageTitleEm}</em>
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

        <div className="challenge-quantity-row">
          <label className="challenge-quantity-label" htmlFor="quantity-input">
            {UI.challenge.quantityLabel}
          </label>
          <input
            id="quantity-input"
            type="number"
            min={1}
            className="challenge-quantity-input"
            value={params.quantity ?? ""}
            onChange={(e) => setParam("quantity", e.target.value ? Number(e.target.value) : undefined)}
            onBlur={() => {
              if (params.quantity !== undefined && params.quantity < 1) setParam("quantity", 1);
            }}
            placeholder={UI.challenge.quantityPlaceholder}
            disabled={phase === "loading"}
          />
          <select
            id="unit-input"
            className="challenge-quantity-select"
            value={params.unit ?? ""}
            onChange={(e) => setParam("unit", (e.target.value as IngredientUnit) || undefined)}
            disabled={phase === "loading"}
            aria-label={UI.challenge.unitLabel}
          >
            <option value="">—</option>
            {UNIT_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        <div className="challenge-params">
          <p className="challenge-params-title">{UI.challenge.paramsTitle}</p>
          <div className="challenge-params-grid">
            <ParamSelect
              id="param-technique"
              label={UI.challenge.techniqueLabel}
              value={params.technique ?? ""}
              onChange={(v) => setParam("technique", v)}
              placeholder={UI.challenge.techniquePlaceholder}
              options={TECHNIQUES.map((t) => ({ value: t.nl, label: t.nl }))}
              disabled={phase === "loading"}
            />
            <ParamSelect
              id="param-cuisine"
              label={UI.challenge.cuisineLabel}
              value={params.cuisine ?? ""}
              onChange={(v) => setParam("cuisine", v)}
              placeholder={UI.challenge.cuisinePlaceholder}
              options={CUISINES.map((c) => ({ value: c, label: c }))}
              disabled={phase === "loading"}
            />
            <ParamSelect
              id="param-time"
              label={UI.challenge.timeLimitLabel}
              value={params.timeLimit ?? ""}
              onChange={(v) => setParam("timeLimit", v as ChallengeParams["timeLimit"])}
              options={TIME_OPTIONS.map((o) => ({ value: o.value ?? "", label: o.label }))}
              disabled={phase === "loading"}
            />

            <div className="challenge-param-field challenge-param-field--checkbox">
              <label className="challenge-param-checkbox-label">
                <input
                  type="checkbox"
                  className="challenge-param-checkbox"
                  checked={params.simpleAndSmall ?? false}
                  onChange={(e) => setParam("simpleAndSmall", e.target.checked || undefined)}
                  disabled={phase === "loading"}
                />
                <span className="challenge-param-checkbox-text">
                  <span className="challenge-param-label">{UI.challenge.simpleLabel}</span>
                  <span className="challenge-param-hint">{UI.challenge.simpleDescription}</span>
                </span>
              </label>
            </div>
          </div>
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
          <strong>{UI.challenge.errorTitle}</strong>
          <p>{errorMsg}</p>
          <button className="recipe-retry-btn" onClick={handleClear}>
            {UI.challenge.retryBtn}
          </button>
        </div>
      )}

      {phase === "done" && challenge && submitted && (
        <ChallengeCard
          key={challenge.id ?? challenge.challenge}
          challenge={challenge}
          ingredient={submitted.ingredient}
          onTryAnother={handleTryAnother}
          onClear={handleClear}
        />
      )}

      {history.length > 0 && (
        <section className="challenge-history">
          <h2 className="challenge-history-title">{UI.challenge.historyTitle}</h2>
          <div className="challenge-history-list">
            {history.map((c) => (
              <button
                key={c.id}
                type="button"
                className="challenge-history-entry challenge-history-entry--clickable"
                onClick={() => handleOpenHistory(c)}
              >
                <div className="challenge-history-entry-meta">
                  <span className="challenge-history-ingredient">{c.ingredient}</span>
                  {c.difficulty && DIFFICULTY[c.difficulty as keyof typeof DIFFICULTY] && (
                    <span className={`challenge-badge ${DIFFICULTY[c.difficulty as keyof typeof DIFFICULTY].cls}`}>
                      {DIFFICULTY[c.difficulty as keyof typeof DIFFICULTY].label}
                    </span>
                  )}
                  <span className="challenge-history-date">{formatDate(c.created_at)}</span>
                </div>
                <p className="challenge-history-text">{c.challenge_text}</p>
                {c.skill && <p className="challenge-history-skill">{c.skill}</p>}
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function ParamSelect({
  id, label, value, onChange, placeholder, options, disabled,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string | undefined) => void;
  placeholder?: string;
  options: { value: string; label: string }[];
  disabled: boolean;
}) {
  return (
    <div className="challenge-param-field">
      <label className="challenge-param-label" htmlFor={id}>{label}</label>
      <select
        id={id}
        className="challenge-param-select"
        value={value}
        onChange={(e) => onChange(e.target.value || undefined)}
        disabled={disabled}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
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
  const diff = DIFFICULTY[challenge.difficulty];
  const [showEval, setShowEval] = useState(false);
  const [evalSuccess, setEvalSuccess] = useState(false);

  async function handleEvalSubmit(attempt: AttemptInput) {
    setShowEval(false);
    await logAttempt(attempt);
    setEvalSuccess(true);
  }

  return (
    <>
      {showEval && (
        <EvaluationOverlay
          sourceType="challenge"
          sourceId={challenge.id ?? null}
          techniques={challenge.taxonomy?.techniques ?? []}
          cuisines={challenge.taxonomy?.cuisines ?? []}
          ingredients={challenge.taxonomy?.ingredients ?? []}
          onSubmit={handleEvalSubmit}
          onDismiss={() => setShowEval(false)}
        />
      )}
      <article className="challenge-card">
        <div className="challenge-card-header">
          <span className="challenge-ingredient-label">{ingredient}</span>
          <div className="challenge-card-badges">
            {challenge.outputType && (
              <span className="challenge-output-badge">{challenge.outputType}</span>
            )}
            <span className={`challenge-badge ${diff.cls}`}>{diff.label}</span>
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

        {challenge.recipe && (
          <div className="challenge-recipe">
            <h3 className="challenge-recipe-title">{challenge.recipe.title}</h3>
            {challenge.recipe.servings > 0 && (
              <p className="challenge-recipe-servings">
                {challenge.recipe.servings} {UI.challenge.labelServings}
              </p>
            )}

            <div className="challenge-recipe-section">
              <h4 className="challenge-recipe-section-label">{UI.challenge.labelIngredients}</h4>
              <ul className="challenge-recipe-ingredients">
                {challenge.recipe.ingredients.map((ing, i) => (
                  <li key={i} className="challenge-recipe-ingredient">
                    <span className="challenge-recipe-amount">
                      {ing.amount}{ing.unit ? ` ${ing.unit}` : ""}
                    </span>
                    <span className="challenge-recipe-ing-name">{ing.name}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="challenge-recipe-section">
              <h4 className="challenge-recipe-section-label">{UI.challenge.labelSteps}</h4>
              <ol className="challenge-recipe-steps">
                {challenge.recipe.steps.map((s) => (
                  <li key={s.step} className="challenge-recipe-step">{s.instruction}</li>
                ))}
              </ol>
            </div>

            {challenge.recipe.tips && challenge.recipe.tips.length > 0 && (
              <div className="challenge-recipe-section challenge-recipe-tips-section">
                <h4 className="challenge-recipe-section-label">{UI.challenge.labelRecipeTips}</h4>
                <ul className="challenge-recipe-tips">
                  {challenge.recipe.tips.map((tip, i) => (
                    <li key={i}>{tip}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        <div className="challenge-cooked-row">
          {evalSuccess ? (
            <span className="eval-success-banner">{UI.challenge.completedMsg}</span>
          ) : (
            <button
              type="button"
              className="cooked-btn"
              onClick={() => setShowEval(true)}
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                <path d="M2 7l3.5 3.5L12 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {UI.challenge.completedBtn}
            </button>
          )}
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
    </>
  );
}
