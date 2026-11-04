# Phase 2 — Curriculum outline

Status: **in progress** (2026-11-04). This is the complete outline of the core program: every course broken into
modules and lessons, with objectives, prerequisites, hours, competencies, practical exercises and assessment lines. It
builds on [Phase 1](01-vision-architecture.md) and does not repeat it. Lesson content and recipes are out of scope;
they come in Phases 5–6.

| Stage | File |
|---|---|
| S1 Foundations | [`02-outline/S1-foundations.md`](02-outline/S1-foundations.md) |

The tables marked *generated* are produced by `tools/validate_curriculum.py` (run it with `--write` after editing an
outline file). CI checks that they are current, and that IDs, prerequisites, hours and competency coverage are
consistent.

---

## Conventions

**Hierarchy.** Stage → course → module → lesson. Practical exercises and the module assessment belong to the module.

| Level | Size | ID | Example |
|---|---|---|---|
| Course | as in Phase 1 §7 (±10 %) | `AAA-n` | `KNF-1` |
| Module | about 2–14 h | `<course>.M<n>` | `KNF-1.M3` |
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
| | | **Total** | **105** | **259** | **430** | **430** | |
<!-- END generated:summary -->

## Competency coverage (generated)

Each cell shows *highest level taught by the end of the stage / Phase 1 target*. "–" means no target yet; "·" means
the stage file doesn't exist yet; ✗ marks a miss.

<!-- BEGIN generated:coverage -->
| Competency | S1 | S2 | S3 | S4 | S5 |
|---|---|---|---|---|---|
| `CK-TERM` | 2 / 2 | 3 / 3 | · | · | · |
| `CK-METH` | 2 / 2 | 3 / 3 | · | · | · |
| `CK-RCPT` | 3 / 3 | 3 / 3 | · | · | · |
| `CK-CLAS` | – | 2 / 2 | · | · | · |
| `CK-CUIS` | – | 1 / 1 | · | · | · |
| `CK-BEVR` | – | – | · | · | · |
| `IN-SEL` | 2 / 2 | 3 / 3 | · | · | · |
| `IN-STO` | 3 / 3 | 3 / 3 | · | · | · |
| `IN-VEG` | 2 / 2 | 3 / 3 | · | · | · |
| `IN-STA` | 2 / 2 | 3 / 3 | · | · | · |
| `IN-DAI` | 2 / 2 | 3 / 3 | · | · | · |
| `IN-MEA` | 1 / 1 | 3 / 3 | · | · | · |
| `IN-FSH` | 1 / 1 | 3 / 3 | · | · | · |
| `IN-ARO` | 1 / 1 | 2 / 2 | · | · | · |
| `IN-BAK` | – | 3 / 3 | · | · | · |
| `TS-MEP` | 3 / 3 | 4 / 4 | · | · | · |
| `TS-KNF` | 3 / 3 | 4 / 4 | · | · | · |
| `TS-DRY` | 2 / 2 | 3 / 3 | · | · | · |
| `TS-MOI` | 3 / 3 | 4 / 4 | · | · | · |
| `TS-CMB` | – | 2 / 2 | · | · | · |
| `TS-EGG` | 3 / 3 | 4 / 4 | · | · | · |
| `TS-VEG` | 3 / 3 | 4 / 4 | · | · | · |
| `TS-STA` | 3 / 3 | 3 / 3 | · | · | · |
| `TS-STK` | – | 3 / 3 | · | · | · |
| `TS-SAU` | – | 3 / 3 | · | · | · |
| `TS-PRO` | 1 / 1 | 3 / 3 | · | · | · |
| `TS-FAB` | – | 2 / 2 | · | · | · |
| `TS-BAK` | – | 3 / 3 | · | · | · |
| `TS-BRD` | – | 2 / 2 | · | · | · |
| `TS-GRD` | – | – | · | · | · |
| `TS-PRS` | – | – | · | · | · |
| `TS-MOD` | – | – | · | · | · |
| `TS-PLT` | 1 / 1 | 2 / 2 | · | · | · |
| `SC-HEAT` | 2 / 2 | 3 / 3 | · | · | · |
| `SC-BRWN` | 2 / 2 | 3 / 3 | · | · | · |
| `SC-PROT` | 2 / 2 | 3 / 3 | · | · | · |
| `SC-STRC` | 2 / 2 | 3 / 3 | · | · | · |
| `SC-EMUL` | 1 / 1 | 3 / 3 | · | · | · |
| `SC-MICR` | 1 / 1 | 3 / 3 | · | · | · |
| `FL-SENS` | 2 / 2 | 3 / 3 | · | · | · |
| `FL-SEAS` | 2 / 2 | 3 / 3 | · | · | · |
| `FL-TEXT` | 1 / 1 | 2 / 2 | · | · | · |
| `FL-ARCH` | – | 2 / 2 | · | · | · |
| `FL-CORR` | 1 / 1 | 3 / 3 | · | · | · |
| `KM-PLAN` | 2 / 2 | 3 / 3 | · | · | · |
| `KM-MATH` | 2 / 2 | 3 / 3 | · | · | · |
| `KM-PURC` | 1 / 1 | 2 / 2 | · | · | · |
| `KM-WAST` | 2 / 2 | 3 / 3 | · | · | · |
| `KM-EQPT` | 2 / 2 | 3 / 3 | · | · | · |
| `SA-HYG` | 3 / 3 | 4 / 4 | · | · | · |
| `SA-TEMP` | 3 / 3 | 4 / 4 | · | · | · |
| `SA-PHYS` | 3 / 3 | 4 / 4 | · | · | · |
| `SA-ALLG` | 2 / 2 | 3 / 3 | · | · | · |
| `SA-HRPR` | – | 1 / 1 | · | · | · |
| `RD-RATO` | – | 2 / 2 | · | · | · |
| `RD-ADPT` | 1 / 1 | 2 / 2 | · | · | · |
| `RD-TEST` | 1 / 1 | 2 / 2 | · | · | · |
| `RD-WRIT` | 2 / 2 | 2 / 2 | · | · | · |
| `JD-CUES` | 2 / 2 | 3 / 3 | · | · | · |
| `JD-CHSE` | 1 / 1 | 2 / 2 | · | · | · |
| `JD-TRBL` | 1 / 1 | 3 / 3 | · | · | · |
| `JD-REFL` | 2 / 2 | 3 / 3 | · | · | · |
| `CR-COMP` | – | 1 / 1 | · | · | · |
| `CR-MENU` | – | – | · | · | · |
| `CR-IMPV` | – | 1 / 1 | · | · | · |
| `CR-ORIG` | – | – | · | · | · |
<!-- END generated:coverage -->

## Support mix (generated)

Share of practical exercises per support level, against Phase 1 §6.6.

<!-- BEGIN generated:support -->
| Stage | Exercises | Recipe | Framework | Brief | Phase 1 §6.6 target |
|---|---|---|---|---|---|
| S1 | 58 | 78 % | 22 % | 0 % | 70 / 30 / 0 % |
| S2 | 70 | 41 % | 49 % | 10 % | 40 / 50 / 10 % |
<!-- END generated:support -->
