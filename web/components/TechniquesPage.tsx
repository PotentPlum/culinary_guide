"use client";

import { useState, useMemo, useCallback } from "react";
import { TECHNIQUES, type Technique, type TechniqueCategory } from "@/lib/techniques";
import { UI } from "@/lib/strings";
import TechniqueCard from "./TechniqueCard";
import TechniqueDetail from "./TechniqueDetail";

type CategoryFilter = TechniqueCategory | "all";

const CATEGORY_OPTIONS: { id: CategoryFilter; label: string }[] = [
  { id: "all",      label: UI.techniques.filterAll },
  { id: "methoden", label: UI.techniques.categoryMethoden },
  { id: "eiwitten", label: UI.techniques.categoryEiwitten },
  { id: "groenten", label: UI.techniques.categoryGroenten },
  { id: "sauzen",   label: UI.techniques.categorySauzen },
];

const COUNTS: Record<CategoryFilter, number> = { all: TECHNIQUES.length, methoden: 0, eiwitten: 0, groenten: 0, sauzen: 0 };
for (const t of TECHNIQUES) COUNTS[t.category]++;

export default function TechniquesPage() {
  const [activeCategory, setActiveCategory] = useState<CategoryFilter>("all");
  const [query, setQuery] = useState("");
  const [openTechnique, setOpenTechnique] = useState<Technique | null>(null);

  const handleClose = useCallback(() => setOpenTechnique(null), []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return TECHNIQUES.filter((t) => {
      const matchesCategory = activeCategory === "all" || t.category === activeCategory;
      const matchesQuery = !q ||
        t.nl.toLowerCase().includes(q) ||
        t.subtitle.toLowerCase().includes(q) ||
        (t.subcategory?.toLowerCase().includes(q) ?? false);
      return matchesCategory && matchesQuery;
    });
  }, [activeCategory, query]);

  return (
    <div className="page">
      <header className="page-hero">
        <h1 className="hero-title">{UI.techniques.pageTitle}</h1>
        <p className="hero-sub">{UI.techniques.pageSubtitle}</p>
      </header>

      <div className="technique-controls">
        <div className="filter-pills" role="tablist">
          {CATEGORY_OPTIONS.map((opt) => (
            <button
              key={opt.id}
              role="tab"
              aria-selected={activeCategory === opt.id}
              className={`pill${activeCategory === opt.id ? " active" : ""}`}
              onClick={() => setActiveCategory(opt.id)}
            >
              <span>{opt.label}</span>
              <span className="pill-count">{COUNTS[opt.id]}</span>
            </button>
          ))}
        </div>

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

      <TechniqueDetail technique={openTechnique} onClose={handleClose} onOpen={setOpenTechnique} />
    </div>
  );
}
