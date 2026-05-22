"use client";

import { useRef, useState } from "react";
import type { ChallengeResponse, ChallengeParams, IngredientUnit } from "@/lib/types";
import { UI } from "@/lib/strings";
import { TECHNIQUES } from "@/lib/techniques";
import { getFacet } from "@/lib/facets";

type Phase = "idle" | "loading" | "error" | "done";

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

export default function ChallengePage() {
  const [ingredient, setIngredient] = useState("");
  const [phase, setPhase] = useState<Phase>("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [challenge, setChallenge] = useState<ChallengeResponse | null>(null);
  const [submitted, setSubmitted] = useState<Submitted | null>(null);
  const [params, setParams] = useState<ChallengeParams>({});
  const inputRef = useRef<HTMLInputElement>(null);

  async function fetchChallenge(ing: string, p: ChallengeParams) {
    setPhase("loading");
    setErrorMsg("");

    try {
      const res = await fetch("/api/challenge", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
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

  function setParam<K extends keyof ChallengeParams>(key: K, value: ChallengeParams[K]) {
    setParams((prev) => ({ ...prev, [key]: value }));
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
          challenge={challenge}
          ingredient={submitted.ingredient}
          onTryAnother={handleTryAnother}
          onClear={handleClear}
        />
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

  return (
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
