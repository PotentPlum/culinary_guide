"use client";

import { useState, useMemo } from "react";
import { VEG_DATA, statusFor, MONTHS_LONG, type VegItem, type VegStatus } from "@/lib/vegetables";
import { UI } from "@/lib/strings";
import VegCard from "@/components/VegCard";
import FilterPills, { type Filter, type Counts } from "@/components/FilterPills";
import FuturePeek from "@/components/FuturePeek";

type View = "grid" | "list";

const STATUS_ORDER: Record<VegStatus, number> = { peak: 0, shoulder: 1, soon: 2, ending: 3, off: 4 };

const STAT_CHIPS = [
  { key: "peak"     as const, cls: "hero-stat--peak",     label: UI.hero.statPeak },
  { key: "shoulder" as const, cls: "hero-stat--shoulder",  label: UI.hero.statShoulder },
  { key: "soon"     as const, cls: "hero-stat--soon",      label: UI.hero.statSoon },
  { key: "ending"   as const, cls: "hero-stat--ending",    label: UI.hero.statEnding },
];

const GRID_ICON = (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <rect x="3" y="3" width="8" height="8" rx="1" /><rect x="13" y="3" width="8" height="8" rx="1" />
    <rect x="3" y="13" width="8" height="8" rx="1" /><rect x="13" y="13" width="8" height="8" rx="1" />
  </svg>
);
const LIST_ICON = (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
  </svg>
);

const VIEW_OPTIONS: { key: View; ariaLabel: string; icon: React.ReactNode }[] = [
  { key: "grid", ariaLabel: UI.controls.gridAriaLabel, icon: GRID_ICON },
  { key: "list", ariaLabel: UI.controls.listAriaLabel, icon: LIST_ICON },
];

type Props = {
  month: number;
  onOpen: (veg: VegItem) => void;
};

export default function SeasonalPage({ month, onOpen }: Props) {
  const [filter, setFilter] = useState<Filter>("all");
  const [query, setQuery] = useState("");
  const [view, setView] = useState<View>("grid");

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
        const matchesFilter = filter === "all" || v._status === filter;
        const matchesQuery = !q || v.name.toLowerCase().includes(q) || v.nl.toLowerCase().includes(q) || v.family.toLowerCase().includes(q);
        return matchesFilter && matchesQuery;
      })
      .sort((a, b) => STATUS_ORDER[a._status] - STATUS_ORDER[b._status] || a.name.localeCompare(b.name));
  }, [inSeason, filter, query]);

  return (
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
        <p className="hero-sub">{UI.hero.heroSub}</p>
        <div className="hero-stats">
          {STAT_CHIPS.map(({ key, cls, label }) => (
            <div key={key} className={`hero-stat ${cls}`}>
              <span className="hero-stat-num">{counts[key]}</span>
              {label}
            </div>
          ))}
        </div>
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
          <div className="seg" role="group" aria-label={UI.controls.viewToggleAriaLabel}>
            {VIEW_OPTIONS.map(({ key, ariaLabel, icon }) => (
              <button
                key={key}
                className={view === key ? "on" : ""}
                onClick={() => setView(key)}
                aria-label={ariaLabel}
                aria-pressed={view === key}
              >
                {icon}
              </button>
            ))}
          </div>
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="empty">{UI.emptyState}</div>
      ) : view === "list" ? (
        <div className="veg-list">
          {filtered.map((v) => (
            <VegCard key={v.id} veg={v} month={month} density="comfortable" onOpen={onOpen} listMode />
          ))}
        </div>
      ) : (
        <div className="veg-grid grid-comfortable">
          {filtered.map((v) => (
            <VegCard key={v.id} veg={v} month={month} density="comfortable" onOpen={onOpen} />
          ))}
        </div>
      )}

      <FuturePeek />
    </div>
  );
}
