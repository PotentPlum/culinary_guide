import type { ProfileStats, UserAttempt, ArchetypeResult } from './profileTypes';
import { getDiscipline } from './disciplineMap';

const SAUCE_TECHNIQUE_IDS = new Set([
  'roux',
  'bechamel',
  'veloute',
  'tomatensaus',
  'hollandaise',
  'beurre-blanc',
]);

export const ARCHETYPES: Array<{
  id: string;
  nl: string;
  description: string;
  check: (stats: ProfileStats, attempts: UserAttempt[]) => boolean;
}> = [
  {
    id: 'emulsie-ingenieur',
    nl: 'De Emulsie-ingenieur',
    description: 'Meester in emulsies en sauzen',
    check: (_stats, attempts) => {
      const flavorAttempts = attempts.filter((a) =>
        a.techniques.some((t) => getDiscipline(t) === 'flavor'),
      );
      if (flavorAttempts.length < 10) return false;
      const scores = flavorAttempts
        .map((a) => a.score)
        .filter((s): s is number => s !== null);
      if (scores.length === 0) return false;
      const avg = scores.reduce((sum, s) => sum + s, 0) / scores.length;
      return avg >= 7.0;
    },
  },
  {
    id: 'koolstof-artisan',
    nl: 'De Koolstof-artisan',
    description: 'Heer van het hoge vuur',
    check: (_stats, attempts) => {
      const count = attempts.filter((a) =>
        a.techniques.some((t) => t === 'schroeien' || t === 'gratineren'),
      ).length;
      return count >= 5;
    },
  },
  {
    id: 'messenvirtuoos',
    nl: 'De Messenvirtuoos',
    description: 'Precisie aan het snijvlak',
    check: (_stats, attempts) => {
      const count = attempts.filter((a) =>
        a.techniques.some((t) => getDiscipline(t) === 'mechanical'),
      ).length;
      return count >= 10;
    },
  },
  {
    id: 'saus-architect',
    nl: 'De Saus-architect',
    description: 'Bouwer van complexe sauzen',
    check: (_stats, attempts) => {
      const sauceAttempts = attempts.filter((a) =>
        a.techniques.some((t) => SAUCE_TECHNIQUE_IDS.has(t)),
      );
      if (sauceAttempts.length < 3) return false;
      const scores = sauceAttempts
        .map((a) => a.score)
        .filter((s): s is number => s !== null);
      if (scores.length === 0) return false;
      const avg = scores.reduce((sum, s) => sum + s, 0) / scores.length;
      return avg >= 8.0;
    },
  },
  {
    id: 'stille-fermenteur',
    nl: 'De Stille Fermenteur',
    description: 'Geduld als geheime ingrediënt',
    check: (_stats, attempts) => {
      return attempts.some((a) =>
        a.techniques.some((t) => t.includes('pekelen') || t.includes('marineren')),
      );
    },
  },
];

export function computeArchetypes(
  stats: ProfileStats,
  attempts: UserAttempt[],
): ArchetypeResult[] {
  return ARCHETYPES.map(({ id, nl, description, check }) => ({
    id,
    nl,
    description,
    earned: check(stats, attempts),
  }));
}
