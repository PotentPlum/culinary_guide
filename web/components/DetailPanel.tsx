"use client";

import { useEffect } from "react";
import type { VegItem } from "@/lib/vegetables";
import { statusFor, MONTHS_LONG } from "@/lib/vegetables";
import VegToken from "./VegToken";
import StatusDot from "./StatusDot";
import PeakBar from "./PeakBar";

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
        <button className="detail-close" onClick={onClose} aria-label="Close">✕</button>

        <div className="detail-hero" style={{ background: veg.color }}>
          <div className="detail-illus">
            <VegToken veg={veg} size={180} />
          </div>
          <div className="detail-hero-meta">
            <div className="detail-family">{veg.family}</div>
            <h2 className="detail-name">{veg.name}</h2>
            <div className="detail-nl">{veg.nl}</div>
            <div className="detail-status"><StatusDot status={status} withLabel /></div>
          </div>
        </div>

        <div className="detail-body">
          <p className="detail-blurb">{veg.blurb}</p>

          <section className="detail-section">
            <h3>Peak season</h3>
            <PeakBar veg={veg} month={month} />
            <div className="peak-legend">
              <span><span className="dot on" /> Peak</span>
              <span><span className="dot half" /> Available</span>
              <span><span className="dot now" /> {MONTHS_LONG[month - 1]}</span>
            </div>
          </section>

          <section className="detail-section">
            <h3>How to store</h3>
            <p>{veg.storage}</p>
          </section>

          <section className="detail-section">
            <h3>How to prep</h3>
            <p>{veg.prep}</p>
          </section>

          <section className="detail-section">
            <h3>Goes well with</h3>
            <div className="chips">
              {veg.pairings.map((p) => <span key={p} className="chip">{p}</span>)}
            </div>
          </section>

          <section className="detail-section ghost">
            <div className="ghost-head">
              <h3>If you don&apos;t have it</h3>
              <span className="ghost-tag">Substitutions · soon</span>
            </div>
            <div className="ghost-body">
              <p className="ghost-preview">Likely subs: <em>{veg.substitutions.join(", ")}</em></p>
              <p className="ghost-note">A full pairing &amp; substitution explorer is coming in the next release.</p>
            </div>
          </section>

          {veg.recipes && veg.recipes.length > 0 ? (
            <section className="detail-section">
              <h3>Cook it</h3>
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
                          {r.difficulty === "easy" ? "Makkelijk" : r.difficulty === "medium" ? "Gemiddeld" : "Moeilijk"}
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
                <h3>Cook it</h3>
                <span className="ghost-tag">Recepten · binnenkort</span>
              </div>
              <div className="ghost-body">
                <p className="ghost-preview"><em>Technique tip:</em> {veg.technique}</p>
                <p className="ghost-note">Linked recipes drop with the recipe module.</p>
              </div>
            </section>
          )}

          <section className="detail-section ghost">
            <div className="ghost-head">
              <h3>Find it nearby</h3>
              <span className="ghost-tag">Markets &amp; foraging · soon</span>
            </div>
            <div className="ghost-body">
              <p className="ghost-note">Farmers&apos; market schedules and (for wild species) safe foraging spots will live here.</p>
            </div>
          </section>
        </div>
      </aside>
    </div>
  );
}
