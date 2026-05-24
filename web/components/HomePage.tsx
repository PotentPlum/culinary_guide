"use client";

import { useMemo } from "react";
import { VEG_DATA, statusFor, MONTHS_LONG, type VegItem } from "@/lib/vegetables";
import { UI } from "@/lib/strings";
import StatusDot from "./StatusDot";
import PeakBar from "./PeakBar";
import VegToken from "./VegToken";
import NavIcon from "./NavIcon";

type Page = "seasonal" | "calendar" | "recipes" | "technieken" | "challenge";

type Props = {
  month: number;
  onNavigate: (page: Page) => void;
  onOpen: (veg: VegItem) => void;
};

function MiniSection({
  items,
  total,
  title,
  dotClass,
  onNavigate,
  onOpen,
}: {
  items: (VegItem & { _status: ReturnType<typeof statusFor> })[];
  total: number;
  title: string;
  dotClass: string;
  onNavigate: () => void;
  onOpen: (veg: VegItem) => void;
}) {
  if (items.length === 0) return null;
  return (
    <section className="hp-section">
      <div className="hp-section-header">
        <div className="hp-section-title">
          <span className={`hp-section-dot ${dotClass}`} />
          {title}
        </div>
        {total > items.length && (
          <button className="hp-section-link" onClick={onNavigate}>
            {UI.home.moreLink}
          </button>
        )}
      </div>
      <div className="hp-mini-list">
        {items.map((v) => (
          <button key={v.id} className="hp-mini-item" onClick={() => onOpen(v)}>
            <div className="hp-mini-swatch" style={{ background: v.color, borderColor: v.accent }} />
            <span className="hp-mini-name">{v.nl}</span>
            <StatusDot status={v._status} />
          </button>
        ))}
      </div>
    </section>
  );
}

export default function HomePage({ month, onNavigate, onOpen }: Props) {
  const now = new Date();
  const monthName = MONTHS_LONG[month - 1];
  const year = now.getFullYear();

  const all = useMemo(
    () => VEG_DATA.map((v) => ({ ...v, _status: statusFor(v, month) })),
    [month]
  );

  const peak   = useMemo(() => all.filter((v) => v._status === "peak"),   [all]);
  const soon   = useMemo(() => all.filter((v) => v._status === "soon"),   [all]);
  const ending = useMemo(() => all.filter((v) => v._status === "ending"), [all]);

  const spotlight = useMemo(() => peak.slice(0, 4),  [peak]);
  const comingUp  = useMemo(() => soon.slice(0, 3),  [soon]);
  const lastCall  = useMemo(() => ending.slice(0, 3), [ending]);

  const tipVeg = peak[0] ?? null;

  return (
    <div className="homepage">
      {/* ── Hero ── */}
      <section className="hp-hero">
        <div className="hp-hero-eyebrow">
          <span className="hp-hero-flag" />
          <span>{UI.hero.eyebrowLoc}</span>
          <span className="hp-hero-dot">·</span>
          <span>{monthName} {year}</span>
        </div>

        <h1 className="hp-hero-title">
          {UI.home.heroTitle}<br />
          <em>{UI.home.heroTitleEm}</em> {UI.home.heroTitleEnd}
        </h1>
        <p className="hp-hero-sub">{UI.home.heroSub}</p>

        <div className="hp-season-stats">
          <div className="hp-stat hp-stat--peak" onClick={() => onNavigate("seasonal")}>
            <span className="hp-stat-num">{peak.length}</span>
            <span className="hp-stat-label">{UI.home.statPeak}</span>
          </div>
          <div className="hp-stat-divider" />
          <div className="hp-stat hp-stat--soon" onClick={() => onNavigate("seasonal")}>
            <span className="hp-stat-num">{soon.length}</span>
            <span className="hp-stat-label">{UI.home.statSoon}</span>
          </div>
          <div className="hp-stat-divider" />
          <div className="hp-stat hp-stat--ending" onClick={() => onNavigate("seasonal")}>
            <span className="hp-stat-num">{ending.length}</span>
            <span className="hp-stat-label">{UI.home.statEnding}</span>
          </div>
        </div>
      </section>

      {/* ── Module nav ── */}
      <section className="hp-modules">
        <div className="hp-module-label">{UI.home.modulesLabel}</div>
        <div className="hp-module-grid">
          {UI.home.modules.map((mod) => (
            <button
              key={mod.id}
              className={`hp-module-card hp-module--${mod.css}`}
              onClick={() => onNavigate(mod.id as Page)}
            >
              <div className="hp-module-icon-wrap">
                <NavIcon name={mod.icon} />
              </div>
              <div className="hp-module-body">
                <div className="hp-module-name">{mod.name}</div>
                <div className="hp-module-desc">{mod.desc}</div>
              </div>
              <span className="hp-module-arrow">→</span>
            </button>
          ))}
        </div>
      </section>

      {/* ── Now in season spotlight ── */}
      {spotlight.length > 0 && (
        <section className="hp-section">
          <div className="hp-section-header">
            <div className="hp-section-title">
              <span className="hp-section-dot hp-section-dot--peak" />
              {UI.home.spotlightTitle}
            </div>
            <button className="hp-section-link" onClick={() => onNavigate("seasonal")}>
              {UI.home.spotlightMore(peak.length)}
            </button>
          </div>
          <div className="hp-spotlight-grid">
            {spotlight.map((v) => (
              <button
                key={v.id}
                className="hp-spotlight-card"
                onClick={() => onOpen(v)}
                style={{ '--spot-color': v.color } as React.CSSProperties}
              >
                <VegToken veg={v} size={56} />
                <div className="hp-spotlight-body">
                  <div className="hp-spotlight-name">{v.nl}</div>
                  <div className="hp-spotlight-family">{v.family}</div>
                  <PeakBar veg={v} month={month} compact />
                </div>
                <span className="hp-spotlight-arrow" aria-hidden="true">→</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {/* ── Binnenkort + Laatste kans ── */}
      <div className="hp-two-col">
        <MiniSection
          items={comingUp}
          total={soon.length}
          title={UI.home.soonTitle}
          dotClass="hp-section-dot--soon"
          onNavigate={() => onNavigate("seasonal")}
          onOpen={onOpen}
        />
        <MiniSection
          items={lastCall}
          total={ending.length}
          title={UI.home.endingTitle}
          dotClass="hp-section-dot--ending"
          onNavigate={() => onNavigate("seasonal")}
          onOpen={onOpen}
        />
      </div>

      {/* ── Seizoenstip ── */}
      <section className="hp-tip-strip">
        <div className="hp-tip-label">{UI.home.tipLabel}</div>
        <p className="hp-tip-text">
          {tipVeg ? `${tipVeg.nl}: ${tipVeg.blurb}` : UI.home.tipFallback}
        </p>
        {tipVeg && (
          <button className="hp-tip-link" onClick={() => onOpen(tipVeg)}>
            {UI.home.tipLink(tipVeg.nl)}
          </button>
        )}
      </section>
    </div>
  );
}
