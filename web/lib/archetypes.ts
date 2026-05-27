import type { ProfileStats, UserAttempt, ArchetypeResult } from './profileTypes';
import { getDiscipline } from './disciplineMap';
import { UI } from './strings';

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
    nl: UI.profile.archetypes.emulsieIngenieur.nl,
    description: UI.profile.archetypes.emulsieIngenieur.desc,
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
    nl: UI.profile.archetypes.koolstofArtisan.nl,
    description: UI.profile.archetypes.koolstofArtisan.desc,
    check: (_stats, attempts) => {
      const count = attempts.filter((a) =>
        a.techniques.some((t) => t === 'schroeien' || t === 'gratineren'),
      ).length;
      return count >= 5;
    },
  },
  {
    id: 'messenvirtuoos',
    nl: UI.profile.archetypes.messenvirtuoos.nl,
    description: UI.profile.archetypes.messenvirtuoos.desc,
    check: (_stats, attempts) => {
      const count = attempts.filter((a) =>
        a.techniques.some((t) => getDiscipline(t) === 'mechanical'),
      ).length;
      return count >= 10;
    },
  },
  {
    id: 'saus-architect',
    nl: UI.profile.archetypes.sausArchitect.nl,
    description: UI.profile.archetypes.sausArchitect.desc,
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
    nl: UI.profile.archetypes.stilleFermenteur.nl,
    description: UI.profile.archetypes.stilleFermenteur.desc,
    check: (_stats, attempts) => {
      const FERMENT_IDS = new Set(['pekelen', 'marineren', 'fermenteren', 'inmaken']);
      const count = attempts.filter((a) =>
        a.techniques.some((t) => FERMENT_IDS.has(t)),
      ).length;
      return count >= 3;
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
