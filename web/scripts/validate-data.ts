/**
 * Build-time integrity check for VEG_DATA.
 * Run with: npm run validate
 * Fails with exit code 1 if any vegetable entry is malformed.
 */

import { VEG_DATA } from "../lib/vegetables";

const VALID_FAMILIES = new Set([
  "Wortel", "Blad", "Brassica", "Allium", "Peulvrucht", "Stengel",
  "Pompoen", "Knolgewas", "Vruchtgroente", "Paddenstoel", "Kruid",
]);

const HEX_RE = /^#[0-9A-Fa-f]{6}$/;

let errors = 0;

function fail(id: string, msg: string) {
  console.error(`  [${id}] ${msg}`);
  errors++;
}

for (const v of VEG_DATA) {
  if (!v.id || !/^[a-z0-9-]+$/.test(v.id))
    fail(v.id ?? "?", "id must be kebab-case");

  if (!v.name)
    fail(v.id, "name is required");

  if (!v.nl)
    fail(v.id, "nl is required");

  if (!HEX_RE.test(v.color))
    fail(v.id, `color "${v.color}" is not a valid 6-digit hex`);

  if (!HEX_RE.test(v.accent))
    fail(v.id, `accent "${v.accent}" is not a valid 6-digit hex`);

  if (!VALID_FAMILIES.has(v.family))
    fail(v.id, `family "${v.family}" is not in the allowed list`);

  for (const arr of [v.peak, v.shoulder ?? []]) {
    for (const m of arr) {
      if (m < 1 || m > 12)
        fail(v.id, `month ${m} is out of range (1–12)`);
    }
  }

  if (!v.blurb)  fail(v.id, "blurb is required");
  if (!v.storage) fail(v.id, "storage is required");
  if (!v.prep)   fail(v.id, "prep is required");

  if (!v.pairings || v.pairings.length < 2)
    fail(v.id, "pairings must have at least 2 entries");

  if (!v.substitutions || v.substitutions.length < 1)
    fail(v.id, "substitutions must have at least 1 entry");
}

if (errors > 0) {
  console.error(`\n${errors} error(s) found in VEG_DATA. Fix them before building.\n`);
  process.exit(1);
} else {
  console.log(`VEG_DATA OK — ${VEG_DATA.length} vegetables validated.`);
}
