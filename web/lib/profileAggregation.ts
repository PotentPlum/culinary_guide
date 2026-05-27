import { DISCIPLINE_MAP } from './disciplineMap';
import type { Discipline, UserAttempt, DisciplineScore, ProfileStats } from './profileTypes';

const DISCIPLINE_LABELS: Record<Discipline, string> = {
  mechanical: 'Mechanisch Meesterschap',
  thermal:    'Thermische Controle',
  flavor:     'Smaakcompositie',
  material:   'Materiaalbeheersing',
};

const ALL_DISCIPLINES: Discipline[] = ['mechanical', 'thermal', 'flavor', 'material'];

/** 30 days in milliseconds */
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

export function computeProfileStats(attempts: UserAttempt[]): ProfileStats {
  const now = Date.now();

  // ── disciplines ────────────────────────────────────────────────────────────
  const disciplines: DisciplineScore[] = ALL_DISCIPLINES.map((discipline) => {
    const matching = attempts.filter((a) =>
      a.techniques.some((t) => DISCIPLINE_MAP[t] === discipline)
    );

    const scores = matching
      .map((a) => a.score)
      .filter((s): s is number => s !== null);

    const proficiency =
      scores.length > 0
        ? scores.reduce((sum, s) => sum + s, 0) / scores.length
        : 0;

    return {
      discipline,
      label: DISCIPLINE_LABELS[discipline],
      frequency: matching.length,
      proficiency,
    };
  });

  // ── topCuisines ────────────────────────────────────────────────────────────
  const cuisineCountMap = new Map<string, number>();
  const cuisineScoreMap = new Map<string, number[]>();

  for (const attempt of attempts) {
    for (const cuisine of attempt.cuisines) {
      cuisineCountMap.set(cuisine, (cuisineCountMap.get(cuisine) ?? 0) + 1);
      if (attempt.taste_score !== null) {
        const scores = cuisineScoreMap.get(cuisine) ?? [];
        scores.push(attempt.taste_score);
        cuisineScoreMap.set(cuisine, scores);
      }
    }
  }

  const topCuisines = Array.from(cuisineCountMap.entries())
    .map(([cuisine, count]) => {
      const scores = cuisineScoreMap.get(cuisine) ?? [];
      const avgScore =
        scores.length > 0
          ? scores.reduce((sum, s) => sum + s, 0) / scores.length
          : 0;
      return { cuisine, count, avgScore };
    })
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);

  // ── ingredientCloud ────────────────────────────────────────────────────────
  const ingredientCountMap = new Map<string, number>();

  for (const attempt of attempts) {
    for (const ingredient of attempt.ingredients) {
      ingredientCountMap.set(
        ingredient,
        (ingredientCountMap.get(ingredient) ?? 0) + 1
      );
    }
  }

  const ingredientCloud = Array.from(ingredientCountMap.entries())
    .map(([ingredient, count]) => ({ ingredient, count }))
    .sort((a, b) => b.count - a.count);

  // ── topTechniques ──────────────────────────────────────────────────────────
  const techniqueCountMap = new Map<string, number>();
  const techniqueScoreMap = new Map<string, number[]>();

  for (const attempt of attempts) {
    for (const techniqueId of attempt.techniques) {
      techniqueCountMap.set(
        techniqueId,
        (techniqueCountMap.get(techniqueId) ?? 0) + 1
      );
      if (attempt.score !== null) {
        const scores = techniqueScoreMap.get(techniqueId) ?? [];
        scores.push(attempt.score);
        techniqueScoreMap.set(techniqueId, scores);
      }
    }
  }

  const topTechniques = Array.from(techniqueCountMap.entries())
    .map(([techniqueId, count]) => {
      const scores = techniqueScoreMap.get(techniqueId) ?? [];
      const avgScore =
        scores.length > 0
          ? scores.reduce((sum, s) => sum + s, 0) / scores.length
          : 0;
      return { techniqueId, count, avgScore };
    })
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);

  // ── neglectedDisciplines ───────────────────────────────────────────────────
  const neglectedDisciplines: Discipline[] = ALL_DISCIPLINES.filter(
    (discipline) => {
      const matching = attempts.filter((a) =>
        a.techniques.some((t) => DISCIPLINE_MAP[t] === discipline)
      );

      if (matching.length === 0) return true;

      const mostRecent = Math.max(
        ...matching.map((a) => new Date(a.attempted_at).getTime())
      );

      return now - mostRecent > THIRTY_DAYS_MS;
    }
  );

  // ── recentAttempts ─────────────────────────────────────────────────────────
  const recentAttempts = [...attempts]
    .sort(
      (a, b) =>
        new Date(b.attempted_at).getTime() - new Date(a.attempted_at).getTime()
    )
    .slice(0, 20);

  return {
    disciplines,
    topCuisines,
    ingredientCloud,
    topTechniques,
    neglectedDisciplines,
    recentAttempts,
  };
}
