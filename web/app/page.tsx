"use client";

import { useState, useMemo } from "react";
import { VEG_DATA, statusFor, MONTHS_LONG, type VegItem, type VegStatus } from "@/lib/vegetables";
import { UI } from "@/lib/strings";
import NavIcon from "@/components/NavIcon";
import VegCard from "@/components/VegCard";
import FilterPills, { type Filter, type Counts } from "@/components/FilterPills";
import FuturePeek from "@/components/FuturePeek";
import CalendarPage from "@/components/CalendarPage";
import DetailPanel from "@/components/DetailPanel";
import RecipesPage from "@/components/RecipesPage";

type Page = "seasonal" | "calendar" | "recipes";
type View = "grid" | "list";
type Density = "cozy" | "comfortable" | "compact";

const NAV_ITEMS = [
  { id: "seasonal",      label: UI.nav.seasonal,      icon: "leaf",    ready: true },
  { id: "calendar",      label: UI.nav.calendar,      icon: "cal",     ready: true },
  { id: "recipes",       label: UI.nav.recipes,       icon: "book",    ready: true },
  { id: "substitutions", label: UI.nav.substitutions, icon: "swap",    ready: false },
  { id: "techniques",    label: UI.nav.techniques,    icon: "knife",   ready: false },
  { id: "foraging",      label: UI.nav.foraging,      icon: "compass", ready: false },
];

const STATUS_ORDER: Record<VegStatus, number> = { peak: 0, shoulder: 1, soon: 2, ending: 3, off: 4 };

export default function App() {
  const month = new Date().getMonth() + 1;
  const [page, setPage] = useState<Page>("seasonal");
  const [filter, setFilter] = useState<Filter>("all");
  const [query, setQuery] = useState("");
  const [view, setView] = useState<View>("grid");
  // density toggle UI is not yet built — state is wired through but fixed at "comfortable"
  const [density] = useState<Density>("comfortable");
  const [openVeg, setOpenVeg] = useState<VegItem | null>(null);

  const all = useMemo(
    () => VEG_DATA.map((v) => ({ ...v, _status: statusFor(v, month) })),
    [month]
  );
  const inSeason = useMemo(() => all.filter((v) => v._status !== "off"), [all]);

  const counts = useMemo<Counts>(() => ({
    all:      inSeason.length,
    peak:     inSeason.filter((v) => v._status === "peak").length,
    shoulder: inSeason.filter((v) => v._status === "shoulder").length,
    soon:     inSeason.filter((v) => v._status === "soon").length,
    ending:   inSeason.filter((v) => v._status === "ending").length,
  }), [inSeason]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return inSeason
      .filter((v) => {
        const matchesFilter = filter === "all" ? true : v._status === filter;
        const matchesQuery = !q || v.name.toLowerCase().includes(q) || v.nl.toLowerCase().includes(q) || v.family.toLowerCase().includes(q);
        return matchesFilter && matchesQuery;
      })
      .sort((a, b) => STATUS_ORDER[a._status] - STATUS_ORDER[b._status] || a.name.localeCompare(b.name));
  }, [inSeason, filter, query]);

  return (
    <div className="app">
      {/* Sidebar */}
      <aside className="sidenav">
        <div className="brand">
          <div className="brand-mark">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
              <path d="M5 19c0-8 6-14 14-14 0 8-6 14-14 14Z" />
              <path d="M5 19l8-8" />
            </svg>
          </div>
          <div className="brand-name">
            <div className="brand-title">{UI.brandTitle}</div>
            <div className="brand-sub">{UI.brandSub}</div>
          </div>
        </div>

        <nav className="nav">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              className={`nav-item${page === item.id ? " active" : ""}${!item.ready ? " soon" : ""}`}
              onClick={() => item.ready && setPage(item.id as Page)}
              disabled={!item.ready}
              title={item.ready ? item.label : `${item.label} — ${UI.nav.soon}`}
            >
              <NavIcon name={item.icon} />
              <span>{item.label}</span>
              {!item.ready && <span className="nav-soon">{UI.nav.soon}</span>}
            </button>
          ))}
        </nav>

        <div className="sidenav-footer">
          <div className="footer-loc">
            <span className="footer-flag" />
            <div>
              <div className="footer-loc-label">{UI.footer.regionLabel}</div>
              <div className="footer-loc-name">{UI.footer.regionName}</div>
            </div>
          </div>
          <div className="footer-build">{UI.footer.build}</div>
        </div>
      </aside>

      {/* Main content */}
      <main className="main">
        {page === "seasonal" && (
          <div className="page seasonal">
            <header className="page-hero">
              <div className="hero-eyebrow">
                <span className="hero-loc">{UI.hero.eyebrowLoc}</span>
                <span className="hero-dot">·</span>
                <span>{MONTHS_LONG[month - 1]} {new Date().getFullYear()}</span>
              </div>
              <h1 className="hero-title">
                {UI.hero.title} <em>{UI.hero.titleEm}</em>
              </h1>
              <p className="hero-sub">
                {counts.peak} {UI.hero.statPeak} · {counts.shoulder} {UI.hero.statShoulder} ·{" "}
                {counts.soon} {UI.hero.statSoon} · {counts.ending} {UI.hero.statEnding}
              </p>
            </header>

            <div className="controls">
              <FilterPills value={filter} onChange={setFilter} counts={counts} />
              <div className="control-right">
                <div className="search">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" />
                  </svg>
                  <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder={UI.controls.searchPlaceholder}
                    aria-label={UI.controls.searchAriaLabel}
                  />
                </div>
                <div className="seg">
                  <button className={view === "grid" ? "on" : ""} onClick={() => setView("grid")} aria-label={UI.controls.gridAriaLabel}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="1"/><rect x="13" y="3" width="8" height="8" rx="1"/><rect x="3" y="13" width="8" height="8" rx="1"/><rect x="13" y="13" width="8" height="8" rx="1"/></svg>
                  </button>
                  <button className={view === "list" ? "on" : ""} onClick={() => setView("list")} aria-label={UI.controls.listAriaLabel}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
                  </button>
                </div>
              </div>
            </div>

            {filtered.length === 0 ? (
              <div className="empty">{UI.emptyState}</div>
            ) : view === "list" ? (
              <div className="veg-list">
                {filtered.map((v) => (
                  <VegCard key={v.id} veg={v} month={month} density={density} onOpen={setOpenVeg} listMode />
                ))}
              </div>
            ) : (
              <div className={`veg-grid grid-${density}`}>
                {filtered.map((v) => (
                  <VegCard key={v.id} veg={v} month={month} density={density} onOpen={setOpenVeg} />
                ))}
              </div>
            )}

            <FuturePeek />
          </div>
        )}

        {page === "calendar" && (
          <CalendarPage month={month} onOpen={setOpenVeg} />
        )}

        {page === "recipes" && <RecipesPage />}
      </main>

      <DetailPanel veg={openVeg} month={month} onClose={() => setOpenVeg(null)} />
    </div>
  );
}
