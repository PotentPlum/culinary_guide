"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { RecipeOutput } from "@/lib/recipeIngestion";
import { UI } from "@/lib/strings";

type Tab = "url" | "photo";
type Phase = "idle" | "loading" | "error" | "done";

type SavedRecipe = {
  url: string;
  recipe: RecipeOutput;
  savedAt: number;
};

const STORAGE_KEY = "mise-saved-recipes";
const MAX_PHOTOS = 5;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];

function loadLocalSaved(): SavedRecipe[] {
  if (typeof window === "undefined") return [];
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "[]");
  } catch {
    return [];
  }
}

async function fetchRemoteSaved(): Promise<SavedRecipe[]> {
  try {
    const res = await fetch("/api/recipes/list");
    if (!res.ok) return [];
    const json = (await res.json()) as { recipes: Array<{ id: string; name: string; instructions: string; source_url: string; created_at: string }> };
    return (json.recipes ?? []).map((row) => {
      let recipe: RecipeOutput;
      try {
        recipe = JSON.parse(row.instructions) as RecipeOutput;
      } catch {
        recipe = {
          title: row.name,
          description: "",
          prepTimeMinutes: null,
          cookTimeMinutes: null,
          totalTimeMinutes: null,
          cuisine: [],
          category: [],
          nutrition: { calories: null, protein: null, carbs: null, fat: null },
          ingredientSections: [],
          instructionSections: [],
        };
      }
      return { url: row.source_url, recipe, savedAt: new Date(row.created_at).getTime() };
    });
  } catch {
    return [];
  }
}

async function saveRemote(item: SavedRecipe): Promise<void> {
  const res = await fetch("/api/recipes/save", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: item.recipe.title,
      ingredients: item.recipe.ingredientSections.flatMap((s) => s.ingredients),
      instructions: JSON.stringify(item.recipe),
      source_url: item.url,
    }),
  });
  if (!res.ok) {
    const json = (await res.json()) as { error?: string };
    throw new Error(json.error ?? "Opslaan mislukt");
  }
}

async function resizeToBase64(
  file: File,
  maxDim = 1200,
  quality = 0.82,
): Promise<{ data: string; mediaType: string }> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      let { width, height } = img;
      if (width > maxDim || height > maxDim) {
        if (width >= height) {
          height = Math.round((height / width) * maxDim);
          width = maxDim;
        } else {
          width = Math.round((width / height) * maxDim);
          height = maxDim;
        }
      }
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      if (!ctx) { reject(new Error("Canvas niet beschikbaar")); return; }
      ctx.drawImage(img, 0, 0, width, height);
      const dataUrl = canvas.toDataURL("image/jpeg", quality);
      resolve({ data: dataUrl.split(",")[1], mediaType: "image/jpeg" });
    };
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("Afbeelding kon niet worden geladen"));
    };
    img.src = objectUrl;
  });
}

export default function RecipesPage() {
  const [tab, setTab] = useState<Tab>("url");
  const [url, setUrl] = useState("");
  const [phase, setPhase] = useState<Phase>("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [recipe, setRecipe] = useState<RecipeOutput | null>(null);
  const [activeUrl, setActiveUrl] = useState("");
  const [savedRecipes, setSavedRecipes] = useState<SavedRecipe[]>([]);
  const [saveError, setSaveError] = useState("");
  const [photoFiles, setPhotoFiles] = useState<File[]>([]);
  const [photoDrag, setPhotoDrag] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setSavedRecipes(loadLocalSaved());
    fetchRemoteSaved().then((remote) => {
      if (remote.length > 0) setSavedRecipes(remote);
    });
  }, []);

  function handleTabChange(newTab: Tab) {
    setTab(newTab);
    if (phase !== "done") {
      setPhase("idle");
      setErrorMsg("");
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = url.trim();
    if (!trimmed) return;

    setPhase("loading");
    setErrorMsg("");
    setRecipe(null);

    try {
      const res = await fetch("/api/recipe", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url: trimmed }),
      });
      const data = (await res.json()) as RecipeOutput & { error?: string };

      if (!res.ok) {
        setPhase("error");
        setErrorMsg(data.error ?? UI.recipes.errorFallback);
        return;
      }

      setRecipe(data);
      setActiveUrl(trimmed);
      setPhase("done");
    } catch {
      setPhase("error");
      setErrorMsg(UI.recipes.errorNetwork);
    }
  }

  async function handlePhotoSubmit() {
    if (photoFiles.length === 0) return;

    setPhase("loading");
    setErrorMsg("");
    setRecipe(null);

    try {
      const resized = await Promise.all(photoFiles.map(resizeToBase64));
      const res = await fetch("/api/recipe-from-images", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ images: resized }),
      });
      const data = (await res.json()) as RecipeOutput & { error?: string };

      if (!res.ok) {
        setPhase("error");
        setErrorMsg(data.error ?? UI.recipes.errorFallback);
        return;
      }

      setRecipe(data);
      setActiveUrl(`mise-photo:${Date.now()}`);
      setPhase("done");
    } catch {
      setPhase("error");
      setErrorMsg(UI.recipes.errorNetwork);
    }
  }

  function handleReset() {
    setPhase("idle");
    setRecipe(null);
    setErrorMsg("");
    setUrl("");
    setActiveUrl("");
    setPhotoFiles([]);
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  async function handleSave() {
    if (!recipe || !activeUrl) return;
    setSaveError("");
    const item: SavedRecipe = { url: activeUrl, recipe, savedAt: Date.now() };
    const updated = [item, ...savedRecipes.filter((r) => r.url !== activeUrl)];
    setSavedRecipes(updated);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    try {
      await saveRemote(item);
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : "Opslaan mislukt");
    }
  }

  function handleRemoveSaved(savedUrl: string) {
    const updated = savedRecipes.filter((r) => r.url !== savedUrl);
    setSavedRecipes(updated);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  }

  function handleLoadSaved(saved: SavedRecipe) {
    setRecipe(saved.recipe);
    setActiveUrl(saved.url);
    setUrl(saved.url.startsWith("mise-photo:") ? "" : saved.url);
    setPhase("done");
    setErrorMsg("");
  }

  function addFiles(incoming: FileList | null) {
    if (!incoming) return;
    const valid = Array.from(incoming).filter(
      (f) => ACCEPTED_TYPES.includes(f.type) && f.size <= MAX_PHOTO_BYTES,
    );
    setPhotoFiles((prev) => {
      const merged = [...prev, ...valid];
      return merged.slice(0, MAX_PHOTOS);
    });
  }

  const handleDropzoneDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setPhotoDrag(false);
    addFiles(e.dataTransfer.files);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const isSaved = savedRecipes.some((r) => r.url === activeUrl);
  const subtitle = tab === "photo" ? UI.recipes.pageSubtitlePhoto : UI.recipes.pageSubtitle;

  return (
    <div className="page recipes-page">
      <header className="page-hero">
        <h1 className="hero-title">
          Recept <em>inladen</em>
        </h1>
        <p className="hero-sub">{subtitle}</p>
      </header>

      {/* Tab switcher */}
      <div className="recipe-tabs">
        <button
          type="button"
          className={`recipe-tab${tab === "url" ? " recipe-tab--active" : ""}`}
          onClick={() => handleTabChange("url")}
        >
          {UI.recipes.tabUrl}
        </button>
        <button
          type="button"
          className={`recipe-tab${tab === "photo" ? " recipe-tab--active" : ""}`}
          onClick={() => handleTabChange("photo")}
        >
          {UI.recipes.tabPhoto}
        </button>
      </div>

      {/* URL tab */}
      {tab === "url" && (
        <form className="recipe-import-form" onSubmit={handleSubmit}>
          <div className="recipe-import-row">
            <input
              ref={inputRef}
              type="url"
              className="recipe-import-input"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder={UI.recipes.inputPlaceholder}
              aria-label={UI.recipes.inputLabel}
              required
              disabled={phase === "loading"}
              autoFocus
            />
            <button
              type="submit"
              className="recipe-import-btn"
              disabled={phase === "loading" || !url.trim()}
            >
              {phase === "loading" ? UI.recipes.loadingBtn : UI.recipes.submitBtn}
            </button>
          </div>
          <p className="recipe-import-hint">{UI.recipes.hintText}</p>
        </form>
      )}

      {/* Photo tab */}
      {tab === "photo" && (
        <div className="recipe-import-form">
          {/* Hidden file input */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            style={{ display: "none" }}
            onChange={(e) => addFiles(e.target.files)}
          />

          {/* Dropzone */}
          <div
            className={`recipe-photo-zone${photoDrag ? " recipe-photo-zone--drag" : ""}`}
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); setPhotoDrag(true); }}
            onDragLeave={() => setPhotoDrag(false)}
            onDrop={handleDropzoneDrop}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") fileInputRef.current?.click(); }}
            aria-label={UI.recipes.photoDropzone}
          >
            <p className="recipe-photo-zone-label">{UI.recipes.photoDropzone}</p>
            <p className="recipe-import-hint">{UI.recipes.photoHint}</p>
          </div>

          {/* Preview thumbnails */}
          {photoFiles.length > 0 && (
            <ul className="recipe-photo-list" aria-label="Geselecteerde foto's">
              {photoFiles.map((file, i) => {
                const src = URL.createObjectURL(file);
                return (
                  <li key={i} className="recipe-photo-item">
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={src}
                      alt={file.name}
                      className="recipe-photo-thumb"
                      onLoad={() => URL.revokeObjectURL(src)}
                    />
                    <button
                      type="button"
                      className="recipe-photo-remove"
                      aria-label={`Verwijder ${file.name}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        setPhotoFiles((prev) => prev.filter((_, j) => j !== i));
                      }}
                    >
                      ×
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {/* Submit row */}
          <div className="recipe-photo-actions">
            <button
              type="button"
              className="recipe-photo-submit"
              disabled={phase === "loading" || photoFiles.length === 0}
              onClick={handlePhotoSubmit}
            >
              {phase === "loading" ? UI.recipes.loadingBtn : UI.recipes.photoSubmitBtn}
            </button>
          </div>
        </div>
      )}

      {phase === "loading" && (
        <div className="recipe-state-loading" aria-live="polite">
          <div className="recipe-spinner" aria-hidden="true" />
          <span>{tab === "photo" ? UI.recipes.photoLoadingMsg : UI.recipes.loadingMsg}</span>
        </div>
      )}

      {phase === "error" && (
        <div className="recipe-state-error" role="alert">
          <strong>{UI.recipes.errorTitle}</strong>
          <p>{errorMsg}</p>
          <button className="recipe-retry-btn" onClick={handleReset}>
            {UI.recipes.retryBtn}
          </button>
        </div>
      )}

      {phase === "done" && recipe && (
        <RecipeResult
          recipe={recipe}
          isSaved={isSaved}
          onSave={handleSave}
          onReset={handleReset}
          saveError={saveError}
        />
      )}

      {phase === "idle" && savedRecipes.length > 0 && (
        <SavedRecipesList
          savedRecipes={savedRecipes}
          onLoad={handleLoadSaved}
          onRemove={handleRemoveSaved}
        />
      )}
    </div>
  );
}

// ─── Saved recipes list ───────────────────────────────────────────────────────

function SavedRecipesList({
  savedRecipes,
  onLoad,
  onRemove,
}: {
  savedRecipes: SavedRecipe[];
  onLoad: (saved: SavedRecipe) => void;
  onRemove: (url: string) => void;
}) {
  return (
    <section className="recipe-saved-section">
      <h2 className="recipe-saved-heading">{UI.recipes.savedSection}</h2>
      <ul className="recipe-saved-list">
        {savedRecipes.map((saved) => {
          let domain: string;
          if (saved.url.startsWith("mise-photo:")) {
            domain = UI.recipes.photoSource;
          } else {
            domain = saved.url;
            try {
              domain = new URL(saved.url).hostname.replace(/^www\./, "");
            } catch {}
          }
          const date = new Date(saved.savedAt).toLocaleDateString("nl-NL", {
            day: "numeric",
            month: "long",
          });
          return (
            <li key={saved.url} className="recipe-saved-item">
              <button className="recipe-saved-item-load" onClick={() => onLoad(saved)}>
                <span className="recipe-saved-item-title">{saved.recipe.title}</span>
                <span className="recipe-saved-item-meta">{domain} · {date}</span>
              </button>
              <button
                className="recipe-saved-item-remove"
                onClick={() => onRemove(saved.url)}
                aria-label={UI.recipes.savedRemoveLabel}
              >
                ×
              </button>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

// ─── Recipe result display ────────────────────────────────────────────────────

function RecipeResult({
  recipe,
  isSaved,
  onSave,
  onReset,
  saveError,
}: {
  recipe: RecipeOutput;
  isSaved: boolean;
  onSave: () => void;
  onReset: () => void;
  saveError?: string;
}) {
  const times = (
    [
      { label: UI.recipes.labelPrepTime, value: recipe.prepTimeMinutes },
      { label: UI.recipes.labelCookTime, value: recipe.cookTimeMinutes },
      { label: UI.recipes.labelTotalTime, value: recipe.totalTimeMinutes },
    ] as { label: string; value: number | null }[]
  ).filter((t) => t.value !== null);

  const cuisines = toArray(recipe.cuisine);
  const categories = toArray(recipe.category);
  const hasMeta = times.length > 0 || cuisines.length > 0 || categories.length > 0;
  const hasNutrition = Object.values(recipe.nutrition).some(Boolean);

  return (
    <article className="recipe-result">
      {/* ── Header ── */}
      <div className="recipe-result-header">
        <h2 className="recipe-result-title">{recipe.title}</h2>
        <div className="recipe-result-actions">
          <button
            className={`recipe-save-btn${isSaved ? " recipe-save-btn--saved" : ""}`}
            onClick={onSave}
            disabled={isSaved}
          >
            {isSaved ? UI.recipes.savedBtn : UI.recipes.saveBtn}
          </button>
          {saveError && <p className="recipe-save-error">{saveError}</p>}
          <button className="recipe-clear-btn" onClick={onReset} aria-label={UI.recipes.clearBtn}>
            {UI.recipes.clearBtn}
          </button>
        </div>
      </div>

      {recipe.description && (
        <p className="recipe-result-desc">{recipe.description}</p>
      )}

      {/* ── Meta chips ── */}
      {hasMeta && (
        <div className="recipe-result-meta">
          {times.map((t) => (
            <span key={t.label} className="recipe-meta-chip recipe-meta-chip--time">
              <span className="recipe-meta-label">{t.label}</span>
              <span className="recipe-meta-value">
                {t.value} {UI.recipes.labelMinutes}
              </span>
            </span>
          ))}
          {[...cuisines, ...categories].map((tag) => (
            <span key={tag} className="recipe-meta-chip recipe-meta-chip--tag">
              {tag}
            </span>
          ))}
        </div>
      )}

      {/* ── Body: ingredients + instructions ── */}
      <div className="recipe-result-body">
        {/* Ingredients column */}
        <div className="recipe-col recipe-ing-col">
          <h3 className="recipe-col-title">{UI.recipes.sectionIngredients}</h3>
          {recipe.ingredientSections.map((section, si) => (
            <div key={si} className="recipe-section">
              {!(section.sectionName === "Ingrediënten" && recipe.ingredientSections.length === 1) && (
                <div className="recipe-section-name">{section.sectionName}</div>
              )}
              <ul className="recipe-ing-list">
                {section.ingredients.map((ing, ii) => (
                  <li key={ii}>{ing}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Instructions column */}
        <div className="recipe-col recipe-step-col">
          <h3 className="recipe-col-title">{UI.recipes.sectionInstructions}</h3>
          {recipe.instructionSections.map((section, si) => (
            <div key={si} className="recipe-section">
              {!(section.sectionName === "Bereiding" && recipe.instructionSections.length === 1) && (
                <div className="recipe-section-name">{section.sectionName}</div>
              )}
              {section.steps.map((step) => (
                <div key={step.stepNumber} className="recipe-step">
                  <span className="recipe-step-num" aria-hidden="true">
                    {step.stepNumber}
                  </span>
                  <p className="recipe-step-text">{step.text}</p>
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>

      {/* ── Nutrition ── */}
      {hasNutrition && (
        <div className="recipe-nutrition">
          <h3 className="recipe-col-title">{UI.recipes.sectionNutrition}</h3>
          <div className="recipe-nutrition-chips">
            {recipe.nutrition.calories && (
              <NutritionChip label={UI.recipes.labelCalories} value={recipe.nutrition.calories} />
            )}
            {recipe.nutrition.protein && (
              <NutritionChip label={UI.recipes.labelProtein} value={recipe.nutrition.protein} />
            )}
            {recipe.nutrition.carbs && (
              <NutritionChip label={UI.recipes.labelCarbs} value={recipe.nutrition.carbs} />
            )}
            {recipe.nutrition.fat && (
              <NutritionChip label={UI.recipes.labelFat} value={recipe.nutrition.fat} />
            )}
          </div>
        </div>
      )}
    </article>
  );
}

function NutritionChip({ label, value }: { label: string; value: string }) {
  return (
    <span className="recipe-nutrition-chip">
      <span className="recipe-nutrition-label">{label}</span>
      <span className="recipe-nutrition-value">{value}</span>
    </span>
  );
}

function toArray(v: string | string[]): string[] {
  if (!v) return [];
  return Array.isArray(v) ? v.filter(Boolean) : v ? [v] : [];
}
