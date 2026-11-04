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
| | | **Total** | **45** | **123** | **170** | **170** | |
<!-- END generated:summary -->

## Competency coverage (generated)

Each cell shows *highest level taught by the end of the stage / Phase 1 target*. "–" means no target yet; "·" means
the stage file doesn't exist yet; ✗ marks a miss.

<!-- BEGIN generated:coverage -->
| Competency | S1 | S2 | S3 | S4 | S5 |
|---|---|---|---|---|---|
| `CK-TERM` | 2 / 2 | · | · | · | · |
| `CK-METH` | 2 / 2 | · | · | · | · |
| `CK-RCPT` | 3 / 3 | · | · | · | · |
| `CK-CLAS` | – | · | · | · | · |
| `CK-CUIS` | – | · | · | · | · |
| `CK-BEVR` | – | · | · | · | · |
| `IN-SEL` | 2 / 2 | · | · | · | · |
| `IN-STO` | 3 / 3 | · | · | · | · |
| `IN-VEG` | 2 / 2 | · | · | · | · |
| `IN-STA` | 2 / 2 | · | · | · | · |
| `IN-DAI` | 2 / 2 | · | · | · | · |
| `IN-MEA` | 1 / 1 | · | · | · | · |
| `IN-FSH` | 1 / 1 | · | · | · | · |
| `IN-ARO` | 1 / 1 | · | · | · | · |
| `IN-BAK` | – | · | · | · | · |
| `TS-MEP` | 3 / 3 | · | · | · | · |
| `TS-KNF` | 3 / 3 | · | · | · | · |
| `TS-DRY` | 2 / 2 | · | · | · | · |
| `TS-MOI` | 3 / 3 | · | · | · | · |
| `TS-CMB` | – | · | · | · | · |
| `TS-EGG` | 3 / 3 | · | · | · | · |
| `TS-VEG` | 3 / 3 | · | · | · | · |
| `TS-STA` | 3 / 3 | · | · | · | · |
| `TS-STK` | – | · | · | · | · |
| `TS-SAU` | – | · | · | · | · |
| `TS-PRO` | 1 / 1 | · | · | · | · |
| `TS-FAB` | – | · | · | · | · |
| `TS-BAK` | – | · | · | · | · |
| `TS-BRD` | – | · | · | · | · |
| `TS-GRD` | – | · | · | · | · |
| `TS-PRS` | – | · | · | · | · |
| `TS-MOD` | – | · | · | · | · |
| `TS-PLT` | 1 / 1 | · | · | · | · |
| `SC-HEAT` | 2 / 2 | · | · | · | · |
| `SC-BRWN` | 2 / 2 | · | · | · | · |
| `SC-PROT` | 2 / 2 | · | · | · | · |
| `SC-STRC` | 2 / 2 | · | · | · | · |
| `SC-EMUL` | 1 / 1 | · | · | · | · |
| `SC-MICR` | 1 / 1 | · | · | · | · |
| `FL-SENS` | 2 / 2 | · | · | · | · |
| `FL-SEAS` | 2 / 2 | · | · | · | · |
| `FL-TEXT` | 1 / 1 | · | · | · | · |
| `FL-ARCH` | – | · | · | · | · |
| `FL-CORR` | 1 / 1 | · | · | · | · |
| `KM-PLAN` | 2 / 2 | · | · | · | · |
| `KM-MATH` | 2 / 2 | · | · | · | · |
| `KM-PURC` | 1 / 1 | · | · | · | · |
| `KM-WAST` | 2 / 2 | · | · | · | · |
| `KM-EQPT` | 2 / 2 | · | · | · | · |
| `SA-HYG` | 3 / 3 | · | · | · | · |
| `SA-TEMP` | 3 / 3 | · | · | · | · |
| `SA-PHYS` | 3 / 3 | · | · | · | · |
| `SA-ALLG` | 2 / 2 | · | · | · | · |
| `SA-HRPR` | – | · | · | · | · |
| `RD-RATO` | – | · | · | · | · |
| `RD-ADPT` | 1 / 1 | · | · | · | · |
| `RD-TEST` | 1 / 1 | · | · | · | · |
| `RD-WRIT` | 2 / 2 | · | · | · | · |
| `JD-CUES` | 2 / 2 | · | · | · | · |
| `JD-CHSE` | 1 / 1 | · | · | · | · |
| `JD-TRBL` | 1 / 1 | · | · | · | · |
| `JD-REFL` | 2 / 2 | · | · | · | · |
| `CR-COMP` | – | · | · | · | · |
| `CR-MENU` | – | · | · | · | · |
| `CR-IMPV` | – | · | · | · | · |
| `CR-ORIG` | – | · | · | · | · |
<!-- END generated:coverage -->

## Support mix (generated)

Share of practical exercises per support level, against Phase 1 §6.6.

<!-- BEGIN generated:support -->
| Stage | Exercises | Recipe | Framework | Brief | Phase 1 §6.6 target |
|---|---|---|---|---|---|
| S1 | 58 | 78 % | 22 % | 0 % | 70 / 30 / 0 % |
<!-- END generated:support -->
