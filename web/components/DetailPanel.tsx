"use client";

import { useEffect } from "react";
import type { VegItem } from "@/lib/vegetables";
import { statusFor, MONTHS_LONG } from "@/lib/vegetables";
import { UI } from "@/lib/strings";
import VegToken from "./VegToken";
import StatusDot from "./StatusDot";
import PeakBar from "./PeakBar";

const DIFFICULTY_LABEL: Record<string, string> = {
  easy:   UI.detail.difficultyEasy,
  medium: UI.detail.difficultyMedium,
  hard:   UI.detail.difficultyHard,
};

type Props = { veg: VegItem | null; month: number; onClose: () => void };

export default function DetailPanel({ veg, month, onClose }: Props) {
  useEffect(() => {
    if (!veg) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [veg, onClose]);

  if (!veg) return null;
  const status = statusFor(veg, month);

  return (
    <div className="detail-scrim" onClick={onClose}>
      <aside className="detail-panel" onClick={(e) => e.stopPropagation()}>
        <button className="detail-close" onClick={onClose} aria-label={UI.detail.close}>✕</button>

        <div className="detail-hero" style={{ '--hero-accent': veg.color } as React.CSSProperties}>
          <div className="detail-illus">
            <VegToken veg={veg} size={180} />
          </div>
          <div className="detail-hero-meta">
            <div className="detail-family">{veg.family}</div>
            <h2 className="detail-name">{veg.nl}</h2>
            <div className="detail-status"><StatusDot status={status} withLabel /></div>
          </div>
        </div>

        <div className="detail-body">
          <p className="detail-blurb">{veg.blurb}</p>

          <section className="detail-section">
            <h3>{UI.detail.sectionSeason}</h3>
            <PeakBar veg={veg} month={month} />
            <div className="peak-legend">
              <span><span className="dot on" /> {UI.detail.legendPeak}</span>
              <span><span className="dot half" /> {UI.detail.legendShoulder}</span>
              <span><span className="dot now" /> {MONTHS_LONG[month - 1]}</span>
            </div>
          </section>

          <section className="detail-section">
            <h3>{UI.detail.sectionStorage}</h3>
            <p>{veg.storage}</p>
          </section>

          <section className="detail-section">
            <h3>{UI.detail.sectionPrep}</h3>
            <p>{veg.prep}</p>
          </section>

          <section className="detail-section">
            <h3>{UI.detail.sectionPairings}</h3>
            <div className="chips">
              {veg.pairings.map((p) => <span key={p} className="chip">{p}</span>)}
            </div>
          </section>

          <section className="detail-section ghost">
            <div className="ghost-head">
              <h3>{UI.detail.sectionSubs}</h3>
              <span className="ghost-tag">{UI.detail.subsTag}</span>
            </div>
            <div className="ghost-body">
              <p className="ghost-preview">{UI.detail.subsPreviewPre} <em>{veg.substitutions.join(", ")}</em></p>
              <p className="ghost-note">{UI.detail.subsNote}</p>
            </div>
          </section>

          {veg.recipes && veg.recipes.length > 0 ? (
            <section className="detail-section">
              <h3>{UI.detail.sectionRecipes}</h3>
              <div className="recipe-list">
                {veg.recipes.map((r, i) => (
                  <a
                    key={`${r.url}-${i}`}
                    href={r.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="recipe-card"
                  >
                    <div className="recipe-card-top">
                      <span className="recipe-source">{r.source}</span>
                      <div className="recipe-meta">
                        <span className="recipe-chip">{r.time_mins} min</span>
                        <span className={`recipe-chip recipe-difficulty recipe-difficulty--${r.difficulty}`}>
                          {DIFFICULTY_LABEL[r.difficulty] ?? UI.detail.difficultyHard}
                        </span>
                      </div>
                    </div>
                    <div className="recipe-title">{r.title}</div>
                    <p className="recipe-why">{r.why}</p>
                  </a>
                ))}
              </div>
            </section>
          ) : (
            <section className="detail-section ghost">
              <div className="ghost-head">
                <h3>{UI.detail.sectionRecipes}</h3>
                <span className="ghost-tag">{UI.detail.recipesTag}</span>
              </div>
              <div className="ghost-body">
                <p className="ghost-preview"><em>{UI.detail.recipeTechPre}</em> {veg.technique}</p>
                <p className="ghost-note">{UI.detail.recipesNote}</p>
              </div>
            </section>
          )}

          <section className="detail-section ghost">
            <div className="ghost-head">
              <h3>{UI.detail.sectionForaging}</h3>
              <span className="ghost-tag">{UI.detail.foragingTag}</span>
            </div>
            <div className="ghost-body">
              <p className="ghost-note">{UI.detail.foragingNote}</p>
            </div>
          </section>
        </div>
      </aside>
    </div>
  );
}
