"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import type { IngredientItem, IngredientSection, RecipeOutput } from "@/lib/recipeIngestion";
import { UI } from "@/lib/strings";
import { FACETS, type FacetKey, type RecipeTags, getCuisineColor } from "@/lib/facets";
import type { RecipeRow } from "@/app/api/recipes/list/route";

type Tab = "url" | "photo";
type Phase = "idle" | "loading" | "error" | "done";
type View = "library" | "detail";
type SortOrder = "date-desc" | "date-asc" | "alpha-asc";

type SavedRecipe = {
  url: string;           // technical key — real URL or mise-photo:timestamp
  source: string;        // human-readable display: domain, cookbook name, etc.
  recipe: RecipeOutput;
  savedAt: number;
  remoteId?: string;     // Supabase row ID, set after list
  tags?: RecipeTags;
};

const STORAGE_KEY = "mise-saved-recipes";
const PHOTO_URL_PREFIX = "mise-photo:";
const MAX_PHOTOS = 5;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];

function getAccentColor(saved: SavedRecipe): string | undefined {
  const cuisineTags = saved.tags?.cuisine ?? [];
  for (const c of cuisineTags) {
    const color = getCuisineColor(c);
    if (color) return color;
  }
  // Fall back to Schema.org cuisine field
  for (const c of toArray(saved.recipe.cuisine)) {
    const color = getCuisineColor(c);
    if (color) return color;
  }
  return undefined;
}

function deriveDomain(url: string): string {
  if (url.startsWith(PHOTO_URL_PREFIX)) return UI.recipes.photoSource;
  try { return new URL(url).hostname.replace(/^www\./, ""); } catch { return url; }
}

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
    const json = (await res.json()) as { recipes: RecipeRow[] };
    return (json.recipes ?? []).map((row) => {
      let recipe: RecipeOutput;
      try {
        recipe = JSON.parse(row.instructions) as RecipeOutput;
      } catch {
        recipe = {
          title: row.name,
          description: row.description ?? "",
          prepTimeMinutes: row.prep_time ?? null,
          cookTimeMinutes: row.cook_time ?? null,
          totalTimeMinutes: row.total_time ?? null,
          servings: row.servings ?? null,
          cuisine: row.cuisine ?? [],
          category: row.category ?? [],
          keywords: row.keywords ?? [],
          image: row.image_url ?? null,
          author: row.author ?? null,
          suitableForDiet: row.suitable_for_diet ?? [],
          datePublished: row.date_published ?? null,
          aggregateRating: row.aggregate_rating ?? null,
          video: row.video_url ?? null,
          nutrition: row.nutrition ?? { calories: null, protein: null, carbs: null, fat: null },
          ingredientSections: [],
          instructionSections: [],
        };
      }
      recipe = {
        ...recipe,
        description: row.description ?? recipe.description,
        prepTimeMinutes: row.prep_time ?? recipe.prepTimeMinutes,
        cookTimeMinutes: row.cook_time ?? recipe.cookTimeMinutes,
        totalTimeMinutes: row.total_time ?? recipe.totalTimeMinutes,
        servings: row.servings ?? recipe.servings,
        cuisine: row.cuisine ?? recipe.cuisine,
        category: row.category ?? recipe.category,
        keywords: row.keywords ?? recipe.keywords ?? [],
        image: row.image_url ?? recipe.image ?? null,
        author: row.author ?? recipe.author ?? null,
        suitableForDiet: row.suitable_for_diet ?? recipe.suitableForDiet ?? [],
        datePublished: row.date_published ?? recipe.datePublished ?? null,
        aggregateRating: row.aggregate_rating ?? recipe.aggregateRating ?? null,
        video: row.video_url ?? recipe.video ?? null,
        nutrition: row.nutrition ?? recipe.nutrition,
      };
      return {
        url: row.source_url,
        source: row.source ?? deriveDomain(row.source_url),
        recipe,
        savedAt: new Date(row.created_at).getTime(),
        remoteId: row.id,
        tags: row.tags ?? {},
      };
    });
  } catch {
    return [];
  }
}

async function saveRemote(item: SavedRecipe): Promise<string> {
  const r = item.recipe;
  const res = await fetch("/api/recipes/save", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: r.title,
      ingredients: flatIngredientTexts(r.ingredientSections),
      instructions: JSON.stringify(r),
      source_url: item.url,
      source: item.source,
      tags: item.tags ?? {},
      description: r.description ?? "",
      prep_time: r.prepTimeMinutes ?? null,
      cook_time: r.cookTimeMinutes ?? null,
      total_time: r.totalTimeMinutes ?? null,
      servings: r.servings ?? null,
      cuisine: toArray(r.cuisine),
      category: toArray(r.category),
      keywords: r.keywords ?? [],
      image_url: r.image ?? null,
      author: r.author ?? null,
      suitable_for_diet: r.suitableForDiet ?? [],
      date_published: r.datePublished ?? null,
      aggregate_rating: r.aggregateRating ?? null,
      video_url: r.video ?? null,
      nutrition: r.nutrition ?? {},
    }),
  });
  if (!res.ok) {
    const json = (await res.json()) as { error?: string };
    throw new Error(json.error ?? "Opslaan mislukt");
  }
  const json = (await res.json()) as { id: string };
  return json.id;
}

async function resizeToBase64(
  file: File,
  maxDim = 800,
  quality = 0.75,
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
  const [view, setView] = useState<View>("library");
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
  const [pendingSource, setPendingSource] = useState("");
  const [sourceManuallyEdited, setSourceManuallyEdited] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortOrder, setSortOrder] = useState<SortOrder>("date-desc");
  const [editTarget, setEditTarget] = useState<SavedRecipe | null>(null);
  const [facetFilters, setFacetFilters] = useState<Partial<Record<FacetKey, string[]>>>({});
  const [filterOpen, setFilterOpen] = useState(false);
  const [pendingTags, setPendingTags] = useState<RecipeTags>({});
  const inputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const sourceRef = useRef<HTMLInputElement>(null);
  const addPanelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setSavedRecipes(loadLocalSaved());
    fetchRemoteSaved().then((remote) => {
      if (remote.length === 0) return;
      setSavedRecipes((prev) => {
        const remoteByUrl = new Map(remote.map((r) => [r.url, r]));
        const localOnly = prev.filter((r) => !remoteByUrl.has(r.url));
        return [...remote, ...localOnly];
      });
    });
  }, []);

  // Close add-panel on outside click
  useEffect(() => {
    if (!addOpen) return;
    function onDown(e: MouseEvent) {
      if (addPanelRef.current && !addPanelRef.current.contains(e.target as Node)) {
        setAddOpen(false);
      }
    }
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [addOpen]);

  function handleTabChange(newTab: Tab) {
    setTab(newTab);
    setSourceManuallyEdited(false);
    setPendingSource("");
    if (phase === "error") {
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
    setPendingTags({});

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
      setAddOpen(false);
      setView("detail");
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
    setPendingTags({});

    try {
      const resized = await Promise.all(photoFiles.map((f) => resizeToBase64(f)));
      const res = await fetch("/api/recipe-from-images", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ images: resized }),
      });
      const data = (await res.json()) as RecipeOutput & { tags?: RecipeTags; error?: string };

      if (!res.ok) {
        setPhase("error");
        setErrorMsg(data.error ?? UI.recipes.errorFallback);
        return;
      }

      setRecipe(data);
      setPendingTags(data.tags ?? {});
      setActiveUrl(`${PHOTO_URL_PREFIX}${Date.now()}`);
      setPhase("done");
      setAddOpen(false);
      setView("detail");
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
    setPendingSource("");
    setSourceManuallyEdited(false);
    setPendingTags({});
    setView("library");
    setAddOpen(false);
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  async function handleSave() {
    if (!recipe || !activeUrl) return;
    setSaveError("");
    const item: SavedRecipe = {
      url: activeUrl,
      source: pendingSource.trim() || deriveDomain(activeUrl),
      recipe,
      savedAt: Date.now(),
      tags: pendingTags,
    };
    const updated = [item, ...savedRecipes.filter((r) => r.url !== activeUrl)];
    setSavedRecipes(updated);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    try {
      const remoteId = await saveRemote(item);
      setSavedRecipes((prev) => {
        const withId = prev.map((r) =>
          r.url === item.url ? { ...r, remoteId } : r,
        );
        localStorage.setItem(STORAGE_KEY, JSON.stringify(withId));
        return withId;
      });
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : "Opslaan mislukt");
    }
  }

  function handleRemoveSaved(savedUrl: string) {
    let remoteId: string | undefined;
    const updated = savedRecipes.filter((r) => {
      if (r.url === savedUrl) { remoteId = r.remoteId; return false; }
      return true;
    });
    setSavedRecipes(updated);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    if (remoteId) {
      fetch(`/api/recipes/${remoteId}`, { method: "DELETE" }).catch(() => {});
    }
  }

  function handleOpenRecipe(saved: SavedRecipe) {
    setRecipe(saved.recipe);
    setActiveUrl(saved.url);
    setUrl(saved.url.startsWith(PHOTO_URL_PREFIX) ? "" : saved.url);
    setPendingTags(saved.tags ?? {});
    setPhase("done");
    setErrorMsg("");
    setView("detail");
  }

  async function handleEditSave(updated: SavedRecipe) {
    const newList = savedRecipes.map(r => r.url === updated.url ? updated : r);
    setSavedRecipes(newList);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newList));
    if (activeUrl === updated.url) setRecipe(updated.recipe);
    setEditTarget(null);
    if (updated.remoteId) {
      const r = updated.recipe;
      try {
        await fetch(`/api/recipes/${updated.remoteId}`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: r.title,
            ingredients: flatIngredientTexts(r.ingredientSections),
            instructions: JSON.stringify(r),
            source: updated.source,
            tags: updated.tags ?? {},
            description: r.description ?? "",
            prep_time: r.prepTimeMinutes ?? null,
            cook_time: r.cookTimeMinutes ?? null,
            total_time: r.totalTimeMinutes ?? null,
            servings: r.servings ?? null,
            cuisine: toArray(r.cuisine),
            category: toArray(r.category),
            keywords: r.keywords ?? [],
            image_url: r.image ?? null,
            author: r.author ?? null,
            suitable_for_diet: r.suitableForDiet ?? [],
            date_published: r.datePublished ?? null,
            aggregate_rating: r.aggregateRating ?? null,
            video_url: r.video ?? null,
            nutrition: r.nutrition ?? {},
          }),
        });
      } catch { /* silent */ }
    }
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

  function handleDropzoneDrop(e: React.DragEvent) {
    e.preventDefault();
    setPhotoDrag(false);
    addFiles(e.dataTransfer.files);
  }

  const photoObjectUrls = useMemo(() => photoFiles.map((f) => URL.createObjectURL(f)), [photoFiles]);
  useEffect(() => {
    return () => { photoObjectUrls.forEach((u) => URL.revokeObjectURL(u)); };
  }, [photoObjectUrls]);

  const isSaved = savedRecipes.some((r) => r.url === activeUrl);

  const activeFilterCount = Object.values(facetFilters).filter(v => v && v.length > 0).length;

  const filteredRecipes = useMemo(() => {
    let list = searchQuery.trim()
      ? savedRecipes.filter(r => r.recipe.title.toLowerCase().includes(searchQuery.toLowerCase()))
      : [...savedRecipes];

    const activeFilters = Object.entries(facetFilters).filter(([, vals]) => vals && vals.length > 0) as [FacetKey, string[]][];
    if (activeFilters.length > 0) {
      list = list.filter(r => {
        if (!r.tags || Object.keys(r.tags).length === 0) return false;
        return activeFilters.every(([key, vals]) => {
          const recipeFacetVals = r.tags?.[key] ?? [];
          return vals.some(v => recipeFacetVals.includes(v));
        });
      });
    }

    if (sortOrder === "date-asc") list.sort((a, b) => a.savedAt - b.savedAt);
    else if (sortOrder === "alpha-asc") list.sort((a, b) => a.recipe.title.localeCompare(b.recipe.title, "nl"));
    else list.sort((a, b) => b.savedAt - a.savedAt);
    return list;
  }, [savedRecipes, searchQuery, sortOrder, facetFilters]);

  // ── Detail view ─────────────────────────────────────────────────────────────
  if (view === "detail" && recipe) {
    return (
      <div className="page rl-detail-page">
        {editTarget && (
          <EditRecipePanel
            target={editTarget}
            onSave={handleEditSave}
            onClose={() => setEditTarget(null)}
          />
        )}
        <nav className="rl-detail-nav">
          <button className="rl-back-btn" onClick={handleReset}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M10 3L5 8l5 5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Recepten
          </button>
          <div className="rl-detail-nav-actions">
            <button
              className={`rl-detail-save-btn${isSaved ? " rl-detail-save-btn--saved" : ""}`}
              onClick={handleSave}
              disabled={isSaved}
            >
              {isSaved ? (
                <>
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                    <path d="M2 7l3.5 3.5L12 3.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  {UI.recipes.savedBtn}
                </>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                    <path d="M7 2v10M2 7h10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
                  </svg>
                  {UI.recipes.saveBtn}
                </>
              )}
            </button>
            {saveError && <span className="rl-save-error">{saveError}</span>}
          </div>
        </nav>
        <RecipeDetail recipe={recipe} />
      </div>
    );
  }

  // ── Loading overlay (triggered from add panel) ────────────────────────────
  if (phase === "loading") {
    return (
      <div className="page rl-library-page">
        <div className="rl-loading-overlay" aria-live="polite">
          <div className="rl-loading-inner">
            <div className="rl-loading-spinner" aria-hidden="true" />
            {tab !== "photo" && <div className="rl-loading-bar" aria-hidden="true" />}
            <p className="rl-loading-msg">
              {tab === "photo" ? UI.recipes.photoLoadingMsg : UI.recipes.loadingMsg}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // ── Library view ─────────────────────────────────────────────────────────────
  return (
    <div className="page rl-library-page">
      {editTarget && (
        <EditRecipePanel
          target={editTarget}
          onSave={handleEditSave}
          onClose={() => setEditTarget(null)}
        />
      )}
      {/* Header */}
      <header className="rl-library-header">
        <div className="rl-library-header-text">
          <p className="rl-library-eyebrow">Mijn keuken</p>
          <h1 className="rl-library-title">
            Recepten<em>boek</em>
          </h1>
          {savedRecipes.length > 0 && (
            <p className="rl-library-count">
              {savedRecipes.length} {savedRecipes.length === 1 ? "recept" : "recepten"} bewaard
            </p>
          )}
        </div>

        {/* Search + Sort + Filter controls */}
        {savedRecipes.length > 1 && (
          <div className="rl-header-controls">
            {savedRecipes.length > 2 && (
              <div className="rl-search-wrap">
                <svg className="rl-search-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <circle cx="7" cy="7" r="4.5" stroke="currentColor" strokeWidth="1.5"/>
                  <path d="M10.5 10.5L13.5 13.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                </svg>
                <input
                  className="rl-search-input"
                  type="search"
                  placeholder="Recept zoeken…"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  aria-label="Recepten zoeken"
                />
                {searchQuery && (
                  <button
                    className="rl-search-clear-btn"
                    onClick={() => setSearchQuery("")}
                    aria-label={UI.recipes.searchClearAriaLabel}
                  >
                    <svg width="10" height="10" viewBox="0 0 10 10" fill="none" aria-hidden="true">
                      <path d="M1 1l8 8M9 1L1 9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
                    </svg>
                  </button>
                )}
              </div>
            )}
            <div className="rl-sort-wrap">
              <select
                className="rl-sort-select"
                value={sortOrder}
                onChange={e => setSortOrder(e.target.value as SortOrder)}
                aria-label={UI.recipes.sortLabel}
              >
                <option value="date-desc">{UI.recipes.sortDateDesc}</option>
                <option value="date-asc">{UI.recipes.sortDateAsc}</option>
                <option value="alpha-asc">{UI.recipes.sortAlpha}</option>
              </select>
            </div>
            <button
              className={`rl-filter-btn${activeFilterCount > 0 ? " rl-filter-btn--active" : ""}`}
              onClick={() => setFilterOpen(v => !v)}
              aria-expanded={filterOpen}
              type="button"
            >
              <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
                <path d="M2 3.5h9M4 6.5h5M6 9.5h1" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
              </svg>
              {UI.recipes.facetFilterLabel}
              {activeFilterCount > 0 && (
                <span className="rl-filter-btn-count">{activeFilterCount}</span>
              )}
            </button>
          </div>
        )}
      </header>

      {/* Facet filter bar */}
      {filterOpen && savedRecipes.length > 1 && (
        <FacetFilterBar
          filters={facetFilters}
          onChange={setFacetFilters}
        />
      )}

      {/* Error state */}
      {phase === "error" && (
        <div className="rl-error-banner" role="alert">
          <strong>{UI.recipes.errorTitle}:</strong> {errorMsg}
          <button className="rl-error-retry" onClick={handleReset}>{UI.recipes.retryBtn}</button>
        </div>
      )}

      {/* Search result count */}
      {searchQuery.trim() && savedRecipes.length > 0 && (
        <p className="rl-search-count">
          {filteredRecipes.length} {UI.recipes.searchResultOf} {savedRecipes.length} {UI.recipes.searchResultLabel}
        </p>
      )}

      {/* Empty state */}
      {savedRecipes.length === 0 && phase !== "error" && (
        <div className="rl-empty-state">
          <div className="rl-empty-illustration" aria-hidden="true" />
          <h2 className="rl-empty-title">Nog geen recepten</h2>
          <p className="rl-empty-sub">
            Voeg je eerste recept toe via een link of een foto van een kookboek.
          </p>
          <button className="rl-empty-cta" onClick={() => setAddOpen(true)}>
            Eerste recept toevoegen
          </button>
        </div>
      )}

      {/* Recipe grid */}
      {filteredRecipes.length > 0 && (
        <div className="rl-recipe-grid">
          {filteredRecipes.map((saved) => (
            <RecipeCard
              key={saved.url}
              saved={saved}
              onOpen={handleOpenRecipe}
              onRemove={handleRemoveSaved}
              onEdit={setEditTarget}
              accentColor={getAccentColor(saved)}
            />
          ))}
        </div>
      )}

      {/* No results for search or filter */}
      {filteredRecipes.length === 0 && savedRecipes.length > 0 && (searchQuery || activeFilterCount > 0) && (
        <div className="rl-no-results">
          <p>
            {searchQuery
              ? `Geen recepten gevonden voor "${searchQuery}"`
              : "Geen recepten gevonden met deze filters"}
          </p>
          <button className="rl-error-retry" onClick={() => { setSearchQuery(""); setFacetFilters({}); }}>
            Wis zoekopdracht en filters
          </button>
        </div>
      )}

      {/* Floating add button */}
      <div className="rl-fab-wrap" ref={addPanelRef}>
        {addOpen && (
          <div className="rl-add-panel">
            <div className="rl-add-panel-header">
              <p className="rl-add-panel-title">Recept toevoegen</p>
              <button className="rl-add-panel-close" onClick={() => setAddOpen(false)} aria-label="Sluiten">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
                </svg>
              </button>
            </div>

            <div className="rl-add-tabs">
              <button
                className={`rl-add-tab${tab === "url" ? " rl-add-tab--active" : ""}`}
                onClick={() => handleTabChange("url")}
              >
                {UI.recipes.tabUrl}
              </button>
              <button
                className={`rl-add-tab${tab === "photo" ? " rl-add-tab--active" : ""}`}
                onClick={() => handleTabChange("photo")}
              >
                {UI.recipes.tabPhoto}
              </button>
            </div>

            {tab === "url" && (
              <form onSubmit={handleSubmit} className="rl-add-form">
                <input
                  ref={inputRef}
                  type="url"
                  className="rl-add-input"
                  value={url}
                  onChange={(e) => {
                    setUrl(e.target.value);
                    if (!sourceManuallyEdited) {
                      const derived = deriveDomain(e.target.value);
                      if (derived && derived !== e.target.value) setPendingSource(derived);
                    }
                  }}
                  placeholder={UI.recipes.inputPlaceholder}
                  aria-label={UI.recipes.inputLabel}
                  required
                  autoFocus
                />
                <p className="rl-add-hint">{UI.recipes.hintText}</p>
                <div className="rl-add-field">
                  <label className="rl-add-label" htmlFor="url-source-input">
                    {UI.recipes.labelSource}
                  </label>
                  <input
                    ref={sourceRef}
                    id="url-source-input"
                    type="text"
                    className="rl-add-input"
                    value={pendingSource}
                    onChange={(e) => { setPendingSource(e.target.value); setSourceManuallyEdited(true); }}
                    placeholder={UI.recipes.placeholderSource}
                    aria-label={UI.recipes.labelSource}
                  />
                  <p className="rl-add-source-note">{UI.recipes.sourceHint}</p>
                </div>
                <button
                  type="submit"
                  className="rl-add-submit"
                  disabled={!url.trim()}
                >
                  {UI.recipes.submitBtn}
                </button>
              </form>
            )}

            {tab === "photo" && (
              <div className="rl-add-form">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  style={{ display: "none" }}
                  onChange={(e) => addFiles(e.target.files)}
                />

                <div
                  className={`rl-photo-zone${photoDrag ? " rl-photo-zone--drag" : ""}`}
                  onClick={() => fileInputRef.current?.click()}
                  onDragOver={(e) => { e.preventDefault(); setPhotoDrag(true); }}
                  onDragLeave={() => setPhotoDrag(false)}
                  onDrop={handleDropzoneDrop}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") fileInputRef.current?.click(); }}
                  aria-label={UI.recipes.photoDropzone}
                >
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" className="rl-photo-zone-icon" aria-hidden="true">
                    <rect x="2" y="5" width="20" height="16" rx="3" stroke="currentColor" strokeWidth="1.5"/>
                    <circle cx="8.5" cy="11.5" r="2" stroke="currentColor" strokeWidth="1.5"/>
                    <path d="M2 17l5-5 4 4 3-3 5 5" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round"/>
                    <path d="M15 2l3 3-3 3M18 5H12" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  <span className="rl-photo-zone-label">{UI.recipes.photoDropzone}</span>
                  <span className="rl-photo-zone-hint">{UI.recipes.photoHint}</span>
                </div>

                {photoFiles.length > 0 && (
                  <>
                    <p className="rl-photo-count">
                      {photoFiles.length} {UI.recipes.photoCountOf} {MAX_PHOTOS} {UI.recipes.photoCountLabel}
                    </p>
                    <ul className="recipe-photo-list" aria-label="Geselecteerde foto's">
                      {photoFiles.map((file, i) => (
                        <li key={photoObjectUrls[i]} className="recipe-photo-item">
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img src={photoObjectUrls[i]} alt={file.name} className="recipe-photo-thumb" />
                          <button
                            type="button"
                            className="recipe-photo-remove"
                            aria-label={`Verwijder ${file.name}`}
                            onClick={(e) => {
                              e.stopPropagation();
                              setPhotoFiles((prev) => prev.filter((_, j) => j !== i));
                            }}
                          >×</button>
                        </li>
                      ))}
                    </ul>
                  </>
                )}

                <div className="rl-add-field">
                  <label className="rl-add-label" htmlFor="photo-source-input">
                    {UI.recipes.labelSource}
                  </label>
                  <input
                    id="photo-source-input"
                    type="text"
                    className="rl-add-input"
                    value={pendingSource}
                    onChange={(e) => { setPendingSource(e.target.value); setSourceManuallyEdited(true); }}
                    placeholder={UI.recipes.placeholderSource}
                    aria-label={UI.recipes.labelSource}
                  />
                </div>

                <button
                  type="button"
                  className="rl-add-submit"
                  disabled={photoFiles.length === 0}
                  onClick={handlePhotoSubmit}
                >
                  {UI.recipes.photoSubmitBtn}
                </button>
              </div>
            )}
          </div>
        )}

        <button
          className={`rl-fab${addOpen ? " rl-fab--open" : ""}`}
          onClick={() => setAddOpen((v) => !v)}
          aria-label="Recept toevoegen"
          aria-expanded={addOpen}
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path
              d="M10 4v12M4 10h12"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              className="rl-fab-icon"
            />
          </svg>
        </button>
      </div>
    </div>
  );
}

// ─── Facet filter bar ──────────────────────────────────────────────────────────

function FacetFilterBar({
  filters,
  onChange,
}: {
  filters: Partial<Record<FacetKey, string[]>>;
  onChange: (filters: Partial<Record<FacetKey, string[]>>) => void;
}) {
  const hasActive = Object.values(filters).some(v => v && v.length > 0);

  function toggle(facetKey: FacetKey, value: string) {
    const current = filters[facetKey] ?? [];
    const next = current.includes(value)
      ? current.filter(v => v !== value)
      : [...current, value];
    onChange({ ...filters, [facetKey]: next });
  }

  return (
    <div className="rl-facet-bar">
      {FACETS.map(facet => (
        <div key={facet.key} className="rl-facet-group">
          <span className="rl-facet-group-label">{facet.nl}</span>
          <div className="rl-facet-pills">
            {facet.options.map(option => {
              const active = (filters[facet.key] ?? []).includes(option.value);
              return (
                <button
                  key={option.value}
                  className={`rl-facet-pill${active ? " rl-facet-pill--active" : ""}`}
                  onClick={() => toggle(facet.key, option.value)}
                  type="button"
                >
                  {option.nl}
                </button>
              );
            })}
          </div>
        </div>
      ))}
      {hasActive && (
        <button
          className="rl-facet-reset"
          onClick={() => onChange({})}
          type="button"
        >
          {UI.recipes.facetFilterReset}
        </button>
      )}
    </div>
  );
}

// ─── Recipe card ──────────────────────────────────────────────────────────────

function RecipeCard({
  saved,
  onOpen,
  onRemove,
  onEdit,
  accentColor,
}: {
  saved: SavedRecipe;
  onOpen: (saved: SavedRecipe) => void;
  onRemove: (url: string) => void;
  onEdit: (saved: SavedRecipe) => void;
  accentColor?: string;
}) {
  const [menuOpen, setMenuOpen] = useState(false);

  const isPhoto = saved.url.startsWith(PHOTO_URL_PREFIX);
  const domain = saved.source ?? deriveDomain(saved.url);

  const date = new Date(saved.savedAt).toLocaleDateString("nl-NL", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  const totalTime = saved.recipe.totalTimeMinutes ?? saved.recipe.cookTimeMinutes ?? null;

  // Prefer structured facet tags; fall back to Schema.org cuisine/category
  const displayTags: string[] = (() => {
    const splitAndTrim = (vals: string[]) =>
      vals.flatMap(v => v.split(",").map(s => s.trim())).filter(Boolean);

    if (saved.tags && Object.keys(saved.tags).length > 0) {
      const uniqueTags = [...new Set(splitAndTrim(Object.values(saved.tags).flat()))];
      return uniqueTags.slice(0, 3);
    }
    return splitAndTrim([...toArray(saved.recipe.cuisine), ...toArray(saved.recipe.category)]).slice(0, 3);
  })();

  return (
    <article
      className={`rl-card${accentColor ? " rl-card--accented" : ""}`}
      style={accentColor ? { borderTopColor: accentColor } : undefined}
    >
      <button className="rl-card-body" onClick={() => onOpen(saved)}>
        <div className="rl-card-top">
          <div className="rl-card-source-badge">
            {isPhoto ? (
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                <rect x="1" y="2" width="10" height="8" rx="1.5" stroke="currentColor" strokeWidth="1.2"/>
                <circle cx="4.5" cy="5.5" r="1.2" stroke="currentColor" strokeWidth="1.1"/>
                <path d="M1 9l2.5-2.5 2 2 1.5-1.5 2.5 2.5" stroke="currentColor" strokeWidth="1.1" strokeLinejoin="round"/>
              </svg>
            ) : (
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                <circle cx="6" cy="6" r="4.5" stroke="currentColor" strokeWidth="1.2"/>
                <path d="M6 1.5C6 1.5 4 4 4 6s2 4.5 2 4.5" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/>
                <path d="M6 1.5C6 1.5 8 4 8 6s-2 4.5-2 4.5M1.5 6h9" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/>
              </svg>
            )}
            {domain}
          </div>
          {totalTime && (
            <span className="rl-card-time">
              <svg width="11" height="11" viewBox="0 0 11 11" fill="none" aria-hidden="true">
                <circle cx="5.5" cy="5.5" r="4" stroke="currentColor" strokeWidth="1.2"/>
                <path d="M5.5 3v2.5l1.5 1.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
              </svg>
              {totalTime} {UI.recipes.labelMinutes}
            </span>
          )}
        </div>

        <h3 className="rl-card-title">{saved.recipe.title}</h3>

        {saved.recipe.description && (
          <p className="rl-card-desc">{saved.recipe.description}</p>
        )}

        <div className="rl-card-footer">
          <div className="rl-card-tags">
            {displayTags.map((tag) => (
              <span key={tag} className="rl-card-tag">{tag}</span>
            ))}
          </div>
          <span className="rl-card-date">{date}</span>
        </div>
      </button>

      <div className="rl-card-menu-wrap">
        <button
          className="rl-card-menu-btn"
          onClick={(e) => { e.stopPropagation(); setMenuOpen((v) => !v); }}
          aria-label="Opties"
          aria-expanded={menuOpen}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <circle cx="8" cy="4" r="1.2" fill="currentColor"/>
            <circle cx="8" cy="8" r="1.2" fill="currentColor"/>
            <circle cx="8" cy="12" r="1.2" fill="currentColor"/>
          </svg>
        </button>
        {menuOpen && (
          <div className="rl-card-menu">
            <button
              className="rl-card-menu-item"
              onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onEdit(saved); }}
            >
              <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
                <path d="M9 2l2 2-6 6H3V8l6-6z" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {UI.recipes.editBtn}
            </button>
            <button
              className="rl-card-menu-item rl-card-menu-item--danger"
              onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onRemove(saved.url); }}
            >
              <svg width="13" height="13" viewBox="0 0 13 13" fill="none" aria-hidden="true">
                <path d="M2 3.5h9M5 3.5V2.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 .5.5v1M11 3.5l-.75 7a.5.5 0 0 1-.5.5H3.25a.5.5 0 0 1-.5-.5L2 3.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {UI.recipes.savedRemoveLabel}
            </button>
          </div>
        )}
      </div>
    </article>
  );
}

// ─── Edit recipe panel ────────────────────────────────────────────────────────

function EditRecipePanel({
  target,
  onSave,
  onClose,
}: {
  target: SavedRecipe;
  onSave: (updated: SavedRecipe) => void;
  onClose: () => void;
}) {
  const [titleVal, setTitleVal] = useState(target.recipe.title);
  const [descVal, setDescVal] = useState(target.recipe.description ?? "");
  const [ingredientsVal, setIngredientsVal] = useState(
    flatIngredientTexts(target.recipe.ingredientSections).join("\n")
  );
  const [instructionsVal, setInstructionsVal] = useState(
    target.recipe.instructionSections.flatMap(s => s.steps.map(st => st.text)).join("\n")
  );
  const [sourceVal, setSourceVal] = useState(target.source || deriveDomain(target.url));
  const [tagsVal, setTagsVal] = useState<RecipeTags>(target.tags ?? {});

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  function toggleTag(facetKey: FacetKey, value: string) {
    const current = tagsVal[facetKey] ?? [];
    const next = current.includes(value)
      ? current.filter(v => v !== value)
      : [...current, value];
    setTagsVal(prev => ({ ...prev, [facetKey]: next }));
  }

  function handleSave() {
    const updatedRecipe: RecipeOutput = {
      ...target.recipe,
      title: titleVal.trim(),
      description: descVal.trim(),
      ingredientSections: [{ sectionName: UI.recipes.sectionIngredients, ingredients: ingredientsVal.split("\n").map(s => s.trim()).filter(Boolean).map(text => ({ text })) }],
      instructionSections: [{ sectionName: UI.recipes.sectionInstructions, steps: instructionsVal.split("\n").map(s => s.trim()).filter(Boolean).map((text, i) => ({ stepNumber: i + 1, text })) }],
    };
    const updated: SavedRecipe = {
      ...target,
      recipe: updatedRecipe,
      source: sourceVal.trim() || deriveDomain(target.url),
      tags: tagsVal,
    };
    onSave(updated);
  }

  return (
    <div className="rl-edit-scrim" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="rl-edit-panel">
        <div className="rl-edit-header">
          <h2 className="rl-edit-header-title">{UI.recipes.editTitle}</h2>
          <button className="rl-edit-close" onClick={onClose} aria-label={UI.detail.close}>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
            </svg>
          </button>
        </div>
        <div className="rl-edit-body">
          <div className="rl-edit-field">
            <label className="rl-edit-label">{UI.recipes.editLabelTitle}</label>
            <input
              type="text"
              className="rl-edit-input"
              value={titleVal}
              onChange={(e) => setTitleVal(e.target.value)}
            />
          </div>
          <div className="rl-edit-field">
            <label className="rl-edit-label">{UI.recipes.editLabelDesc}</label>
            <textarea
              className="rl-edit-textarea rl-edit-textarea--sm"
              value={descVal}
              onChange={(e) => setDescVal(e.target.value)}
            />
          </div>
          <div className="rl-edit-field">
            <label className="rl-edit-label">{UI.recipes.editLabelIngredients}</label>
            <textarea
              className="rl-edit-textarea rl-edit-textarea--lg"
              value={ingredientsVal}
              onChange={(e) => setIngredientsVal(e.target.value)}
            />
          </div>
          <div className="rl-edit-field">
            <label className="rl-edit-label">{UI.recipes.editLabelInstructions}</label>
            <textarea
              className="rl-edit-textarea rl-edit-textarea--lg"
              value={instructionsVal}
              onChange={(e) => setInstructionsVal(e.target.value)}
            />
          </div>
          <div className="rl-edit-field">
            <label className="rl-edit-label">{UI.recipes.editLabelSource}</label>
            <input
              type="text"
              className="rl-edit-input"
              value={sourceVal}
              onChange={(e) => setSourceVal(e.target.value)}
            />
          </div>
          <div className="rl-edit-field">
            <label className="rl-edit-label">{UI.recipes.editLabelTags}</label>
            <div className="rl-edit-facets">
              {FACETS.map(facet => (
                <div key={facet.key} className="rl-edit-facet-group">
                  <span className="rl-edit-facet-label">{facet.nl}</span>
                  <div className="rl-edit-facet-pills">
                    {facet.options.map(option => {
                      const active = (tagsVal[facet.key] ?? []).includes(option.value);
                      return (
                        <button
                          key={option.value}
                          type="button"
                          className={`rl-edit-facet-pill${active ? " rl-edit-facet-pill--active" : ""}`}
                          onClick={() => toggleTag(facet.key, option.value)}
                        >
                          {option.nl}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div className="rl-edit-footer">
          <button className="rl-edit-cancel-btn" onClick={onClose}>{UI.recipes.editCancelBtn}</button>
          <button className="rl-edit-save-btn" onClick={handleSave}>{UI.recipes.editSaveBtn}</button>
        </div>
      </div>
    </div>
  );
}

// ─── Serving size scaler ──────────────────────────────────────────────────────

const FRACTION_REPLACEMENTS: [RegExp, string][] = [
  [/½/g, "0.5"], [/¼/g, "0.25"], [/¾/g, "0.75"],
  [/⅓/g, "0.333"], [/⅔/g, "0.667"], [/⅛/g, "0.125"], [/⅜/g, "0.375"],
  [/⅝/g, "0.625"], [/⅞/g, "0.875"],
];

function scaleIngredient(text: string, ratio: number): string {
  if (ratio === 1) return text;
  let result = text;
  for (const [re, dec] of FRACTION_REPLACEMENTS) {
    result = result.replace(re, dec);
  }
  return result.replace(/([\d]+(?:[.,][\d]+)?)/g, (match) => {
    const num = parseFloat(match.replace(",", ".")) * ratio;
    if (num === Math.round(num)) return String(Math.round(num));
    return num.toFixed(1).replace(/\.0$/, "");
  });
}

function flatIngredientTexts(sections: IngredientSection[]): string[] {
  return sections.flatMap(s => s.ingredients.map(i => i.text));
}

function displayIngredient(item: IngredientItem, system: 'us' | 'metric', ratio: number): string {
  const text = system === 'metric' && item.textMetric ? item.textMetric : item.text;
  return scaleIngredient(text, ratio);
}

// ─── Recipe detail ────────────────────────────────────────────────────────────

function RecipeDetail({ recipe }: { recipe: RecipeOutput }) {
  const [checked, setChecked] = useState<Set<string>>(new Set());

  const baseServings = recipe.servings ?? 4;
  const [servings, setServings] = useState<number>(baseServings);
  const ratio = servings / baseServings;

  const [unitSystem, setUnitSystem] = useState<'us' | 'metric'>('us');

  const [cookingMode, setCookingMode] = useState(false);
  const [wakeLockUnsupported, setWakeLockUnsupported] = useState(false);

  useEffect(() => {
    if (!cookingMode) return;
    if (!('wakeLock' in navigator)) {
      setWakeLockUnsupported(true);
      setCookingMode(false);
      return;
    }
    let sentinel: WakeLockSentinel | null = null;
    navigator.wakeLock.request('screen')
      .then(s => {
        sentinel = s;
        s.addEventListener('release', () => setCookingMode(false));
      })
      .catch(() => {
        setWakeLockUnsupported(true);
        setCookingMode(false);
      });
    return () => { sentinel?.release(); };
  }, [cookingMode]);

  useEffect(() => {
    setServings(recipe.servings ?? 4);
    setUnitSystem('us');
  }, [recipe]);

  function toggleChecked(key: string) {
    setChecked(prev => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  }

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
  const keywords = recipe.keywords ?? [];
  const diets = recipe.suitableForDiet ?? [];
  const rating = recipe.aggregateRating;

  return (
    <article className="rl-detail">
      <header className="rl-detail-header">
        <div className="rl-detail-header-inner">
          {(cuisines.length > 0 || categories.length > 0) && (
            <p className="rl-detail-eyebrow">
              {[...cuisines, ...categories].join(" · ")}
            </p>
          )}
          <h1 className="rl-detail-title">{recipe.title}</h1>
          {recipe.description && (
            <p className="rl-detail-desc">{recipe.description}</p>
          )}
          {(recipe.author || rating || recipe.datePublished) && (
            <p className="rl-detail-author">
              {recipe.author && (
                <>{UI.recipes.labelAuthor} <span className="rl-detail-author-name">{recipe.author}</span></>
              )}
              {rating?.ratingValue != null && (
                <span className="rl-detail-rating">
                  {" "}· ★ {rating.ratingValue.toFixed(1)}{rating.ratingCount != null ? ` (${rating.ratingCount})` : ""}
                </span>
              )}
              {recipe.datePublished && (
                <span className="rl-detail-date-published">
                  {" "}· {recipe.datePublished.slice(0, 10)}
                </span>
              )}
            </p>
          )}
          {diets.length > 0 && (
            <div className="rl-detail-pills-row" aria-label={UI.recipes.labelSuitableForDiet}>
              {diets.map((d) => (
                <span key={d} className="rl-detail-pill rl-detail-pill--diet">{d}</span>
              ))}
            </div>
          )}
          {keywords.length > 0 && (
            <div className="rl-detail-pills-row" aria-label={UI.recipes.labelKeywords}>
              {keywords.map((k) => (
                <span key={k} className="rl-detail-pill">{k}</span>
              ))}
            </div>
          )}
          {hasMeta && (
            <div className="rl-detail-meta">
              {times.map((t) => (
                <div key={t.label} className="rl-detail-meta-item">
                  <span className="rl-detail-meta-label">{t.label}</span>
                  <span className="rl-detail-meta-value">{t.value} {UI.recipes.labelMinutes}</span>
                </div>
              ))}
            </div>
          )}
          <div className="rl-servings-row">
            <span className="rl-servings-label">{UI.recipes.labelServings}</span>
            <div className="rl-servings-controls">
              <button
                className="rl-servings-btn"
                onClick={() => setServings(s => Math.max(1, s - 1))}
                aria-label={UI.recipes.servingsDecrease}
                disabled={servings <= 1}
              >−</button>
              <span className="rl-servings-value">{servings}</span>
              <button
                className="rl-servings-btn"
                onClick={() => setServings(s => s + 1)}
                aria-label={UI.recipes.servingsIncrease}
              >+</button>
            </div>
          </div>

          {recipe.unitSystems?.hasMetric && (
            <div className="rl-unit-toggle-row">
              <span className="rl-unit-toggle-label">{UI.recipes.unitToggleLabel}</span>
              <div className="rl-unit-toggle">
                <button
                  type="button"
                  className={`rl-unit-btn${unitSystem === 'us' ? ' rl-unit-btn--active' : ''}`}
                  onClick={() => setUnitSystem('us')}
                >{UI.recipes.unitUs}</button>
                <button
                  type="button"
                  className={`rl-unit-btn${unitSystem === 'metric' ? ' rl-unit-btn--active' : ''}`}
                  onClick={() => setUnitSystem('metric')}
                >{UI.recipes.unitMetric}</button>
              </div>
            </div>
          )}

          <div className="rl-cooking-mode-row">
            <span className="rl-cooking-mode-text">
              {UI.recipes.cookingMode}
              {cookingMode && (
                <span className="rl-cooking-mode-active-label">{UI.recipes.cookingModeActive}</span>
              )}
              {wakeLockUnsupported && (
                <span className="rl-cooking-mode-unsupported">{UI.recipes.cookingModeUnsupported}</span>
              )}
            </span>
            <button
              role="switch"
              aria-checked={cookingMode}
              className={`rl-cooking-mode-switch${cookingMode ? " rl-cooking-mode-switch--on" : ""}`}
              onClick={() => { setWakeLockUnsupported(false); setCookingMode(v => !v); }}
              aria-label={UI.recipes.cookingMode}
            >
              <span className="rl-cooking-mode-thumb" />
            </button>
          </div>
        </div>
      </header>

      <div className="rl-detail-body">
        <aside className="rl-detail-ing">
          <h2 className="rl-detail-col-title">{UI.recipes.sectionIngredients}</h2>
          {recipe.ingredientSections.map((section, si) => (
            <div key={si} className="rl-detail-section">
              {!(section.sectionName === UI.recipes.sectionIngredients && recipe.ingredientSections.length === 1) && (
                <p className="rl-detail-section-name">{section.sectionName}</p>
              )}
              <ul className="rl-ing-list">
                {section.ingredients.map((item, ii) => {
                  const key = `${si}-${ii}`;
                  const isChecked = checked.has(key);
                  const scaledIng = displayIngredient(item, unitSystem, ratio);
                  return (
                    <li key={ii} className={`rl-ing-item${isChecked ? " rl-ing-item--checked" : ""}`}>
                      <label className="rl-ing-label">
                        <input
                          type="checkbox"
                          className="rl-ing-checkbox-input"
                          checked={isChecked}
                          onChange={() => toggleChecked(key)}
                          aria-label={scaledIng}
                        />
                        <span className="rl-ing-check-box" aria-hidden="true" />
                        <span className="rl-ing-text">{scaledIng}</span>
                      </label>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </aside>

        <div className="rl-detail-steps">
          <h2 className="rl-detail-col-title">{UI.recipes.sectionInstructions}</h2>
          {recipe.instructionSections.map((section, si) => (
            <div key={si} className="rl-detail-section">
              {!(section.sectionName === UI.recipes.sectionInstructions && recipe.instructionSections.length === 1) && (
                <p className="rl-detail-section-name">{section.sectionName}</p>
              )}
              {section.steps.map((step) => (
                <div key={step.stepNumber} className="rl-step">
                  <span className="rl-step-num" aria-hidden="true">{step.stepNumber}</span>
                  <p className="rl-step-text">{step.text}</p>
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>

      {hasNutrition && (
        <footer className="rl-detail-nutrition">
          <p className="rl-detail-col-title">{UI.recipes.sectionNutrition}</p>
          <div className="rl-nutrition-row">
            {recipe.nutrition.calories && <NutritionChip label={UI.recipes.labelCalories} value={recipe.nutrition.calories} />}
            {recipe.nutrition.protein && <NutritionChip label={UI.recipes.labelProtein} value={recipe.nutrition.protein} />}
            {recipe.nutrition.carbs && <NutritionChip label={UI.recipes.labelCarbs} value={recipe.nutrition.carbs} />}
            {recipe.nutrition.fat && <NutritionChip label={UI.recipes.labelFat} value={recipe.nutrition.fat} />}
          </div>
        </footer>
      )}
      {recipe.video && (
        <footer className="rl-detail-video">
          <a
            href={recipe.video}
            target="_blank"
            rel="noopener noreferrer"
            className="rl-detail-video-link"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
              <rect x="1" y="2.5" width="8" height="9" rx="1.2" stroke="currentColor" strokeWidth="1.3"/>
              <path d="M9 5.5l3.5-2v7L9 8.5" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round"/>
            </svg>
            {UI.recipes.labelVideo}
          </a>
        </footer>
      )}
    </article>
  );
}

function NutritionChip({ label, value }: { label: string; value: string }) {
  return (
    <span className="rl-nutrition-chip">
      <span className="rl-nutrition-label">{label}</span>
      <span className="rl-nutrition-value">{value}</span>
    </span>
  );
}

function toArray(v: string | string[]): string[] {
  if (!v) return [];
  return Array.isArray(v) ? v.filter(Boolean) : v ? [v] : [];
}
