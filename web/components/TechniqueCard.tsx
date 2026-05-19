"use client";

import type { Technique } from "@/lib/techniques";
import { LEVEL_LABELS } from "@/lib/techniques";

type Props = { technique: Technique; onOpen: (t: Technique) => void };

export default function TechniqueCard({ technique, onOpen }: Props) {
  const levelLabel = LEVEL_LABELS[technique.level];

  return (
    <button className="technique-card" onClick={() => onOpen(technique)}>
      <div className="technique-card-header">
        <h3 className="technique-card-name">{technique.nl}</h3>
        <span className="technique-level-badge">{levelLabel}</span>
      </div>
      <p className="technique-card-subtitle">{technique.subtitle}</p>
      <div className="technique-card-meta">
        {technique.heatLevel && technique.heatLevel !== "geen" && (
          <span className={`technique-heat-dot ${technique.heatLevel}`} />
        )}
        {technique.subcategory && (
          <span className="technique-subcategory">{technique.subcategory}</span>
        )}
      </div>
    </button>
  );
}
