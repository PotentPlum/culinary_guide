import { describe, it, expect } from "vitest";
import { TECHNIQUES } from "./techniques";
import { VEG_DATA } from "./vegetables";

describe("TECHNIQUES data integrity", () => {
  const ids = new Set(TECHNIQUES.map(t => t.id));
  const vegNames = new Set(VEG_DATA.map(v => v.name));

  it("has at least 40 entries", () => {
    expect(TECHNIQUES.length).toBeGreaterThanOrEqual(40);
  });

  it("all entries have non-empty body (2+ paragraphs) and tips (3+)", () => {
    for (const t of TECHNIQUES) {
      expect(t.body.length, `${t.id} body`).toBeGreaterThanOrEqual(2);
      expect(t.tips.length, `${t.id} tips`).toBeGreaterThanOrEqual(3);
      for (const p of t.body) expect(p.trim().length, `${t.id} body paragraph`).toBeGreaterThan(0);
      for (const tip of t.tips) expect(tip.trim().length, `${t.id} tip`).toBeGreaterThan(0);
    }
  });

  it("all relatedVeg values exist in VEG_DATA", () => {
    for (const t of TECHNIQUES) {
      for (const vegName of t.relatedVeg ?? []) {
        expect(vegNames.has(vegName), `${t.id}: unknown veg "${vegName}"`).toBe(true);
      }
    }
  });

  it("all relatedTechniqueIds reference valid technique ids", () => {
    for (const t of TECHNIQUES) {
      for (const refId of t.relatedTechniqueIds ?? []) {
        expect(ids.has(refId), `${t.id}: unknown technique id "${refId}"`).toBe(true);
      }
    }
  });

  it("all ids are unique", () => {
    expect(ids.size).toBe(TECHNIQUES.length);
  });
});
