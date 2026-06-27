"use client";

import { useEffect, useState } from "react";
import { UI } from "@/lib/strings";
import { apiHeaders } from "@/lib/apiClient";
import { computeProfileStats } from "@/lib/profileAggregation";
import { computeArchetypes } from "@/lib/archetypes";
import { TECHNIQUES } from "@/lib/techniques";
import type { UserAttempt, ProfileStats } from "@/lib/profileTypes";
import RadarChart, { type RadarAxis } from "@/components/RadarChart";
import ArchetypeBadges from "@/components/ArchetypeBadges";

const DISCIPLINE_ORDER = ["mechanical", "thermal", "flavor", "material"] as const;

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("nl-NL", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function getTechniqueLabel(id: string): string {
  return TECHNIQUES.find((t) => t.id === id)?.nl ?? id;
}

export default function ProfilePage() {
  const [attempts, setAttempts] = useState<UserAttempt[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/profile/stats", { headers: apiHeaders() })
      .then((r) => r.json())
      .then((d: { attempts?: UserAttempt[] }) => {
        setAttempts(d.attempts ?? []);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="page profile-page">
        <div className="profile-empty-state">
          <p>{UI.profile.loadingMsg}</p>
        </div>
      </div>
    );
  }

  if (attempts.length === 0) {
    return (
      <div className="page profile-page">
        <header className="profile-page-hero">
          <h1 className="hero-title"><em>{UI.profile.pageTitle}</em></h1>
          <p className="hero-sub">{UI.profile.pageSubtitle}</p>
        </header>
        <div className="profile-empty-state">
          <p>{UI.profile.emptyState}</p>
        </div>
      </div>
    );
  }

  const stats: ProfileStats = computeProfileStats(attempts);
  const archetypes = computeArchetypes(stats, attempts);

  const maxFreq = Math.max(...stats.disciplines.map((d) => d.frequency), 1);
  const maxProf = 10;

  const radarAxes: RadarAxis[] = DISCIPLINE_ORDER.flatMap((disc) => {
    const d = stats.disciplines.find((x) => x.discipline === disc);
    if (!d) return [];
    return [{ label: d.label, frequency: d.frequency / maxFreq, proficiency: d.proficiency / maxProf }];
  });

  const topCuisinesMax = stats.topCuisines[0]?.count ?? 1;
  const ingredientCloudMax = stats.ingredientCloud[0]?.count ?? 1;
  const highRatedCuisines = [...stats.topCuisines]
    .filter((c) => c.avgScore > 0)
    .sort((a, b) => b.avgScore - a.avgScore)
    .slice(0, 5);

  const topTechniquesDisplay = stats.topTechniques.slice(0, 5);
  const highRatedTechniques = [...stats.topTechniques]
    .filter((t) => t.avgScore > 0 && t.count >= 2)
    .sort((a, b) => b.avgScore - a.avgScore)
    .slice(0, 5);

  return (
    <div className="page profile-page">
      <header className="profile-page-hero">
        <h1 className="hero-title"><em>{UI.profile.pageTitle}</em></h1>
        <p className="hero-sub">{UI.profile.pageSubtitle}</p>
      </header>

      <div className="profile-pillars-grid">
        {/* Pillar A — Radar */}
        <section className="profile-pillar">
          <p className="profile-pillar-title">{UI.profile.pillarA}</p>
          <div className="profile-radar-wrapper">
            <RadarChart axes={radarAxes} size={280} />
          </div>
          <div className="profile-radar-legend">
            <span className="profile-radar-legend-item">
              <span className="profile-radar-legend-dot" style={{ background: "rgba(63,94,42,0.25)" }} />
              {UI.profile.labelFrequency}
            </span>
            <span className="profile-radar-legend-item">
              <span className="profile-radar-legend-dot" style={{ background: "var(--primary)" }} />
              {UI.profile.labelProficiency}
            </span>
          </div>
        </section>

        {/* Pillar B — Archetypes */}
        <section className="profile-pillar">
          <p className="profile-pillar-title">{UI.profile.pillarB}</p>
          <ArchetypeBadges archetypes={archetypes} />
        </section>


        {/* Pillar C — Cuisines & Ingredients */}
        <section className="profile-pillar profile-pillar--full">
        <p className="profile-pillar-title">{UI.profile.pillarC}</p>

        {stats.topCuisines.length > 0 && (
          <>
            <p className="profile-section-label">{UI.profile.topCuisinesTitle}</p>
            <div className="profile-cuisine-bar">
              {stats.topCuisines.slice(0, 6).map((c) => (
                <div key={c.cuisine} className="profile-cuisine-row">
                  <div className="profile-cuisine-label">
                    <span>{c.cuisine}</span>
                    <span className="profile-cuisine-count">{c.count}×</span>
                  </div>
                  <div className="profile-cuisine-track">
                    <div
                      className="profile-cuisine-fill"
                      style={{ width: `${(c.count / topCuisinesMax) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {highRatedCuisines.length > 0 && (
          <>
            <p className="profile-section-label" style={{ marginTop: 12 }}>{UI.profile.highRatedCuisinesTitle}</p>
            <div className="profile-cuisine-bar">
              {highRatedCuisines.map((c) => (
                <div key={c.cuisine} className="profile-stat-row">
                  <span className="profile-stat-name">{c.cuisine}</span>
                  <span className="profile-score-chip profile-score-chip--taste">
                    {c.avgScore.toFixed(1)}
                  </span>
                </div>
              ))}
            </div>
          </>
        )}

        {stats.ingredientCloud.length > 0 && (
          <>
            <p className="profile-section-label" style={{ marginTop: 12 }}>{UI.profile.ingredientCloudTitle}</p>
            <div className="profile-ingredient-cloud">
              {stats.ingredientCloud.slice(0, 30).map((item) => {
                const scale = 0.75 + (item.count / ingredientCloudMax) * 0.75;
                return (
                  <span
                    key={item.ingredient}
                    style={{ fontSize: `${scale}em`, opacity: 0.5 + (item.count / ingredientCloudMax) * 0.5 }}
                  >
                    {item.ingredient}
                  </span>
                );
              })}
            </div>
          </>
        )}

        {stats.topCuisines.length === 0 && stats.ingredientCloud.length === 0 && (
          <p className="profile-stat-name" style={{ fontStyle: "italic", color: "var(--ink-soft)" }}>
            {UI.profile.ingredientCloudEmpty}
          </p>
        )}
        </section>

        {/* Pillar D — Technique stats */}
        <section className="profile-pillar profile-pillar--full">
        <p className="profile-pillar-title">{UI.profile.pillarD}</p>

        {topTechniquesDisplay.length > 0 && (
          <>
            <p className="profile-section-label">{UI.profile.labelTopTechniques}</p>
            <div>
              {topTechniquesDisplay.map((t) => (
                <div key={t.techniqueId} className="profile-stat-row">
                  <span className="profile-stat-name">{getTechniqueLabel(t.techniqueId)}</span>
                  <span className="profile-stat-count">{t.count}×</span>
                  {t.avgScore > 0 && (
                    <span className="profile-score-chip">{t.avgScore.toFixed(1)}</span>
                  )}
                </div>
              ))}
            </div>
          </>
        )}

        {highRatedTechniques.length > 0 && (
          <>
            <p className="profile-section-label" style={{ marginTop: 12 }}>{UI.profile.labelHighRated}</p>
            <div>
              {highRatedTechniques.map((t) => (
                <div key={t.techniqueId} className="profile-stat-row">
                  <span className="profile-stat-name">{getTechniqueLabel(t.techniqueId)}</span>
                  <span className="profile-score-chip">{t.avgScore.toFixed(1)}</span>
                </div>
              ))}
            </div>
          </>
        )}

        {stats.neglectedDisciplines.length > 0 && (
          <>
            <p className="profile-section-label" style={{ marginTop: 12 }}>{UI.profile.labelNeglected}</p>
            <div className="profile-neglected-list">
              {stats.neglectedDisciplines.map((disc) => {
                const label = stats.disciplines.find((d) => d.discipline === disc)?.label ?? disc;
                return (
                  <span key={disc} className="profile-neglected-chip">{label}</span>
                );
              })}
            </div>
          </>
        )}

        {topTechniquesDisplay.length === 0 && (
          <p className="profile-stat-name" style={{ fontStyle: "italic", color: "var(--ink-soft)" }}>
            {UI.profile.neglectedEmpty}
          </p>
        )}
        </section>

        {/* Pillar E — Attempt ledger */}
        <section className="profile-pillar profile-pillar--full">
          <p className="profile-pillar-title">{UI.profile.pillarE}</p>
          {stats.recentAttempts.length === 0 ? (
            <p className="profile-stat-name" style={{ fontStyle: "italic", color: "var(--ink-soft)" }}>
              {UI.profile.ledgerEmpty}
            </p>
          ) : (
            <div className="ledger-list">
              {stats.recentAttempts.map((a) => (
                <div key={a.id} className="ledger-entry">
                  <div className="ledger-entry-meta">
                    <span className="ledger-entry-date">{formatDate(a.attempted_at)}</span>
                    <span className="ledger-entry-source">{a.source_type}</span>
                    {a.score !== null && (
                      <span className="profile-score-chip">{a.score}/10</span>
                    )}
                    {a.taste_score !== null && (
                      <span className="profile-score-chip profile-score-chip--taste">{a.taste_score}/10</span>
                    )}
                  </div>
                  {a.techniques.length > 0 && (
                    <p className="ledger-entry-techniques">
                      {a.techniques.slice(0, 4).map(getTechniqueLabel).join(" · ")}
                    </p>
                  )}
                  {a.notes && <p className="ledger-entry-notes">{a.notes}</p>}
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
