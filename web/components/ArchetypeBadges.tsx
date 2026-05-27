"use client";

import type { ArchetypeResult } from "@/lib/profileTypes";

const ARCHETYPE_ICONS: Record<string, string> = {
  "emulsie-ingenieur": "🫧",
  "koolstof-artisan":  "🔥",
  "messenvirtuoos":    "🔪",
  "saus-architect":    "🍶",
  "stille-fermenteur": "🧂",
};

interface ArchetypeBadgesProps {
  archetypes: ArchetypeResult[];
}

export default function ArchetypeBadges({ archetypes }: ArchetypeBadgesProps) {
  return (
    <div className="archetype-badges-grid">
      {archetypes.map((archetype) => (
        <div
          key={archetype.id}
          className={[
            "archetype-badge",
            archetype.earned
              ? "archetype-badge--earned"
              : "archetype-badge--locked",
          ].join(" ")}
          aria-label={
            archetype.earned
              ? archetype.nl
              : `${archetype.nl} (vergrendeld)`
          }
        >
          <div className="archetype-badge-icon" aria-hidden="true">
            {archetype.earned
              ? (ARCHETYPE_ICONS[archetype.id] ?? "🏅")
              : "🔒"}
          </div>
          <p className="archetype-badge-title">{archetype.nl}</p>
          <p className="archetype-badge-desc">{archetype.description}</p>
        </div>
      ))}
    </div>
  );
}
