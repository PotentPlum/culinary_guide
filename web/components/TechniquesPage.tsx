"use client";

import { useState, useMemo, useCallback, type ReactNode } from "react";
import { TECHNIQUES, type Technique, type TechniqueCategory } from "@/lib/techniques";
import { UI } from "@/lib/strings";
import TechniqueCard from "./TechniqueCard";
import TechniqueDetail from "./TechniqueDetail";

const PRIMARY_CATEGORIES: Array<{ id: TechniqueCategory; label: string; icon: ReactNode }> = [
  {
    id: "snijden",
    label: UI.techniques.categorySnijden,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 21L17 7M17 7l2-2a2 2 0 112.8 2.8L3 21"/><path d="M7.5 17.5l-3 3"/>
      </svg>
    ),
  },
  {
    id: "garing",
    label: UI.techniques.categoryGaring,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 2c0 5-4 7-4 11a4 4 0 008 0c0-4-4-6-4-11z"/><path d="M9 17.5a2.5 2.5 0 005 0"/>
      </svg>
    ),
  },
  {
    id: "smaak",
    label: UI.techniques.categorySmaak,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <polygon points="12,2 15,9 22,9 16.5,14 18.5,21 12,17 5.5,21 7.5,14 2,9 9,9"/>
      </svg>
    ),
  },
  {
    id: "eiwitten",
    label: UI.techniques.categoryEiwitten,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <ellipse cx="12" cy="13.5" rx="6.5" ry="7.5"/><path d="M8.5 8.5C8.5 6 9.5 3 12 3s3.5 3 3.5 5.5"/>
      </svg>
    ),
  },
  {
    id: "sauzen",
    label: UI.techniques.categorySauzen,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 3l4 9H8l4-9z"/><path d="M7 13v1a5 5 0 0010 0v-1"/>
      </svg>
    ),
  },
  {
    id: "groenten",
    label: UI.techniques.categoryGroenten,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 22V12"/><path d="M12 12c0 0-5-2.5-6-8 4.5 0 6 4 6 8z"/><path d="M12 12c0 0 5-2.5 6-8-4.5 0-6 4-6 8z"/>
      </svg>
    ),
  },
  {
    id: "gereedschap",
    label: UI.techniques.categoryGereedschap,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3-3a6 6 0 01-7.9 7.9L6.4 20.7a2.1 2.1 0 01-3-3l6.8-6.8A6 6 0 0114.7 6.3z"/>
      </svg>
    ),
  },
];

const CHEVRON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9 18l6-6-6-6"/>
  </svg>
);

const ARROW_BACK = (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.25" strokeLinecap="round" strokeLinejoin="round">
    <path d="M15 18l-6-6 6-6"/>
  </svg>
);

const SEARCH_ICON = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" />
  </svg>
);

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

  const currentCategoryLabel = viewState.level !== "category"
    ? PRIMARY_CATEGORIES.find((c) => c.id === viewState.category)?.label
    : null;

  return (
    <div className="page">
      <header className="page-hero">
        <div className="hero-eyebrow">
          <span className="hero-loc">{UI.techniques.pageTitle}</span>
          <span className="hero-dot">·</span>
          <span>Kookmethoden</span>
        </div>
        <h1 className="hero-title">Leer de <em>techniek</em></h1>
        <p className="hero-sub">{UI.techniques.pageSubtitle}</p>
      </header>

      {!showCategoryView && (
        <nav className="technique-breadcrumb">
          <button
            className="breadcrumb-back"
            onClick={() => {
              if (showTechniquesView) {
                setViewState({ level: "subcategory", category: viewState.category });
              } else {
                setViewState({ level: "category" });
              }
              setQuery("");
            }}
          >
            {ARROW_BACK}
            {showSubcategoryView ? UI.techniques.pageTitle : currentCategoryLabel}
          </button>
          {showTechniquesView && (
            <>
              <span className="breadcrumb-sep">/</span>
              <span className="breadcrumb-current">{viewState.subcategory}</span>
            </>
          )}
        </nav>
      )}

      {showCategoryView && (
        <div className="category-list">
          {PRIMARY_CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              className="category-item"
              onClick={() => setViewState({ level: "subcategory", category: cat.id })}
            >
              <div className="category-item-icon">{cat.icon}</div>
              <div className="category-item-body">
                <span className="category-name">{cat.label}</span>
                <span className="category-count">{getCategoryCount(cat.id)}</span>
              </div>
              <span className="category-item-arrow">{CHEVRON}</span>
            </button>
          ))}
        </div>
      )}

      {showSubcategoryView && (
        <div className="subcategory-list">
          {getSubcategories(viewState.category).map((sub) => (
            <button
              key={sub}
              className="subcategory-item"
              onClick={() => setViewState({ level: "techniques", category: viewState.category, subcategory: sub })}
            >
              <div className="subcategory-item-accent" />
              <div className="category-item-body">
                <span className="subcategory-name">{sub}</span>
                <span className="subcategory-count">{getSubcategoryCount(viewState.category, sub)}</span>
              </div>
              <span className="subcategory-item-arrow">{CHEVRON}</span>
            </button>
          ))}
        </div>
      )}

      {showTechniquesView && (
        <>
          <div className="technique-controls">
            <div className="search">
              {SEARCH_ICON}
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
