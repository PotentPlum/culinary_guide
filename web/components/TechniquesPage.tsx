"use client";

import { useState, useMemo, useCallback } from "react";
import { TECHNIQUES, type Technique, type TechniqueCategory } from "@/lib/techniques";
import { UI } from "@/lib/strings";
import TechniqueCard from "./TechniqueCard";
import TechniqueDetail from "./TechniqueDetail";

const PRIMARY_CATEGORIES: Array<{ id: TechniqueCategory; label: string }> = [
  { id: "snijden", label: UI.techniques.categorySnijden },
  { id: "garing", label: UI.techniques.categoryGaring },
  { id: "smaak", label: UI.techniques.categorySmaak },
  { id: "eiwitten", label: UI.techniques.categoryEiwitten },
  { id: "sauzen", label: UI.techniques.categorySauzen },
  { id: "groenten", label: UI.techniques.categoryGroenten },
  { id: "gereedschap", label: UI.techniques.categoryGereedschap },
];

type ViewState =
  | { level: "category" }
  | { level: "subcategory"; category: TechniqueCategory }
  | { level: "techniques"; category: TechniqueCategory; subcategory: string };

export default function TechniquesPage() {
  const [viewState, setViewState] = useState<ViewState>({ level: "category" });
  const [query, setQuery] = useState("");
  const [openTechnique, setOpenTechnique] = useState<Technique | null>(null);

  const handleClose = useCallback(() => setOpenTechnique(null), []);

  const getSubcategories = useCallback((category: TechniqueCategory): string[] => {
    const subs = new Set<string>();
    TECHNIQUES.filter((t) => t.category === category).forEach((t) => {
      if (t.subcategory) subs.add(t.subcategory);
    });
    return Array.from(subs).sort();
  }, []);

  const getSubcategoryCount = useCallback((category: TechniqueCategory, subcategory: string): number => {
    return TECHNIQUES.filter((t) => t.category === category && t.subcategory === subcategory).length;
  }, []);

  const getCategoryCount = useCallback((category: TechniqueCategory): number => {
    return TECHNIQUES.filter((t) => t.category === category).length;
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (viewState.level === "category") return [];

    let base = TECHNIQUES.filter((t) => t.category === viewState.category);
    if (viewState.level === "techniques") {
      base = base.filter((t) => t.subcategory === viewState.subcategory);
    }

    if (!q) return base;
    return base.filter((t) =>
      t.nl.toLowerCase().includes(q) ||
      t.subtitle.toLowerCase().includes(q) ||
      (t.subcategory?.toLowerCase().includes(q) ?? false)
    );
  }, [viewState, query]);

  const showCategoryView = viewState.level === "category";
  const showSubcategoryView = viewState.level === "subcategory";
  const showTechniquesView = viewState.level === "techniques";

  return (
    <div className="page">
      <header className="page-hero">
        <h1 className="hero-title">{UI.techniques.pageTitle}</h1>
        <p className="hero-sub">{UI.techniques.pageSubtitle}</p>
      </header>

      {!showCategoryView && (
        <div className="technique-breadcrumb">
          <button className="breadcrumb-back" onClick={() => {
            if (showTechniquesView) {
              setViewState({ level: "subcategory", category: viewState.category });
            } else {
              setViewState({ level: "category" });
            }
            setQuery("");
          }}>
            ← {UI.techniques.backButton}
          </button>
        </div>
      )}

      {showCategoryView && (
        <div className="technique-controls">
          <div className="category-grid">
            {PRIMARY_CATEGORIES.map((cat) => (
              <button
                key={cat.id}
                className="category-button"
                onClick={() => setViewState({ level: "subcategory", category: cat.id })}
              >
                <span className="category-name">{cat.label}</span>
                <span className="category-count">{getCategoryCount(cat.id)}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {showSubcategoryView && (
        <div className="technique-controls">
          <div className="subcategory-grid">
            {getSubcategories(viewState.category).map((sub) => (
              <button
                key={sub}
                className="subcategory-button"
                onClick={() => setViewState({ level: "techniques", category: viewState.category, subcategory: sub })}
              >
                <span className="subcategory-name">{sub}</span>
                <span className="subcategory-count">{getSubcategoryCount(viewState.category, sub)}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {showTechniquesView && (
        <>
          <div className="technique-controls">
            <div className="search">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" />
              </svg>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={UI.techniques.searchPlaceholder}
                aria-label={UI.techniques.searchAriaLabel}
              />
            </div>
          </div>

          {filtered.length === 0 ? (
            <div className="empty">{UI.techniques.emptyState}</div>
          ) : (
            <div className="technique-grid">
              {filtered.map((t) => (
                <TechniqueCard key={t.id} technique={t} onOpen={setOpenTechnique} />
              ))}
            </div>
          )}
        </>
      )}

      <TechniqueDetail technique={openTechnique} onClose={handleClose} onOpen={setOpenTechnique} />
    </div>
  );
}
