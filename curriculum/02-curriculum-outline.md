# Phase 2 — Curriculum outline

Status: **reviewed; revised in Phase 3** (2026-11-04). This is the complete outline of the core program: every course
broken into modules and lessons, with objectives, prerequisites, hours, practical sessions, competencies, practical
exercises and assessment lines. It builds on [Phase 1](01-vision-architecture.md) and does not repeat it. Lesson content
and recipes are out of scope; they come in Phases 5–6.

Phase 3 ([`03-validation.md`](03-validation.md)) revised this outline: lessons now give **active hours, practical
sessions and elapsed time** separately; every lesson was re-estimated; `FIR-3` *Barbecue and live fire* and 21 further lessons
were added (33 new lessons and components in all). Phase 3 lists every change (R1–R12); the change log below covers Phase 2 only.

| Stage | File | Courses | Hours |
|---|---|---|---|
| S1 Foundations | [`02-outline/S1-foundations.md`](02-outline/S1-foundations.md) | 10 + `MS-S1` | 200.5 |
| S2 Core techniques | [`02-outline/S2-core-techniques.md`](02-outline/S2-core-techniques.md) | 11 + `MS-S2` | 285 |
| S3 Integration | [`02-outline/S3-integration.md`](02-outline/S3-integration.md) | 13 + `MS-S3` | 396 |
| S4 Advanced | [`02-outline/S4-advanced.md`](02-outline/S4-advanced.md) | 8 + `MS-S4` | 248.5 |
| S5 Mastery and independence | [`02-outline/S5-mastery.md`](02-outline/S5-mastery.md) | 3 + `CAP-5` | 147.5 |
| **Core program** | | **45 + 5 assessments** | **1,277.5** |

In numbers: **227 modules, 559 lessons and components, 722 practical sessions and 272 practical
exercises**. Every competency reaches its Phase 1 target level at every stage, with enough practical sessions behind
every level claimed.

The tables marked *generated* are produced by `tools/validate_curriculum.py` (run it with `--write` after editing an
outline file). CI checks that they are current, and that IDs, prerequisites, hours and competency coverage are
consistent.

---

## Conventions

**Hierarchy.** Stage → course → module → lesson. Practical exercises and the module assessment belong to the module.

| Level | Size | ID | Example |
|---|---|---|---|
| Course | as in Phase 1 §7 (±10 %) | `AAA-n` | `KNF-1` |
| Module | about 1–14 h (milestone and capstone modules up to 30 h) | `<course>.M<n>` | `KNF-1.M3` |
| Lesson | 0.5–6 h of active time: a theory block plus one or more practical sessions | `<module>.L<n>` | `KNF-1.M3.L2` |
| Practical exercise | part of the module hours | `<module>.X<n>` | `KNF-1.M3.X1` |

Numbers are assigned once and never reused. A lesson inserted later gets the next free number; order is the position
in the document. Milestones (`MS-Sn`) and the capstone (`CAP-5`) use the same format, but their rows are
*components* that re-test competencies. They don't count towards coverage.

**Lesson rows.**

| Column | Content |
|---|---|
| ID | The lesson ID. |
| Lesson | Short title. FR/NL terms are in italics where the learner will meet them. |
| Objectives | What the learner will be able to do, tagged **[K]** knowledge, **[S]** skill, **[J]** judgment, **[M]** mastery. |
| Prereqs | Lesson or course IDs the lesson builds on, besides the previous lesson in the same module (which is always assumed). "—" means nothing beyond that. |
| h | Estimated **active** hours: studying, cooking, tasting and writing up. Waiting time is not counted. |
| Practice | The number of separate practical sessions in the lesson: cooking, tasting, experiments or applied tasks (0 for a theory-only lesson). |
| Elapsed | Calendar time the lesson needs beyond its active hours, when that matters for planning: a stock simmering (`7 h`), an overnight rest (`1 d`), a ferment or a series of drills (`4 wk`). "—" means none. |
| Moves | The competencies the lesson develops and the level the learner can show after it, e.g. `TS-KNF→3`. |

**Exercises.** Each is tagged *type, support*:
- **Type**, from Phase 1 §9.1: drill, comparison, guided (a guided dish), framework (a framework dish), brief, project.
- **Support**, from Phase 1 §6.6: recipe (full instructions), framework (ratio or outline; the learner fills in
  details), brief (goal, constraints and criteria only).

**Assessment lines** name the module check: a quiz, evidence, or both. The evidence types are those of Phase 1 §10.1,
including household tasters and AI review of photos and video.

**Coverage rule.** By the end of each stage, the lessons must reach every competency's Phase 1 §5.3 target level. A
milestone checks that the level was actually *demonstrated*; the lessons are where it is *taught*.

**Practice rule** (Phase 3 §4.3). A level only counts as taught when enough practical sessions stand behind it, summed
over every lesson that moves the competency: level 2 needs 1, level 3 needs 3, level 4 needs 5 and level 5 needs 8.
These are the Phase 1 §10.3 demonstrations (2, 3 and 5) plus the sessions in which the step is learned.

---

## Course summary (generated)

<!-- BEGIN generated:summary -->
| Stage | Course | Title | Modules | Lessons | Practical sessions | Budget h | Outline h | Drift |
|---|---|---|---|---|---|---|---|---|
| S1 | `ORI-1` | Orientation and baseline | 3 | 7 | 5 | 10.5 | 10.5 | ±0 |
| S1 | `SAF-1` | Food safety and hygiene I | 4 | 10 | 9 | 15.5 | 15.5 | ±0 |
| S1 | `WRK-1` | Mise en place, workflow and recipe literacy | 4 | 11 | 10 | 16 | 16 | ±0 |
| S1 | `EQP-1` | Equipment, heat sources and tools | 3 | 9 | 6 | 12 | 12 | ±0 |
| S1 | `KNF-1` | Knife skills I | 5 | 15 | 28 | 28.5 | 28.5 | ±0 |
| S1 | `TST-1` | Tasting and seasoning I | 4 | 13 | 14 | 17.5 | 17.5 | ±0 |
| S1 | `HEA-1` | Heat and cooking methods I | 6 | 17 | 18 | 30.5 | 30.5 | ±0 |
| S1 | `EGG-1` | Eggs and breakfast cookery | 5 | 11 | 13 | 18.5 | 18.5 | ±0 |
| S1 | `VEG-1` | Vegetables I | 6 | 15 | 18 | 27.5 | 27.5 | ±0 |
| S1 | `STA-1` | Rice, grains and pasta I | 4 | 11 | 11 | 17 | 17 | ±0 |
| S1 | `MS-S1` | Milestone S1 | 1 | 5 | 3 | 7 | 7 | ±0 |
| S2 | `SCI-2` | Food science II | 6 | 11 | 12 | 20.5 | 20.5 | ±0 |
| S2 | `HEA-2` | Cooking methods II | 6 | 12 | 13 | 26.5 | 26.5 | ±0 |
| S2 | `KNF-2` | Knife skills II | 3 | 5 | 9 | 10.5 | 10.5 | ±0 |
| S2 | `STK-2` | Stocks and soups | 5 | 14 | 14 | 27 | 27 | ±0 |
| S2 | `SAU-2` | Sauces I | 7 | 12 | 20 | 28 | 28 | ±0 |
| S2 | `POU-2` | Poultry | 6 | 13 | 16 | 24.5 | 24.5 | ±0 |
| S2 | `MEA-2` | Meat I | 6 | 14 | 14 | 29 | 29 | ±0 |
| S2 | `FSH-2` | Fish and shellfish I | 5 | 12 | 16 | 23.5 | 23.5 | ±0 |
| S2 | `LEG-2` | Legumes, grains II and dairy | 4 | 10 | 13 | 19.5 | 19.5 | ±0 |
| S2 | `BAK-2` | Baking science and pastry I | 6 | 17 | 23 | 37 | 37 | ±0 |
| S2 | `BRD-2` | Bread I | 5 | 11 | 18 | 23 | 23 | ±0 |
| S2 | `MS-S2` | Milestone S2 | 1 | 6 | 3 | 16 | 16 | ±0 |
| S3 | `SAF-3` | Food safety II | 2 | 5 | 3 | 7 | 7 | ±0 |
| S3 | `FLV-3` | Flavour architecture | 6 | 14 | 18 | 30 | 30 | ±0 |
| S3 | `BUT-3` | Home butchery and fabrication | 4 | 9 | 13 | 21.5 | 21.5 | ±0 |
| S3 | `SLW-3` | Braising, slow cooking and sous vide | 5 | 12 | 14 | 25.5 | 25.5 | ±0 |
| S3 | `FIR-3` | Barbecue and live fire | 6 | 12 | 14 | 28.5 | 28.5 | ±0 |
| S3 | `GRD-3` | Cold kitchen (*garde manger*) | 5 | 11 | 15 | 25.5 | 25.5 | ±0 |
| S3 | `PRS-3` | Preservation | 6 | 10 | 13 | 23.5 | 23.5 | ±0 |
| S3 | `VEG-3` | Vegetables II: vegetable-led cooking | 5 | 10 | 16 | 24.5 | 24.5 | ±0 |
| S3 | `PST-3` | Pastry II | 6 | 14 | 23 | 33.5 | 33.5 | ±0 |
| S3 | `BRD-3` | Bread II | 5 | 11 | 20 | 24 | 24 | ±0 |
| S3 | `CUI-3` | World cuisines I | 9 | 34 | 50 | 99 | 99 | ±0 |
| S3 | `KMG-3` | Kitchen management | 5 | 9 | 11 | 19.5 | 19.5 | ±0 |
| S3 | `NUT-3` | Applied nutrition and allergens | 3 | 6 | 9 | 14 | 14 | ±0 |
| S3 | `MS-S3` | Milestone S3 | 1 | 7 | 4 | 20 | 20 | ±0 |
| S4 | `CLS-4` | Classical French repertoire | 6 | 13 | 20 | 35.5 | 35.5 | ±0 |
| S4 | `CUI-4` | World cuisines II: deep dives | 3 | 12 | 18 | 42 | 42 | ±0 |
| S4 | `PRO-4` | Advanced proteins | 6 | 11 | 17 | 31.5 | 31.5 | ±0 |
| S4 | `PST-4` | Advanced pastry and plated desserts | 5 | 11 | 18 | 30 | 30 | ±0 |
| S4 | `MOD-4` | Modern techniques | 5 | 7 | 11 | 17 | 17 | ±0 |
| S4 | `PLT-4` | Presentation and plating | 4 | 6 | 8 | 14 | 14 | ±0 |
| S4 | `RDV-4` | Recipe development | 5 | 10 | 16 | 25 | 25 | ±0 |
| S4 | `MNU-4` | Menu design and multi-course execution | 6 | 14 | 16 | 33.5 | 33.5 | ±0 |
| S4 | `MS-S4` | Milestone S4 | 1 | 7 | 3 | 20 | 20 | ±0 |
| S5 | `IND-5` | Cooking without recipes | 6 | 15 | 29 | 43.5 | 43.5 | ±0 |
| S5 | `RDV-5` | Original recipe portfolio | 3 | 9 | 19 | 38 | 38 | ±0 |
| S5 | `REV-5` | Consolidation | 1 | 4 | 9 | 15 | 15 | ±0 |
| S5 | `CAP-5` | Capstone | 3 | 15 | 9 | 51 | 51 | ±0 |
| | | **Total** | **227** | **559** | **722** | **1277.5** | **1277.5** | |
<!-- END generated:summary -->

## Competency coverage (generated)

Each cell shows *highest level taught by the end of the stage / Phase 1 target*. "–" means no target yet; "·" means
the stage file doesn't exist yet; ✗ marks a miss. The last column gives the practical sessions behind the final level
(see the practice rule).

<!-- BEGIN generated:coverage -->
| Competency | S1 | S2 | S3 | S4 | S5 | Practical sessions |
|---|---|---|---|---|---|---|
| `CK-TERM` | 2 / 2 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 6 (level 4 needs 5) |
| `CK-METH` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 9 (level 5 needs 8) |
| `CK-RCPT` | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 5 (level 4 needs 5) |
| `CK-CLAS` | – | 2 / 2 | 2 / 2 | 4 / 4 | 4 / 4 | 12 (level 4 needs 5) |
| `CK-CUIS` | – | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 | 40 (level 4 needs 5) |
| `CK-BEVR` | – | – | 1 / 1 | 3 / 3 | 3 / 3 | 5 (level 3 needs 3) |
| `IN-SEL` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 7 (level 4 needs 5) |
| `IN-STO` | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 9 (level 4 needs 5) |
| `IN-VEG` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 15 (level 5 needs 8) |
| `IN-STA` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 9 (level 4 needs 5) |
| `IN-DAI` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 7 (level 4 needs 5) |
| `IN-MEA` | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 9 (level 4 needs 5) |
| `IN-FSH` | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 9 (level 4 needs 5) |
| `IN-ARO` | 1 / 1 | 2 / 2 | 4 / 4 | 4 / 4 | 4 / 4 | 12 (level 4 needs 5) |
| `IN-BAK` | – | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 8 (level 4 needs 5) |
| `TS-MEP` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 | 8 (level 5 needs 8) |
| `TS-KNF` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 | 34 (level 5 needs 8) |
| `TS-DRY` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 30 (level 5 needs 8) |
| `TS-MOI` | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 4 / 4 | 10 (level 4 needs 5) |
| `TS-CMB` | – | 2 / 2 | 4 / 4 | 4 / 4 | 4 / 4 | 10 (level 4 needs 5) |
| `TS-FIRE` | – | – | 3 / 3 | 3 / 3 | 4 / 4 | 15 (level 4 needs 5) |
| `TS-EGG` | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 4 / 4 | 19 (level 4 needs 5) |
| `TS-VEG` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 | 32 (level 5 needs 8) |
| `TS-STA` | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 26 (level 4 needs 5) |
| `TS-STK` | – | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 13 (level 4 needs 5) |
| `TS-SAU` | – | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 36 (level 5 needs 8) |
| `TS-PRO` | 1 / 1 | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 37 (level 5 needs 8) |
| `TS-FAB` | – | 2 / 2 | 3 / 3 | 3 / 3 | 3 / 3 | 21 (level 3 needs 3) |
| `TS-BAK` | – | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 43 (level 4 needs 5) |
| `TS-BRD` | – | 2 / 2 | 3 / 3 | 3 / 3 | 3 / 3 | 26 (level 3 needs 3) |
| `TS-GRD` | – | – | 3 / 3 | 3 / 3 | 3 / 3 | 8 (level 3 needs 3) |
| `TS-PRS` | – | – | 3 / 3 | 3 / 3 | 3 / 3 | 11 (level 3 needs 3) |
| `TS-MOD` | – | – | 1 / 1 | 2 / 2 | 2 / 2 | 9 (level 2 needs 1) |
| `TS-PLT` | 1 / 1 | 2 / 2 | 2 / 2 | 4 / 4 | 4 / 4 | 14 (level 4 needs 5) |
| `SC-HEAT` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 11 (level 4 needs 5) |
| `SC-BRWN` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 11 (level 4 needs 5) |
| `SC-PROT` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 9 (level 4 needs 5) |
| `SC-STRC` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 8 (level 4 needs 5) |
| `SC-EMUL` | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 18 (level 4 needs 5) |
| `SC-MICR` | 1 / 1 | 3 / 3 | 3 / 3 | 3 / 3 | 3 / 3 | 4 (level 3 needs 3) |
| `FL-SENS` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 13 (level 5 needs 8) |
| `FL-SEAS` | 2 / 2 | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 20 (level 5 needs 8) |
| `FL-TEXT` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 17 (level 4 needs 5) |
| `FL-ARCH` | – | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 36 (level 4 needs 5) |
| `FL-CORR` | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 10 (level 5 needs 8) |
| `KM-PLAN` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 20 (level 5 needs 8) |
| `KM-MATH` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 8 (level 4 needs 5) |
| `KM-PURC` | 1 / 1 | 2 / 2 | 4 / 4 | 4 / 4 | 4 / 4 | 9 (level 4 needs 5) |
| `KM-WAST` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 12 (level 4 needs 5) |
| `KM-EQPT` | 2 / 2 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 19 (level 4 needs 5) |
| `SA-HYG` | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 5 / 5 | 12 (level 5 needs 8) |
| `SA-TEMP` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 | 11 (level 5 needs 8) |
| `SA-PHYS` | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 5 / 5 | 9 (level 5 needs 8) |
| `SA-ALLG` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 7 (level 4 needs 5) |
| `SA-HRPR` | – | 1 / 1 | 4 / 4 | 4 / 4 | 4 / 4 | 5 (level 4 needs 5) |
| `RD-RATO` | – | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 19 (level 4 needs 5) |
| `RD-ADPT` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 5 / 5 | 23 (level 5 needs 8) |
| `RD-TEST` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 17 (level 4 needs 5) |
| `RD-WRIT` | 2 / 2 | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 10 (level 4 needs 5) |
| `JD-CUES` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 24 (level 5 needs 8) |
| `JD-CHSE` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 5 / 5 | 16 (level 5 needs 8) |
| `JD-TRBL` | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 14 (level 5 needs 8) |
| `JD-REFL` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 26 (level 5 needs 8) |
| `CR-COMP` | – | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 | 18 (level 4 needs 5) |
| `CR-MENU` | – | – | 2 / 2 | 4 / 4 | 4 / 4 | 5 (level 4 needs 5) |
| `CR-IMPV` | – | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 13 (level 4 needs 5) |
| `CR-ORIG` | – | – | 1 / 1 | 3 / 3 | 4 / 4 | 24 (level 4 needs 5) |
<!-- END generated:coverage -->

## Support mix (generated)

Share of practical exercises per support level, against Phase 1 §6.6.

<!-- BEGIN generated:support -->
| Stage | Exercises | Recipe | Framework | Brief | Phase 1 §6.6 target |
|---|---|---|---|---|---|
| S1 | 58 | 78 % | 22 % | 0 % | 70 / 30 / 0 % |
| S2 | 71 | 42 % | 48 % | 10 % | 40 / 50 / 10 % |
| S3 | 85 | 14 % | 51 % | 35 % | 15 / 50 / 35 % |
| S4 | 47 | 6 % | 32 % | 62 % | 5 / 35 / 60 % |
| S5 | 11 | 0 % | 9 % | 91 % | 0 / 10 / 90 % |
<!-- END generated:support -->

## Weekly load (generated)

Active hours per stage and how long they take at the weekly study times of Phase 1 A2, over 46 active weeks a year.
Lessons with a long elapsed time (a day or more) need planning across days; they are counted in the fifth column.

<!-- BEGIN generated:load -->
| Stage | Lessons | Active h | Practical sessions | Lessons with ≥ 1 d elapsed | Weeks at 5 h | Weeks at 6.5 h | Weeks at 8 h |
|---|---|---|---|---|---|---|---|
| S1 | 124 | 200.5 | 135 | 4 | 40 | 31 | 25 |
| S2 | 137 | 285 | 171 | 7 | 57 | 44 | 36 |
| S3 | 164 | 396 | 223 | 22 | 79 | 61 | 50 |
| S4 | 91 | 248.5 | 127 | 14 | 50 | 38 | 31 |
| S5 | 43 | 147.5 | 66 | 13 | 30 | 23 | 18 |
| **Core** | | **1277.5** | | | **256** (≈ 5.6 yr) | **197** (≈ 4.3 yr) | **160** (≈ 3.5 yr) |
<!-- END generated:load -->

---

## Subject coverage

This is the "nothing overlooked" check. It maps every subject in the original brief, and every subject Phase 1 §4.1
added, to the modules that teach it.

**Subjects from the brief**

| Subject | Where it is taught |
|---|---|
| Culinary fundamentals | `WRK-1`, `HEA-1`, `KNF-1`; history in `CUI-3.M1`, `CLS-4.M1`, `CLS-4.M6` |
| Kitchen organization | `WRK-1.M1`, `KMG-3.M1`, `MNU-4.M3` |
| Equipment | `EQP-1`, `KNF-2.M3`, `MOD-4.M2`, `MNU-4.M3.L2` |
| Knife skills | `KNF-1`, `KNF-2`, `BUT-3`, `CLS-4.M2` |
| Ingredients (selection, quality, storage, seasonality) | `VEG-1.M2`, `MEA-2.M2`, `FSH-2.M1`, `FLV-3.M2.L3`, `KMG-3.M2`, `VEG-3.M1` |
| Vegetables and fruits | `VEG-1`, `VEG-3`; fruit in `PRS-3.M5`, `PST-3.M3.L1`, `PST-4.M2.L3` |
| Meat (beef, pork, lamb, game, poultry) | `MEA-2`, `POU-2`, `BUT-3.M1`, `BUT-3.M3`, `SLW-3`, `PRO-4.M1`–`M3` |
| Fish and seafood | `FSH-2`, `BUT-3.M2`, `GRD-3.M3`, `PRO-4.M4`–`M5` |
| Eggs and dairy | `EGG-1`, `LEG-2.M3`, `BAK-2.M5`, `SAU-2.M5`, `GRD-3.M5.L1` |
| Herbs and spices | `STK-2.M1`, `FLV-3.M1`–`M2` |
| Stocks and broths | `STK-2.M2`, `CLS-4.M4`, `CUI-3.M8.L1` (*dashi*) |
| Sauces | `SAU-2`, `CLS-4.M3`, `GRD-3.M2`, `SLW-3.M3.L2`, `FIR-3.M4.L3` |
| Soups | `STK-2.M3`–`M5` |
| Cooking methods | `HEA-1`, `HEA-2`, `SLW-3`, `FIR-3` |
| Food science | `SCI-2`, plus the science thread below |
| Baking and pastry | `BAK-2` (buttercream and cake finishing in `BAK-2.M4.L4`), `PST-3`, `PST-4` |
| Breadmaking | `BRD-2`, `BRD-3` |
| Pasta and grains | `STA-1`, `LEG-2`, `CUI-3.M3.L2` (fresh pasta), `CUI-3.M3.L3` (filled pasta, *gnocchi*), `CUI-3.M4.L2`, `CUI-3.M7.L3` (dumplings), `CUI-3.M9.L3` (tortillas) |
| Classical cuisine | `SAU-2.M1`, `CLS-4` |
| International cuisine | `CUI-3`, `CUI-4` |
| Flavour development | `TST-1`, `FLV-3` |
| Recipe development | `SCI-2.M1`, `RDV-4`, `RDV-5` |
| Menu planning | `KMG-3.M1`, `MNU-4` |
| Presentation | `PLT-4`, `PST-4.M5` |
| Nutrition | `NUT-3` |
| Preservation | `SAF-3`, `PRS-3`, `GRD-3.M3` |
| Kitchen management | `KMG-3`, `MNU-4.M2`–`M3` |
| Food economics | `KMG-3.M2`–`M3`, `BUT-3.M4` |
| Advanced cooking | Stage S4: `CLS-4`, `PRO-4`, `PST-4`, `MOD-4` |
| Food safety | `SAF-1`, `SAF-3`, and a safety lesson in every high-risk course (see the threads table) |

**Subjects added in Phase 1 §4.1**

| Subject | Where it is taught |
|---|---|
| Sensory evaluation and tasting | `TST-1`, `FLV-3.M6`, `IND-5.M4.L2`, `IND-5.M6.L2` |
| Culinary math | `WRK-1.M3`, `BAK-2.M1`, `BUT-3.M4`, `KMG-3.M3`, `RDV-4.M1` |
| Recipe literacy | `WRK-1.M2`, `CUI-3.M1.L2` |
| Kitchen terminology | `WRK-1.M4.L1`, `STK-2.M1.L1`, `SAU-2.M1`, `CLS-4.M1` |
| Knowing your own equipment | `EQP-1.M2`, `MNU-4.M3.L2` |
| Deep-frying and oil safety | `HEA-2.M4` |
| Breakfast and egg cookery | `EGG-1` |
| Sous vide | `SLW-3.M5`, `MOD-4.M2` |
| Allergens and dietary adaptation | `SAF-1.M4`, `NUT-3.M2`–`M3`, `IND-5.M5.L3` |
| Leftovers and batch cooking | `KMG-3.M1.L2`, `KMG-3.M4`, `IND-5.M3` |
| Kitchen journal and reflection | `ORI-1.M1.L2`, and the journal thread below |
| Hosting and serving at home | `MNU-4.M5`; finger food in `GRD-3.M5.L3` |
| Barbecue and live fire (added in Phase 3) | `FIR-3` |
| Sandwiches, toasts and canapés (added in Phase 3) | `GRD-3.M5.L3` |
| Beverage basics and food–drink pairing | `MNU-4.M4` (required) |
| Producer and seasonal knowledge | `VEG-1.M2`, `VEG-3.M1`, `KMG-3.M2.L2` |

**Benchmark subjects with thin coverage, decided in Phase 3** (§2):
- *Barbecue and live fire*: **added to the core** as `FIR-3`.
- *Cuisines of the Americas* (the CIA has a whole course [S1]; `CUI-3` covers Mexico), *smoking* as a preservation
  method (Ballymaloe [S6]; wood smoke as a flavour is now in `FIR-3.M3.L2`) and *cheesemaking beyond a fresh cheese*
  (`LEG-2.M3.L3`): **left as they are**, outside the core.

## Thread placement

The Phase 1 §6.5 threads, as they appear in the outline:

| Thread | Lessons and components |
|---|---|
| Food safety | `SAF-1`, `SAF-3`; `FIR-3.M1.L1`, `FIR-3.M4.L1`, `HEA-2.M4.L1`, `POU-2.M1.L2`, `MEA-2.M5.L1`, `FSH-2.M4.L1`, `GRD-3.M3.L1`, `PRS-3.M1`, `SLW-3.M5.L2`, `NUT-3.M3`, `MNU-4.M3.L3`; re-tested in every milestone, where a failed item can't be compensated |
| Tasting and sensory | `TST-1`; `STK-2.M3.L2`, `FLV-3.M6.L2`, `MNU-4.M4.L1`, `IND-5.M4.L2`, `IND-5.M6.L2`; calibration in `MS-S1.M1.L3` and `MS-S3.M1.L4` |
| Kitchen journal | `ORI-1.M1.L2`; `WRK-1.M4.L2`, `BAK-2.M6.L2`, `BRD-2.M5.L1`, `KMG-3.M5.L1`, `PLT-4.M4.L1`, `MNU-4.M5.L2`, `RDV-5.M3.L1`, `REV-5.M1.L1`; a reflection in every milestone |
| Knife repetition | `KNF-1.M5`, `KNF-2.M1`, `BUT-3`, `CLS-4.M2`; re-tested in `MS-S1.M1.L1`, `MS-S2.M1.L4`, `MS-S4.M1.L7` |
| Food science | `HEA-1.M1`–`M2`, `EGG-1.M1`, `VEG-1.M1`, `VEG-1.M3`, `STA-1.M1`, `SCI-2`, `SLW-3.M1`, `PST-3.M2.L1`, `PST-4.M2.L1`, `MOD-4` |
| Terminology | `WRK-1.M4.L1`, `STK-2.M1.L1`, `SAU-2.M1`, `CLS-4.M1`; the glossary grows with every course |

## Change log against Phase 1

Phase 2 changed no stage, course, competency or hour budget. It made the following refinements. (Phase 3's changes,
including new hour budgets, are logged in [`03-validation.md`](03-validation.md) §6.)

| # | Change | Reason |
|---|---|---|
| C1 | Course prerequisites made exact. S1: `EQP-1` needs `SAF-1`, `HEA-1` needs `KNF-1`, `EGG-1` needs `WRK-1`, `STA-1` needs `TST-1`. S2: `HEA-2` needs `SCI-2`; `SAU-2` needs `SCI-2` and `STK-2`; `POU-2` and `FSH-2` need `KNF-2` and `SAU-2`; `MEA-2` needs `STK-2`; `LEG-2` needs `SCI-2` and `SAU-2`. S3: `GRD-3` needs `SAF-3`; `KMG-3` needs `SAF-3` and `BUT-3`; `NUT-3` needs `VEG-3` and `KMG-3`. | Lessons build on lessons in those courses. Phase 1's §6.4 graph showed the main dependencies; the validator now enforces the exact ones. |
| C2 | S3 order: `SAF-3` first. | Phase 1 §7.3 listed it after `SLW-3`, which needs it. |
| C3 | `CUI-4` gets a 10-hour deep-dive template, used three times. | The traditions are the learner's choice; Phase 5 writes a deep dive for each available tradition. |
| C4 | Fresh pasta is placed in `CUI-3.M3.L2`. | Phase 1's `TS-STA` includes fresh pasta, but no course purpose named it. |
| C5 | Milestones and the capstone are broken into components, with hours. | Needed for planning. The briefs and rubrics remain Phase 4. |
| C6 | No competency is introduced earlier than Phase 1 §5.3 expects. The coverage table shows the levels taught, which sometimes exceed the stage target (e.g. `TS-EGG` 4 in S2). | Recorded for Phase 3: a lesson teaching beyond its stage target is fine; a milestone still checks only the target. |

---

## End of phase

### Decisions and assumptions
1. **Granularity.**
   - Lessons are 0.5–4 h, averaging about 1.9 h: a theory block plus practice, usually one or two cooking sessions.
     *Phase 3*: 0.5–6 h of active time, averaging about 2.2 h, with practical sessions counted separately.
   - Modules are 1–10 h.
   - In total: 221 modules and 526 lessons and components.
2. **Hours.** Every course kept its Phase 1 hours exactly, so the core stayed at 1,020 h. *Superseded in Phase 3*: the
   hours were re-estimated lesson by lesson, and the core is now 1,277.5 h.
3. **Coverage is taught, then demonstrated.** Each lesson states the level it teaches a competency to. Milestones re-test
   competencies but don't count towards coverage, so no target depends on an exam alone.
4. **Support mix.** Each stage's mix of recipe, framework and brief exercises is within about 10 points of Phase 1 §6.6
   (see *Support mix*).
5. **Evidence.** Modules name concrete evidence as Phase 1 §10.1 allows: photos for AI review, logs, household
   tasters, quizzes. A safety item failed anywhere means a resit.
6. **Validator.** `tools/validate_curriculum.py` keeps the outline consistent through later phases and runs in CI.

### Gaps and open questions for Phase 3

*Answered on 2026-11-04 and handled in [Phase 3](03-validation.md): barbecue added, the other thin subjects left as
they are; active and elapsed time separated; hours made realistic, with no ceiling.*

- **Thin benchmark subjects:** cuisines of the Americas, smoking, cheesemaking, barbecue and live fire (see *Subject
  coverage*). Each needs a decision: add it to the core, or leave it to a specialization.
- **Thinly taught competencies.** Some competencies are taught in only 4–5 lessons: `CK-RCPT`, `CR-MENU`, `SA-ALLG`,
  `RD-WRIT`, `FL-CORR`, `TS-MOD` and `CK-BEVR`. Most are reinforced by exercises and milestones, but Phase 3 should
  check that each has enough practice to reach its target in the evidence terms of Phase 1 §10.3.
- **Realistic lesson time.** Some lessons fold long unattended time into their hours: stocks, braises, sourdough,
  ferments and cures. Phase 3 should separate *active* hours from *elapsed* time, so that weekly planning is realistic.
- **`TS-KNF` and `TS-VEG` at level 5.** Both reach level 5 in a single S4 module (`CLS-4.M2`). Mastery evidence will
  have to come from repetition in S5 and the capstone. Phase 3 should check that this is enough.
- **Variable deep dives.** `CUI-4`'s traditions are the learner's choice, so its coverage claims assume a full deep
  dive whichever traditions are picked.

### Recommendations for Phase 3
1. Compare the outline chapter by chapter with *The Professional Chef* [S8] and Gisslen [S10], and work process by
   work process with the MBO *Keuken* qualification [S7]. Record each gap as *add*, *specialization* or *out of scope*.
2. Decide on the thin benchmark subjects above. If any are added to the core, rebalance hours instead of growing the
   total again, unless you choose to grow it.
3. Check every lesson's prerequisites for *hidden* dependencies: techniques used in exercises but not named in the
   prerequisites. Extend the validator if a pattern emerges.
4. Add an "active hours / elapsed time" field to lessons, and check the weekly load against 5–8 h.
5. Review the overlap between `KNF-1.M5`/`KNF-2.M1` and between `STK-2.M3`/`CLS-4.M4`, and decide whether each is
   repetition by design (spiral) or redundancy.

