# Phase 2 — Curriculum outline

Status: **ready for review** (2026-11-04). This is the complete outline of the core program: every course broken into
modules and lessons, with objectives, prerequisites, hours, competencies, practical exercises and assessment lines. It
builds on [Phase 1](01-vision-architecture.md) and does not repeat it. Lesson content and recipes are out of scope;
they come in Phases 5–6.

| Stage | File | Courses | Hours |
|---|---|---|---|
| S1 Foundations | [`02-outline/S1-foundations.md`](02-outline/S1-foundations.md) | 10 + `MS-S1` | 170 |
| S2 Core techniques | [`02-outline/S2-core-techniques.md`](02-outline/S2-core-techniques.md) | 11 + `MS-S2` | 260 |
| S3 Integration | [`02-outline/S3-integration.md`](02-outline/S3-integration.md) | 12 + `MS-S3` | 270 |
| S4 Advanced | [`02-outline/S4-advanced.md`](02-outline/S4-advanced.md) | 8 + `MS-S4` | 210 |
| S5 Mastery and independence | [`02-outline/S5-mastery.md`](02-outline/S5-mastery.md) | 3 + `CAP-5` | 110 |
| **Core program** | | **44 + 5 assessments** | **1,020** |

In numbers: **221 modules, 526 lessons and components, and 255 practical exercises**. Every competency reaches its
Phase 1 target level at every stage, and every course keeps its Phase 1 hours exactly.

The tables marked *generated* are produced by `tools/validate_curriculum.py` (run it with `--write` after editing an
outline file). CI checks that they are current, and that IDs, prerequisites, hours and competency coverage are
consistent.

---

## Conventions

**Hierarchy.** Stage → course → module → lesson. Practical exercises and the module assessment belong to the module.

| Level | Size | ID | Example |
|---|---|---|---|
| Course | as in Phase 1 §7 (±10 %) | `AAA-n` | `KNF-1` |
| Module | about 1–10 h (milestone and capstone modules up to 20 h) | `<course>.M<n>` | `KNF-1.M3` |
| Lesson | 0.5–4 h: a theory block plus practice | `<module>.L<n>` | `KNF-1.M3.L2` |
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
| h | Estimated hours, including practice. |
| Moves | The competencies the lesson develops and the level the learner can show after it, e.g. `TS-KNF→3`. |

**Exercises.** Each is tagged *type, support*:
- **Type**, from Phase 1 §9.1: drill, comparison, guided (a guided dish), framework (a framework dish), brief, project.
- **Support**, from Phase 1 §6.6: recipe (full instructions), framework (ratio or outline; the learner fills in
  details), brief (goal, constraints and criteria only).

**Assessment lines** name the module check: a quiz, evidence, or both. The evidence types are those of Phase 1 §10.1,
including household tasters and AI review of photos and video.

**Coverage rule.** By the end of each stage, the lessons must reach every competency's Phase 1 §5.3 target level. A
milestone checks that the level was actually *demonstrated*; the lessons are where it is *taught*.

---

## Course summary (generated)

<!-- BEGIN generated:summary -->
| Stage | Course | Title | Modules | Lessons | Phase 1 h | Phase 2 h | Drift |
|---|---|---|---|---|---|---|---|
| S1 | `ORI-1` | Orientation and baseline | 3 | 7 | 8 | 8 | ±0 |
| S1 | `SAF-1` | Food safety and hygiene I | 4 | 10 | 12 | 12 | ±0 |
| S1 | `WRK-1` | Mise en place, workflow and recipe literacy | 4 | 11 | 14 | 14 | ±0 |
| S1 | `EQP-1` | Equipment, heat sources and tools | 3 | 9 | 12 | 12 | ±0 |
| S1 | `KNF-1` | Knife skills I | 5 | 15 | 22 | 22 | ±0 |
| S1 | `TST-1` | Tasting and seasoning I | 4 | 13 | 16 | 16 | ±0 |
| S1 | `HEA-1` | Heat and cooking methods I | 6 | 17 | 26 | 26 | ±0 |
| S1 | `EGG-1` | Eggs and breakfast cookery | 5 | 11 | 16 | 16 | ±0 |
| S1 | `VEG-1` | Vegetables I | 6 | 15 | 24 | 24 | ±0 |
| S1 | `STA-1` | Rice, grains and pasta I | 4 | 11 | 14 | 14 | ±0 |
| S1 | `MS-S1` | Milestone S1 | 1 | 4 | 6 | 6 | ±0 |
| S2 | `SCI-2` | Food science II | 6 | 11 | 20 | 20 | ±0 |
| S2 | `HEA-2` | Cooking methods II | 6 | 12 | 22 | 22 | ±0 |
| S2 | `KNF-2` | Knife skills II | 3 | 5 | 8 | 8 | ±0 |
| S2 | `STK-2` | Stocks and soups | 5 | 14 | 24 | 24 | ±0 |
| S2 | `SAU-2` | Sauces I | 7 | 12 | 26 | 26 | ±0 |
| S2 | `POU-2` | Poultry | 6 | 13 | 22 | 22 | ±0 |
| S2 | `MEA-2` | Meat I | 6 | 14 | 28 | 28 | ±0 |
| S2 | `FSH-2` | Fish and shellfish I | 5 | 12 | 24 | 24 | ±0 |
| S2 | `LEG-2` | Legumes, grains II and dairy | 4 | 10 | 18 | 18 | ±0 |
| S2 | `BAK-2` | Baking science and pastry I | 6 | 16 | 30 | 30 | ±0 |
| S2 | `BRD-2` | Bread I | 5 | 11 | 22 | 22 | ±0 |
| S2 | `MS-S2` | Milestone S2 | 1 | 6 | 16 | 16 | ±0 |
| S3 | `SAF-3` | Food safety II | 2 | 5 | 6 | 6 | ±0 |
| S3 | `FLV-3` | Flavour architecture | 6 | 14 | 26 | 26 | ±0 |
| S3 | `BUT-3` | Home butchery and fabrication | 4 | 9 | 18 | 18 | ±0 |
| S3 | `SLW-3` | Braising, slow cooking and sous vide | 5 | 12 | 22 | 22 | ±0 |
| S3 | `GRD-3` | Cold kitchen (*garde manger*) | 5 | 10 | 22 | 22 | ±0 |
| S3 | `PRS-3` | Preservation | 6 | 10 | 22 | 22 | ±0 |
| S3 | `VEG-3` | Vegetables II: vegetable-led cooking | 5 | 10 | 20 | 20 | ±0 |
| S3 | `PST-3` | Pastry II | 6 | 14 | 30 | 30 | ±0 |
| S3 | `BRD-3` | Bread II | 5 | 11 | 20 | 20 | ±0 |
| S3 | `CUI-3` | World cuisines I | 9 | 18 | 34 | 34 | ±0 |
| S3 | `KMG-3` | Kitchen management | 5 | 9 | 18 | 18 | ±0 |
| S3 | `NUT-3` | Applied nutrition and allergens | 3 | 6 | 12 | 12 | ±0 |
| S3 | `MS-S3` | Milestone S3 | 1 | 7 | 20 | 20 | ±0 |
| S4 | `CLS-4` | Classical French repertoire | 6 | 13 | 30 | 30 | ±0 |
| S4 | `CUI-4` | World cuisines II: deep dives | 3 | 12 | 30 | 30 | ±0 |
| S4 | `PRO-4` | Advanced proteins | 6 | 11 | 24 | 24 | ±0 |
| S4 | `PST-4` | Advanced pastry and plated desserts | 5 | 11 | 26 | 26 | ±0 |
| S4 | `MOD-4` | Modern techniques | 5 | 7 | 16 | 16 | ±0 |
| S4 | `PLT-4` | Presentation and plating | 4 | 6 | 14 | 14 | ±0 |
| S4 | `RDV-4` | Recipe development | 5 | 10 | 22 | 22 | ±0 |
| S4 | `MNU-4` | Menu design and multi-course execution | 6 | 14 | 28 | 28 | ±0 |
| S4 | `MS-S4` | Milestone S4 | 1 | 7 | 20 | 20 | ±0 |
| S5 | `IND-5` | Cooking without recipes | 6 | 15 | 40 | 40 | ±0 |
| S5 | `RDV-5` | Original recipe portfolio | 3 | 9 | 24 | 24 | ±0 |
| S5 | `REV-5` | Consolidation | 1 | 3 | 6 | 6 | ±0 |
| S5 | `CAP-5` | Capstone | 3 | 14 | 40 | 40 | ±0 |
| | | **Total** | **221** | **526** | **1020** | **1020** | |
<!-- END generated:summary -->

## Competency coverage (generated)

Each cell shows *highest level taught by the end of the stage / Phase 1 target*. "–" means no target yet; "·" means
the stage file doesn't exist yet; ✗ marks a miss.

<!-- BEGIN generated:coverage -->
| Competency | S1 | S2 | S3 | S4 | S5 |
|---|---|---|---|---|---|
| `CK-TERM` | 2 / 2 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 |
| `CK-METH` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `CK-RCPT` | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `CK-CLAS` | – | 2 / 2 | 2 / 2 | 4 / 4 | 4 / 4 |
| `CK-CUIS` | – | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 |
| `CK-BEVR` | – | – | 1 / 1 | 3 / 3 | 3 / 3 |
| `IN-SEL` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `IN-STO` | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `IN-VEG` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `IN-STA` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `IN-DAI` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `IN-MEA` | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 |
| `IN-FSH` | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 |
| `IN-ARO` | 1 / 1 | 2 / 2 | 4 / 4 | 4 / 4 | 4 / 4 |
| `IN-BAK` | – | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 |
| `TS-MEP` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 |
| `TS-KNF` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 |
| `TS-DRY` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `TS-MOI` | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 4 / 4 |
| `TS-CMB` | – | 2 / 2 | 4 / 4 | 4 / 4 | 4 / 4 |
| `TS-EGG` | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 | 4 / 4 |
| `TS-VEG` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 |
| `TS-STA` | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `TS-STK` | – | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `TS-SAU` | – | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 |
| `TS-PRO` | 1 / 1 | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 |
| `TS-FAB` | – | 2 / 2 | 3 / 3 | 3 / 3 | 3 / 3 |
| `TS-BAK` | – | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `TS-BRD` | – | 2 / 2 | 3 / 3 | 3 / 3 | 3 / 3 |
| `TS-GRD` | – | – | 3 / 3 | 3 / 3 | 3 / 3 |
| `TS-PRS` | – | – | 3 / 3 | 3 / 3 | 3 / 3 |
| `TS-MOD` | – | – | 1 / 1 | 2 / 2 | 2 / 2 |
| `TS-PLT` | 1 / 1 | 2 / 2 | 2 / 2 | 4 / 4 | 4 / 4 |
| `SC-HEAT` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `SC-BRWN` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `SC-PROT` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `SC-STRC` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `SC-EMUL` | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 |
| `SC-MICR` | 1 / 1 | 3 / 3 | 3 / 3 | 3 / 3 | 3 / 3 |
| `FL-SENS` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `FL-SEAS` | 2 / 2 | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 |
| `FL-TEXT` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 |
| `FL-ARCH` | – | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 |
| `FL-CORR` | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `KM-PLAN` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `KM-MATH` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `KM-PURC` | 1 / 1 | 2 / 2 | 4 / 4 | 4 / 4 | 4 / 4 |
| `KM-WAST` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `KM-EQPT` | 2 / 2 | 3 / 3 | 3 / 3 | 4 / 4 | 4 / 4 |
| `SA-HYG` | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 5 / 5 |
| `SA-TEMP` | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 | 5 / 5 |
| `SA-PHYS` | 3 / 3 | 4 / 4 | 5 / 5 | 5 / 5 | 5 / 5 |
| `SA-ALLG` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 4 / 4 |
| `SA-HRPR` | – | 1 / 1 | 4 / 4 | 4 / 4 | 4 / 4 |
| `RD-RATO` | – | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 |
| `RD-ADPT` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 5 / 5 |
| `RD-TEST` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 |
| `RD-WRIT` | 2 / 2 | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 |
| `JD-CUES` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `JD-CHSE` | 1 / 1 | 2 / 2 | 3 / 3 | 4 / 4 | 5 / 5 |
| `JD-TRBL` | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `JD-REFL` | 2 / 2 | 3 / 3 | 4 / 4 | 4 / 4 | 5 / 5 |
| `CR-COMP` | – | 1 / 1 | 3 / 3 | 4 / 4 | 4 / 4 |
| `CR-MENU` | – | – | 2 / 2 | 4 / 4 | 4 / 4 |
| `CR-IMPV` | – | 1 / 1 | 3 / 3 | 3 / 3 | 4 / 4 |
| `CR-ORIG` | – | – | 1 / 1 | 3 / 3 | 4 / 4 |
<!-- END generated:coverage -->

## Support mix (generated)

Share of practical exercises per support level, against Phase 1 §6.6.

<!-- BEGIN generated:support -->
| Stage | Exercises | Recipe | Framework | Brief | Phase 1 §6.6 target |
|---|---|---|---|---|---|
| S1 | 58 | 78 % | 22 % | 0 % | 70 / 30 / 0 % |
| S2 | 70 | 41 % | 49 % | 10 % | 40 / 50 / 10 % |
| S3 | 69 | 16 % | 52 % | 32 % | 15 / 50 / 35 % |
| S4 | 47 | 6 % | 32 % | 62 % | 5 / 35 / 60 % |
| S5 | 11 | 0 % | 9 % | 91 % | 0 / 10 / 90 % |
<!-- END generated:support -->

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
| Sauces | `SAU-2`, `CLS-4.M3`, `GRD-3.M2`, `SLW-3.M3.L2` |
| Soups | `STK-2.M3`–`M5` |
| Cooking methods | `HEA-1`, `HEA-2`, `SLW-3` |
| Food science | `SCI-2`, plus the science thread below |
| Baking and pastry | `BAK-2`, `PST-3`, `PST-4` |
| Breadmaking | `BRD-2`, `BRD-3` |
| Pasta and grains | `STA-1`, `LEG-2`, `CUI-3.M3.L2` (fresh pasta), `CUI-3.M4.L2` |
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
| Hosting and serving at home | `MNU-4.M5` |
| Beverage basics and food–drink pairing | `MNU-4.M4` (required) |
| Producer and seasonal knowledge | `VEG-1.M2`, `VEG-3.M1`, `KMG-3.M2.L2` |

**Benchmark subjects with thin or no coverage** (for Phase 3):
- *Cuisines of the Americas.* The CIA has a whole course on them [S1]; `CUI-3` covers only Mexico.
- *Smoking.* Ballymaloe teaches hot and cold smoking [S6]; nothing here does.
- *Cheesemaking beyond a fresh cheese.* Only `LEG-2.M3.L3` touches it.
- *Barbecue and live fire.* Only `HEA-2.M3` touches it.

## Thread placement

The Phase 1 §6.5 threads, as they appear in the outline:

| Thread | Lessons and components |
|---|---|
| Food safety | `SAF-1`, `SAF-3`; `HEA-2.M4.L1`, `POU-2.M1.L2`, `MEA-2.M5.L1`, `FSH-2.M4.L1`, `GRD-3.M3.L1`, `PRS-3.M1`, `SLW-3.M5.L2`, `NUT-3.M3`, `MNU-4.M3.L3`; re-tested in every milestone, where a failed item can't be compensated |
| Tasting and sensory | `TST-1`; `STK-2.M3.L2`, `FLV-3.M6.L2`, `MNU-4.M4.L1`, `IND-5.M4.L2`, `IND-5.M6.L2`; calibration in `MS-S1.M1.L3` and `MS-S3.M1.L4` |
| Kitchen journal | `ORI-1.M1.L2`; `WRK-1.M4.L2`, `BAK-2.M6.L2`, `BRD-2.M5.L1`, `KMG-3.M5.L1`, `PLT-4.M4.L1`, `MNU-4.M5.L2`, `RDV-5.M3.L1`, `REV-5.M1.L1`; a reflection in every milestone |
| Knife repetition | `KNF-1.M5`, `KNF-2.M1`, `BUT-3`, `CLS-4.M2`; re-tested in `MS-S1.M1.L1`, `MS-S2.M1.L4`, `MS-S4.M1.L7` |
| Food science | `HEA-1.M1`–`M2`, `EGG-1.M1`, `VEG-1.M1`, `VEG-1.M3`, `STA-1.M1`, `SCI-2`, `SLW-3.M1`, `PST-3.M2.L1`, `PST-4.M2.L1`, `MOD-4` |
| Terminology | `WRK-1.M4.L1`, `STK-2.M1.L1`, `SAU-2.M1`, `CLS-4.M1`; the glossary grows with every course |

## Change log against Phase 1

Phase 2 changes no stage, course, competency or hour budget. It does make the following refinements:

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
   - Modules are 1–10 h.
   - In total: 221 modules and 526 lessons and components.
2. **Hours.** Every course keeps its Phase 1 hours exactly, so the stage totals and the 1,020-hour core are unchanged.
3. **Coverage is taught, then demonstrated.** Each lesson states the level it teaches a competency to. Milestones re-test
   competencies but don't count towards coverage, so no target depends on an exam alone.
4. **Support mix.** Each stage's mix of recipe, framework and brief exercises is within about 10 points of Phase 1 §6.6
   (see *Support mix*).
5. **Evidence.** Modules name concrete evidence as Phase 1 §10.1 allows: photos for AI review, logs, household
   tasters, quizzes. A safety item failed anywhere means a resit.
6. **Validator.** `tools/validate_curriculum.py` keeps the outline consistent through later phases and runs in CI.

### Gaps and open questions for Phase 3
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

