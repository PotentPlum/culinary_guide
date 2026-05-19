"use client";

import { useEffect } from "react";
import type { Technique } from "@/lib/techniques";
import { TECHNIQUES, LEVEL_LABELS, HEAT_LABELS, CATEGORY_LABELS } from "@/lib/techniques";
import { VEG_DATA } from "@/lib/vegetables";
import { UI } from "@/lib/strings";

type Props = { technique: Technique | null; onClose: () => void };

export default function TechniqueDetail({ technique, onClose }: Props) {
  useEffect(() => {
    if (!technique) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [technique, onClose]);

  if (!technique) return null;

  const levelLabel = LEVEL_LABELS[technique.level];
  const heatLabel = technique.heatLevel != null ? HEAT_LABELS[technique.heatLevel] : null;
  const categoryLabel = CATEGORY_LABELS[technique.category];

  const relatedVegItems = (technique.relatedVeg ?? [])
    .map(name => VEG_DATA.find(v => v.name === name))
    .filter((v): v is NonNullable<typeof v> => v !== undefined);

  const relatedTechniques = (technique.relatedTechniqueIds ?? [])
    .map(id => TECHNIQUES.find(t => t.id === id))
    .filter((t): t is NonNullable<typeof t> => t !== undefined);

  return (
    <div className="detail-scrim" onClick={onClose}>
      <aside className="technique-drawer" onClick={(e) => e.stopPropagation()}>
        <button className="detail-close" onClick={onClose} aria-label={UI.techniques.closeAriaLabel}>✕</button>

        <div className="technique-drawer-hero">
          <div className="technique-drawer-category">
            {categoryLabel}{technique.subcategory ? ` · ${technique.subcategory}` : ""}
          </div>
          <h2 className="technique-drawer-name">{technique.nl}</h2>
          <p className="technique-drawer-subtitle">{technique.subtitle}</p>
          <div className="technique-drawer-badges">
            <span className="technique-level-badge">{levelLabel}</span>
            {technique.heatLevel && heatLabel && (
              <span style={{ display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "12px", color: "var(--ink-soft)" }}>
                <span className={`technique-heat-dot ${technique.heatLevel}`} />
                {heatLabel}
              </span>
            )}
          </div>
        </div>

        <div className="technique-drawer-body">
          {technique.body.map((para, i) => (
            <p
              key={i}
              className="detail-section"
              style={{ borderTop: i === 0 ? "none" : undefined, paddingTop: i === 0 ? 0 : undefined }}
            >
              {para}
            </p>
          ))}

          <section className="detail-section">
            <h3>{UI.techniques.sectionTips}</h3>
            <ul className="technique-tips-list">
              {technique.tips.map((tip, i) => <li key={i}>{tip}</li>)}
            </ul>
          </section>

          {technique.cookware && technique.cookware.length > 0 && (
            <section className="detail-section">
              <h3>{UI.techniques.sectionCookware}</h3>
              <div className="technique-cookware-list">
                {technique.cookware.map((c) => <span key={c} className="chip">{c}</span>)}
              </div>
            </section>
          )}

          {relatedVegItems.length > 0 && (
            <section className="detail-section">
              <h3>{UI.techniques.sectionRelatedVeg}</h3>
              <div className="chips">
                {relatedVegItems.map((v) => (
                  <span
                    key={v.name}
                    className="technique-veg-chip"
                    style={{ background: v.color, borderColor: v.accent }}
                  >
                    {v.nl}
                  </span>
                ))}
              </div>
            </section>
          )}

          {relatedTechniques.length > 0 && (
            <section className="detail-section">
              <h3>{UI.techniques.sectionRelatedTechniques}</h3>
              <div className="technique-related-grid">
                {relatedTechniques.map((t) => (
                  <button
                    key={t.id}
                    className="technique-related-card"
                    onClick={(e) => {
                      e.stopPropagation();
                      onClose();
                    }}
                  >
                    <div className="technique-related-card-name">{t.nl}</div>
                    <div className="technique-related-card-sub">{t.subtitle}</div>
                  </button>
                ))}
              </div>
            </section>
          )}
        </div>
      </aside>
    </div>
  );
}
