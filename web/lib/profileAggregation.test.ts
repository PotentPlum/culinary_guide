import { describe, it, expect } from 'vitest';
import { computeProfileStats } from './profileAggregation';
import type { UserAttempt } from './profileTypes';

function makeAttempt(overrides: Partial<UserAttempt> & { id: string }): UserAttempt {
  return {
    user_id: 'user-1',
    source_type: 'challenge',
    source_id: null,
    attempted_at: new Date().toISOString(),
    score: null,
    techniques: [],
    cuisines: [],
    ingredients: [],
    taste_score: null,
    notes: null,
    ...overrides,
  };
}

describe('computeProfileStats', () => {
  it('empty attempts returns sensible defaults', () => {
    const stats = computeProfileStats([]);

    expect(stats.disciplines).toHaveLength(4);
    for (const d of stats.disciplines) {
      expect(d.frequency).toBe(0);
      expect(d.proficiency).toBe(0);
      expect(typeof d.label).toBe('string');
      expect(d.label.length).toBeGreaterThan(0);
    }

    expect(stats.topCuisines).toHaveLength(0);
    expect(stats.ingredientCloud).toHaveLength(0);
    expect(stats.topTechniques).toHaveLength(0);
    expect(stats.neglectedDisciplines).toHaveLength(4);
    expect(stats.recentAttempts).toHaveLength(0);
  });

  it('counts discipline frequency correctly from sample attempts', () => {
    const attempts: UserAttempt[] = [
      makeAttempt({ id: 'a1', techniques: ['julienne', 'brunoise'], score: 8 }),
      makeAttempt({ id: 'a2', techniques: ['julienne'], score: 6 }),
      makeAttempt({ id: 'a3', techniques: ['sauteren'], score: 7 }),
      makeAttempt({ id: 'a4', techniques: ['roux', 'hollandaise'], score: 9 }),
    ];

    const stats = computeProfileStats(attempts);

    const mechanical = stats.disciplines.find((d) => d.discipline === 'mechanical')!;
    const thermal = stats.disciplines.find((d) => d.discipline === 'thermal')!;
    const flavor = stats.disciplines.find((d) => d.discipline === 'flavor')!;
    const material = stats.disciplines.find((d) => d.discipline === 'material')!;

    // a1 and a2 have mechanical techniques
    expect(mechanical.frequency).toBe(2);
    expect(mechanical.proficiency).toBeCloseTo(7); // (8 + 6) / 2

    // a3 has thermal technique
    expect(thermal.frequency).toBe(1);
    expect(thermal.proficiency).toBeCloseTo(7);

    // a4 has flavor techniques
    expect(flavor.frequency).toBe(1);
    expect(flavor.proficiency).toBeCloseTo(9);

    // no material techniques used
    expect(material.frequency).toBe(0);
    expect(material.proficiency).toBe(0);
  });

  it('neglectedDisciplines correctly identified when recent and old attempts exist', () => {
    const recent = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString();    // 5 days ago
    const old    = new Date(Date.now() - 40 * 24 * 60 * 60 * 1000).toISOString();   // 40 days ago

    const attempts: UserAttempt[] = [
      makeAttempt({ id: 'r1', techniques: ['julienne'], attempted_at: recent }),   // mechanical — recent
      makeAttempt({ id: 'r2', techniques: ['sauteren'], attempted_at: old }),      // thermal — old
      // flavor and material: no attempts
    ];

    const stats = computeProfileStats(attempts);

    expect(stats.neglectedDisciplines).not.toContain('mechanical');
    expect(stats.neglectedDisciplines).toContain('thermal');
    expect(stats.neglectedDisciplines).toContain('flavor');
    expect(stats.neglectedDisciplines).toContain('material');
  });

  it('topCuisines sorts by count desc and limits to 10', () => {
    const attempts: UserAttempt[] = Array.from({ length: 15 }, (_, i) =>
      makeAttempt({
        id: `a${i}`,
        cuisines: [`cuisine-${i % 12}`],
        taste_score: 7,
      })
    );

    const stats = computeProfileStats(attempts);

    expect(stats.topCuisines.length).toBeLessThanOrEqual(10);
    for (let i = 1; i < stats.topCuisines.length; i++) {
      expect(stats.topCuisines[i - 1].count).toBeGreaterThanOrEqual(
        stats.topCuisines[i].count
      );
    }
  });

  it('ingredientCloud counts and sorts all ingredients', () => {
    const attempts: UserAttempt[] = [
      makeAttempt({ id: 'i1', ingredients: ['wortel', 'ui', 'knoflook'] }),
      makeAttempt({ id: 'i2', ingredients: ['wortel', 'ui'] }),
      makeAttempt({ id: 'i3', ingredients: ['wortel'] }),
    ];

    const stats = computeProfileStats(attempts);

    expect(stats.ingredientCloud[0]).toEqual({ ingredient: 'wortel', count: 3 });
    expect(stats.ingredientCloud[1]).toEqual({ ingredient: 'ui', count: 2 });
    expect(stats.ingredientCloud[2]).toEqual({ ingredient: 'knoflook', count: 1 });
  });

  it('recentAttempts returns at most 20 sorted by attempted_at desc', () => {
    const attempts: UserAttempt[] = Array.from({ length: 25 }, (_, i) =>
      makeAttempt({
        id: `ra${i}`,
        attempted_at: new Date(Date.now() - i * 60 * 1000).toISOString(),
      })
    );

    const stats = computeProfileStats(attempts);

    expect(stats.recentAttempts).toHaveLength(20);
    // should be sorted newest first (ra0 is newest)
    expect(stats.recentAttempts[0].id).toBe('ra0');
    expect(stats.recentAttempts[19].id).toBe('ra19');
  });

  it('Dutch labels are assigned to all disciplines', () => {
    const stats = computeProfileStats([]);
    const labels = stats.disciplines.map((d) => d.label);
    expect(labels).toContain('Mechanisch Meesterschap');
    expect(labels).toContain('Thermische Controle');
    expect(labels).toContain('Smaakcompositie');
    expect(labels).toContain('Materiaalbeheersing');
  });
});
