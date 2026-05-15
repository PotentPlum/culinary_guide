"use client";

import { useState, useMemo } from "react";
import { VEG_DATA, statusFor, MONTHS_LONG, type VegItem, type VegStatus } from "@/lib/vegetables";
import NavIcon from "@/components/NavIcon";
import VegCard from "@/components/VegCard";
import FilterPills, { type Filter, type Counts } from "@/components/FilterPills";
import FuturePeek from "@/components/FuturePeek";
import CalendarPage from "@/components/CalendarPage";
import DetailPanel from "@/components/DetailPanel";

type Page = "seasonal" | "calendar";
type View = "grid" | "list";
type Density = "cozy" | "comfortable" | "compact";

/** VegItem with its computed seasonal status for the current month attached. */
type VegWithStatus = VegItem & { _status: VegStatus };

const NAV_ITEMS = [
  { id: "seasonal",      label: "Seasonal",      icon: "leaf",    ready: true },
  { id: "calendar",      label: "Calendar",      icon: "cal",     ready: true },
  { id: "recipes",       label: "Recipes",       icon: "book",    ready: false },
  { id: "substitutions", label: "Substitutions", icon: "swap",    ready: false },
  { id: "techniques",    label: "Techniques",    icon: "knife",   ready: false },
  { id: "foraging",      label: "Foraging",      icon: "compass", ready: false },
];

/** Lower number = shown earlier in the seasonal grid. */
const STATUS_ORDER: Record<VegStatus, number> = { peak: 0, shoulder: 1, soon: 2, ending: 3, off: 4 };

export default function App() {
  const month = new Date().getMonth() + 1;
  const [page, setPage] = useState<Page>("seasonal");
  const [filter, setFilter] = useState<Filter>("all");
  const [query, setQuery] = useState("");
  const [view, setView] = useState<View>("grid");
  /** Density is a planned UI preference feature; the setter is unused until that ships. */
  const [density] = useState<Density>("comfortable");
  const [openVeg, setOpenVeg] = useState<VegItem | null>(null);

  /** All vegetables with their seasonal status for this month. */
  const all = useMemo<VegWithStatus[]>(
    () => VEG_DATA.map((v) => ({ ...v, _status: statusFor(v, month) })),
    [month]
  );

  /** Subset that is currently reachable (peak, shoulder, soon, or ending — not fully off). */
  const inSeason = useMemo(
    () => all.filter((v) => v._status !== "off"),
    [all]
  );

  /**
   * Badge counts for the filter pills.
   * "All" reflects the full dataset; the status-specific counts cover in-season only.
   */
  const counts = useMemo<Counts>(() => ({
    all:      all.length,
    peak:     inSeason.filter((v) => v._status === "peak").length,
    shoulder: inSeason.filter((v) => v._status === "shoulder").length,
    soon:     inSeason.filter((v) => v._status === "soon").length,
    ending:   inSeason.filter((v) => v._status === "ending").length,
  }), [all, inSeason]);

  /**
   * "All" searches and shows the entire dataset so out-of-season vegetables are
   * still reachable (they render in a dimmed section below the in-season grid).
   * Status-specific filters (peak / shoulder / soon / ending) operate within
   * the in-season pool only.
   */
  const filtered = useMemo<VegWithStatus[]>(() => {
    const q = query.trim().toLowerCase();
    const source = filter === "all" ? all : inSeason;
    return source
      .filter((v) => {
        if (filter !== "all" && v._status !== filter) return false;
        return !q || v.name.toLowerCase().includes(q) || v.nl.toLowerCase().includes(q) || v.family.toLowerCase().includes(q);
      })
      .sort((a, b) => STATUS_ORDER[a._status] - STATUS_ORDER[b._status] || a.name.localeCompare(b.name));
  }, [all, inSeason, filter, query]);

  /**
   * Split filtered results so in-season and out-of-season items can be rendered
   * with a visual divider between them.
   */
  const inSeasonItems  = useMemo(() => filtered.filter((v) => v._status !== "off"), [filtered]);
  const offSeasonItems = useMemo(() => filtered.filter((v) => v._status === "off"),  [filtered]);

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
            <div className="brand-title">Mise</div>
            <div className="brand-sub">a cook&apos;s almanac</div>
          </div>
        </div>

        <nav className="nav">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              className={`nav-item${page === item.id ? " active" : ""}${!item.ready ? " soon" : ""}`}
              onClick={() => item.ready && setPage(item.id as Page)}
              disabled={!item.ready}
              title={item.ready ? item.label : `${item.label} — coming soon`}
            >
              <NavIcon name={item.icon} />
              <span>{item.label}</span>
              {!item.ready && <span className="nav-soon">soon</span>}
            </button>
          ))}
        </nav>

        <div className="sidenav-footer">
          <div className="footer-loc">
            <span className="footer-flag" />
            <div>
              <div className="footer-loc-label">Region</div>
              <div className="footer-loc-name">The Netherlands</div>
            </div>
          </div>
          <div className="footer-build">v0.1 · seasonal module</div>
        </div>
      </aside>

      {/* Main content */}
      <main className="main">
        {page === "seasonal" && (
          <div className="page seasonal">
            <header className="page-hero">
              <div className="hero-eyebrow">
                <span className="hero-loc">📍 Netherlands</span>
                <span className="hero-dot">·</span>
                <span>{MONTHS_LONG[month - 1]} {new Date().getFullYear()}</span>
              </div>
              <h1 className="hero-title">
                What&apos;s in season <em>right now</em>
              </h1>
              <p className="hero-sub">
                {counts.peak} at peak · {counts.shoulder} available ·{" "}
                {counts.soon} coming soon · {counts.ending} ending
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
                    placeholder="Search…"
                    aria-label="Search vegetables"
                  />
                </div>
                <div className="seg">
                  <button className={view === "grid" ? "on" : ""} onClick={() => setView("grid")} aria-label="Grid view">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><rect x="3" y="3" width="8" height="8" rx="1"/><rect x="13" y="3" width="8" height="8" rx="1"/><rect x="3" y="13" width="8" height="8" rx="1"/><rect x="13" y="13" width="8" height="8" rx="1"/></svg>
                  </button>
                  <button className={view === "list" ? "on" : ""} onClick={() => setView("list")} aria-label="List view">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
                  </button>
                </div>
              </div>
            </div>

            {filtered.length === 0 ? (
              <div className="empty">No vegetables match. Try clearing the search.</div>
            ) : (
              <>
                {/* In-season vegetables */}
                {inSeasonItems.length > 0 && (
                  view === "list" ? (
                    <div className="veg-list">
                      {inSeasonItems.map((v) => (
                        <VegCard key={v.id} veg={v} status={v._status} month={month} density={density} onOpen={setOpenVeg} listMode />
                      ))}
                    </div>
                  ) : (
                    <div className={`veg-grid grid-${density}`}>
                      {inSeasonItems.map((v) => (
                        <VegCard key={v.id} veg={v} status={v._status} month={month} density={density} onOpen={setOpenVeg} />
                      ))}
                    </div>
                  )
                )}

                {/* Out-of-season vegetables — dimmed and separated below in-season items.
                    Visible when "All" is the active filter (the only filter that uses
                    the full dataset). Status-specific filters never produce off items. */}
                {offSeasonItems.length > 0 && (
                  <>
                    <div className="offseason-divider">
                      <span>Out of season</span>
                      <span className="offseason-count">{offSeasonItems.length}</span>
                    </div>
                    {view === "list" ? (
                      <div className="veg-list">
                        {offSeasonItems.map((v) => (
                          <VegCard key={v.id} veg={v} status={v._status} month={month} density={density} onOpen={setOpenVeg} listMode />
                        ))}
                      </div>
                    ) : (
                      <div className={`veg-grid grid-${density}`}>
                        {offSeasonItems.map((v) => (
                          <VegCard key={v.id} veg={v} status={v._status} month={month} density={density} onOpen={setOpenVeg} />
                        ))}
                      </div>
                    )}
                  </>
                )}
              </>
            )}

            <FuturePeek />
          </div>
        )}

        {page === "calendar" && (
          <CalendarPage month={month} onOpen={setOpenVeg} />
        )}
      </main>

      <DetailPanel veg={openVeg} month={month} onClose={() => setOpenVeg(null)} />
    </div>
  );
}
