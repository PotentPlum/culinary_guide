# Phase 5 — Course development

Status: **S1 and S2 reviewed; S3 in progress** (2026-11-04). The review answers are applied (§7.3 and §8.3). S3–S5
follow one stage at a time, in the format agreed on S1.

**Stages developed:** S1, S2

This phase develops every course of the outline in depth. For each course it gives:
- an introduction;
- the learning outcomes, and where each is taught, practised and checked;
- the module structure and the estimated hours;
- a detailed outline of every lesson;
- a specification of every practical exercise;
- the knowledge checks, written out item by item;
- the assessment criteria;
- the materials and equipment, with an indicative ingredient budget.

Every course is validated against the outline (Phases 2–3) and the competency framework (Phase 4).

What this phase does **not** contain:
- **Lesson text and recipes.** These belong to Phase 6. Lesson outlines say what a lesson must teach and practise, and
  exercise specs say what the learner does and shows, but neither gives quantities to follow.
- **Milestones and the capstone.** Their briefs and rubrics are complete in
  [assessment §7–8](04-assessment.md#7-milestones). A stage's courses prepare for them and link to them.

---

## 1. The course file

There is one file per course, `05-courses/<stage>/<course>.md`, for example
`05-courses/S1/KNF-1.md`. Parts marked *generated* are produced from the outline by `tools/validate_curriculum.py
--write`, so they cannot drift from it. The rest is written by hand and checked by the same script.

| Section | Content | Source |
|---|---|---|
| Header | Stage, hours, practical sessions, prerequisites, the courses that build on this one, the outline's purpose | generated |
| **Introduction** | Why the course exists; where it sits in the spiral; what changes for the learner; how to pace it; how it connects to the milestone | written |
| **Learning outcomes** | The outline's outcomes, each with the lessons that teach it, the exercises that practise it and the checks that verify it | generated |
| **Module structure and hours** | Modules with hours, lessons, sessions, exercises, knowledge-check items and evidence rows; weeks at 5, 6.5 and 8 h a week; calendar time | generated |
| **Modules** | Per module: the outline's scope, objectives and check (generated); then every lesson outline, every exercise spec, and the module check | both |
| **Assessment criteria** | The completion rule, the evidence rows per module, and what the milestone re-tests (generated), with the course's own notes | both |
| **Materials and equipment** | The kit by tier, with Good and Best; consumables; how ingredients are shared across a week; the indicative ingredient budget | written |
| **Validation** | What the script checked for this course | generated |

### 1.1 Lesson outlines

Each lesson has a heading `#### <lesson ID> — <title>`, the outline's hours, sessions, prerequisites, moves and
objectives (generated), and four fields:

| Field | Content |
|---|---|
| **Concepts** | What the lesson must teach, with the science behind it, as short points. Technical claims are cited `[S#]`, and safety figures come only from [assessment §9](04-assessment.md#9-safety-figures-used-in-assessment). |
| **Sessions** | A numbered plan, one item per practical session in the outline, so the count must match: what the learner does, with the time it takes. A theory-only lesson says so and has no numbered items. |
| **Cues** | What the learner should see, hear, smell, taste or feel when it goes right. |
| **Faults** | The typical mistakes and their causes, which Phase 6 must address in the lesson's troubleshooting. |

An outline runs to about 150–300 words. It is a brief for the Phase 6 writer, not lesson text.

### 1.2 Exercise specs

Each exercise has a heading `#### <exercise ID> — <title>`, the outline's type, support and description (generated),
and seven fields:

| Field | Content |
|---|---|
| **Evidences** | The competencies the exercise gives evidence for, with the level, e.g. `TS-KNF→3, SA-PHYS→3`. This replaces the Phase 4 approximation that an exercise counts for every competency of its module ([framework §1.2](04-competency-framework.md#12-the-entry-format)). An exercise may evidence nothing (`—`) when it only prepares, such as a baseline, or when its module reaches level 1 only: level 1 is shown by the quiz. |
| **Aim** | What the exercise trains, in one or two sentences. |
| **Conditions** | Support, time limit, and the variable that changes when the exercise is part of a series (Phase 1 §9.2). |
| **Inputs** | The kinds of ingredient and rough quantities, chosen for cost and for Dutch seasonal produce. Not a recipe. |
| **Procedure** | The steps in outline. |
| **Evidence** | The evidence methods first (`photo, measure — …`, or `photo, measure:` followed by a list), then what to capture. Where AI review scores a photo or video, the field names the **reference images** Phase 6 must produce (*Competent* and *Not yet*) and the short rubric the AI scores against ([assessment §6](04-assessment.md#6-ai-review)). |
| **Time** | Active time, and calendar time if it differs. |

**Tagging rules** (checked by the script):
- a tag names a competency that one of the module's lessons moves;
- its level is from 2 up to the highest level the module reaches;
- every competency a module moves to level 2 or more has an exercise tagged at that level, named in the module's
  evidence check.

### 1.3 Module checks

Every module ends with `#### Module check`, which has two parts.

**Knowledge check.** Each module has a knowledge check, even where the outline's *Assessment* line names only evidence:
the brief asks for knowledge checks in every course, and the [K] objectives need one (decision 2, §5). The size follows
the [assessment §4.1 blueprint](04-assessment.md#41-quiz-blueprint), counted on the lessons' objectives: 2 items per [K]
objective, plus up to 1 per [J] objective, with at least 8 and at most 20. A module with neither [K] nor [J]
objectives has no quiz. A module with only [J] objectives has a quiz where its judgments can be put as scenarios, and
otherwise rests on its evidence check: the blueprint counts [J] objectives only "that can be put as a scenario". The
items are written out:

| Column | Content |
|---|---|
| ID | `<module>.Q<n>` |
| Type | multiple choice, scenario, calculation, image identification or ordering |
| Lesson | The lesson whose objective the item tests. Every lesson with a [K] objective is tested. |
| Safety | `yes` for items on an `SA-*` objective or a safety rule: these must all be correct to pass. Every module that moves an `SA-*` competency has at least one. |
| Item | The question, with the options. An image item describes the image Phase 6 must supply. |
| Answer | The key. For a scenario, the points a good answer contains. |
| Why | A one-line rationale, used as feedback. |

This is one complete check per module. The item bank of three times that size, which lets a retake draw new items, is
written in Phase 6 (review decision, 2026-11-04).

**Evidence check.** For each competency the module's lessons move to level 2 or more, a row gives:
- the level, which is the highest level the module reaches;
- the exercises that supply the evidence, each tagged at that level, or `knowledge check` where the Phase 4 success
  row at that level rests on the quiz alone (for example `SA-PHYS` level 2);
- the evidence methods and what must be shown.

The standard is the competency's Phase 4 success row at that level ([assessment §4.2](04-assessment.md#42-evidence-check)).
Where a success row has parts that the outline teaches in different modules (for example a sensory form and the
calibration tests for `FL-SENS` level 2), the earlier module's row says which part it covers and where the rest is
shown.
The counts of the mastery criterion (two demonstrations on different days for level 3, and so on) are built up in the
evidence record over the stage, not in a single check.

### 1.4 Materials, equipment and budget

- **Equipment** is listed per course in a table: item, tier (0–3 from Phase 1 §9.4, or `—` for small items outside the
  tiers), **Good** and **Best**. A course never requires a tier above its stage (S1: tier 0). Good and Best are given
  where an item first appears; later courses refer back.
- **Consumables** and **ingredient sharing** follow Phase 1 §9.3: exercises in one week share ingredients, and drills use
  cheap produce that becomes a meal.
- **Indicative ingredient budget**: the extra cost of the course's exercises in euros, at Dutch supermarket prices in
  2026, rounded to €5. It excludes the pantry staples bought in `ORI-1`, and food that replaces meals the household
  would have bought anyway is counted in full. It is an estimate for planning, not a quote.

---

## 2. Shared conventions

1. **Produce.** Where an exercise names vegetables, it uses Dutch seasonal produce and gives a choice across the seasons
   (for example carrot, celeriac or beetroot), so a course can start in any month.
2. **Terms.** French (FR) and Dutch (NL) names are given where the learner will meet them, as in the outline.
3. **Safety figures** in lessons, exercises and items come only from
   [assessment §9](04-assessment.md#9-safety-figures-used-in-assessment). Where a figure is a Dutch home rule and a
   professional standard differs, the stricter one is taught.
4. **No recipes.** Quantities in exercise inputs are rough, enough to buy and plan. A guided exercise points to the
   recipe Phase 6 will write.
5. **Spiral repetition adds a variable** (Phase 3, decision 4). An outline that revisits a technique names what is new.
6. **Prerequisites.** An exercise uses only techniques taught in its prerequisites, or in earlier lessons of the same
   course (the Phase 3 scan, repeated per course).

---

## 3. Status (generated)

<!-- BEGIN generated:status -->
| Stage | Course | Lessons outlined | Exercises specified | Knowledge-check items | Safety items | Evidence rows | Budget |
|---|---|---|---|---|---|---|---|
| S1 | [`ORI-1`](05-courses/S1/ORI-1.md) | 7 / 7 | 5 / 5 | 24 | 0 | 2 | €15 |
| S1 | [`SAF-1`](05-courses/S1/SAF-1.md) | 10 / 10 | 5 / 5 | 32 | 32 | 7 | €20 |
| S1 | [`WRK-1`](05-courses/S1/WRK-1.md) | 11 / 11 | 6 / 6 | 32 | 0 | 7 | €15 |
| S1 | [`EQP-1`](05-courses/S1/EQP-1.md) | 9 / 9 | 3 / 3 | 24 | 0 | 4 | €10 |
| S1 | [`KNF-1`](05-courses/S1/KNF-1.md) | 15 / 15 | 7 / 7 | 24 | 6 | 11 | €35 |
| S1 | [`TST-1`](05-courses/S1/TST-1.md) | 13 / 13 | 6 / 6 | 34 | 0 | 6 | €25 |
| S1 | [`HEA-1`](05-courses/S1/HEA-1.md) | 17 / 17 | 8 / 8 | 40 | 2 | 10 | €60 |
| S1 | [`EGG-1`](05-courses/S1/EGG-1.md) | 11 / 11 | 6 / 6 | 32 | 2 | 9 | €30 |
| S1 | [`VEG-1`](05-courses/S1/VEG-1.md) | 15 / 15 | 7 / 7 | 40 | 1 | 12 | €35 |
| S1 | [`STA-1`](05-courses/S1/STA-1.md) | 11 / 11 | 5 / 5 | 32 | 5 | 10 | €20 |
| S2 | [`SCI-2`](05-courses/S2/SCI-2.md) | 11 / 11 | 6 / 6 | 48 | 7 | 10 | €25 |
| S2 | [`HEA-2`](05-courses/S2/HEA-2.md) | 12 / 12 | 7 / 7 | 40 | 5 | 18 | €75 |
| S2 | [`KNF-2`](05-courses/S2/KNF-2.md) | 5 / 5 | 4 / 4 | 0 | 0 | 6 | €40 |
| S2 | [`STK-2`](05-courses/S2/STK-2.md) | 14 / 14 | 7 / 7 | 40 | 3 | 20 | €45 |
| S2 | [`SAU-2`](05-courses/S2/SAU-2.md) | 12 / 12 | 9 / 9 | 56 | 4 | 15 | €65 |
| S2 | [`POU-2`](05-courses/S2/POU-2.md) | 13 / 13 | 6 / 6 | 40 | 12 | 16 | €75 |
| S2 | [`MEA-2`](05-courses/S2/MEA-2.md) | 14 / 14 | 7 / 7 | 48 | 9 | 19 | €150 |
| S2 | [`FSH-2`](05-courses/S2/FSH-2.md) | 12 / 12 | 6 / 6 | 32 | 8 | 14 | €110 |
| S2 | [`LEG-2`](05-courses/S2/LEG-2.md) | 10 / 10 | 5 / 5 | 32 | 9 | 8 | €35 |
| S2 | [`BAK-2`](05-courses/S2/BAK-2.md) | 17 / 17 | 9 / 9 | 40 | 3 | 16 | €80 |
| S2 | [`BRD-2`](05-courses/S2/BRD-2.md) | 11 / 11 | 5 / 5 | 32 | 2 | 11 | €25 |
| S3 | [`SAF-3`](05-courses/S3/SAF-3.md) | 5 / 5 | 2 / 2 | 16 | 16 | 4 | €15 |
| S3 | [`FLV-3`](05-courses/S3/FLV-3.md) | 14 / 14 | 7 / 7 | 32 | 2 | 13 | €60 |
| S3 | [`BUT-3`](05-courses/S3/BUT-3.md) | 9 / 9 | 4 / 4 | 8 | 3 | 7 | €130 |
| S3 | [`SLW-3`](05-courses/S3/SLW-3.md) | 12 / 12 | 7 / 7 | 32 | 8 | 13 | €150 |
| S3 | [`FIR-3`](05-courses/S3/FIR-3.md) | 12 / 12 | 7 / 7 | 40 | 13 | 20 | €190 |
| S3 | [`GRD-3`](05-courses/S3/GRD-3.md) | 11 / 11 | 6 / 6 | 32 | 12 | 11 | €135 |
| S3 | [`PRS-3`](05-courses/S3/PRS-3.md) | 10 / 10 | 7 / 7 | 40 | 21 | 12 | €70 |
| S3 | [`VEG-3`](05-courses/S3/VEG-3.md) | 10 / 10 | 6 / 6 | 24 | 1 | 16 | €80 |
| S3 | [`PST-3`](05-courses/S3/PST-3.md) | 14 / 14 | 7 / 7 | 32 | 3 | 12 | €95 |
| S3 | [`BRD-3`](05-courses/S3/BRD-3.md) | 11 / 11 | 6 / 6 | 32 | 2 | 11 | €40 |
| S3 | `CUI-3` | 0 / 34 | — | — | — | — | — |
| S3 | [`KMG-3`](05-courses/S3/KMG-3.md) | 9 / 9 | 5 / 5 | 16 | 6 | 13 | €0 |
| S3 | `NUT-3` | 0 / 6 | — | — | — | — | — |
| S4 | `CLS-4` | 0 / 13 | — | — | — | — | — |
| S4 | `CUI-4` | 0 / 12 | — | — | — | — | — |
| S4 | `PRO-4` | 0 / 11 | — | — | — | — | — |
| S4 | `PST-4` | 0 / 11 | — | — | — | — | — |
| S4 | `MOD-4` | 0 / 7 | — | — | — | — | — |
| S4 | `PLT-4` | 0 / 6 | — | — | — | — | — |
| S4 | `RDV-4` | 0 / 10 | — | — | — | — | — |
| S4 | `MNU-4` | 0 / 14 | — | — | — | — | — |
| S5 | `IND-5` | 0 / 15 | — | — | — | — | — |
| S5 | `RDV-5` | 0 / 9 | — | — | — | — | — |
| S5 | `REV-5` | 0 / 4 | — | — | — | — | — |
<!-- END generated:status -->

## 4. Exercise reachability (generated)

For each competency with a target in a developed stage: the target per stage, and how many exercises give evidence at
each level. ✗ marks a stage target that no exercise up to that stage evidences; *(quiz)* marks a target whose success
row rests on the quiz alone.

<!-- BEGIN generated:reachability -->
| Competency | S1 | S2 | S3 (in progress) | Exercises at L2 · L3 · L4 · L5 |
|---|---|---|---|---|
| `CK-TERM` | 2 (quiz) | 3 | 3 | 0 · 2 · 0 · 0 |
| `CK-METH` | 2 | 3 | 4 | 1 · 2 · 2 · 0 |
| `CK-RCPT` | 3 | 3 | 4 ✗ | 0 · 2 · 0 · 0 |
| `CK-CLAS` | – | 2 | 2 | 2 · 0 · 0 · 0 |
| `CK-CUIS` | – | 1 | 3 ✗ | 0 · 0 · 0 · 0 |
| `CK-BEVR` | – | – | 1 | 0 · 0 · 0 · 0 |
| `IN-SEL` | 2 | 3 | 4 | 1 · 2 · 2 · 0 |
| `IN-STO` | 3 | 3 | 4 | 1 · 5 · 3 · 0 |
| `IN-VEG` | 2 | 3 | 4 | 3 · 3 · 2 · 0 |
| `IN-STA` | 2 | 3 | 4 ✗ | 4 · 3 · 0 · 0 |
| `IN-DAI` | 2 | 3 | 4 | 2 · 2 · 2 · 0 |
| `IN-MEA` | 1 | 3 | 3 | 0 · 3 · 0 · 0 |
| `IN-FSH` | 1 | 3 | 3 | 0 · 3 · 0 · 0 |
| `IN-ARO` | 1 | 2 | 4 | 1 · 0 · 3 · 0 |
| `IN-BAK` | – | 3 | 3 | 0 · 3 · 0 · 0 |
| `TS-MEP` | 3 | 4 | 4 | 1 · 1 · 2 · 0 |
| `TS-KNF` | 3 | 4 | 4 | 4 · 3 · 2 · 0 |
| `TS-DRY` | 2 | 3 | 4 | 3 · 9 · 2 · 0 |
| `TS-MOI` | 3 | 4 | 4 | 0 · 2 · 3 · 0 |
| `TS-CMB` | – | 2 | 4 | 2 · 0 · 4 · 0 |
| `TS-FIRE` | – | – | 3 | 1 · 5 · 0 · 0 |
| `TS-EGG` | 3 | 4 | 4 | 1 · 3 · 3 · 0 |
| `TS-VEG` | 3 | 4 | 4 | 0 · 4 · 8 · 0 |
| `TS-STA` | 3 | 3 | 4 ✗ | 1 · 4 · 0 · 0 |
| `TS-STK` | – | 3 | 4 ✗ | 0 · 5 · 0 · 0 |
| `TS-SAU` | – | 3 | 4 | 0 · 8 · 4 · 0 |
| `TS-PRO` | 1 | 3 | 4 | 0 · 8 · 2 · 0 |
| `TS-FAB` | – | 2 | 3 | 4 · 3 · 0 · 0 |
| `TS-BAK` | – | 3 | 4 | 0 · 6 · 6 · 0 |
| `TS-BRD` | – | 2 | 3 | 3 · 4 · 0 · 0 |
| `TS-GRD` | – | – | 3 | 0 · 3 · 0 · 0 |
| `TS-PRS` | – | – | 3 | 0 · 5 · 0 · 0 |
| `TS-MOD` | – | – | 1 | 0 · 0 · 0 · 0 |
| `TS-PLT` | 1 | 2 | 2 | 2 · 0 · 0 · 0 |
| `SC-HEAT` | 2 | 3 | 4 | 3 · 2 · 2 · 0 |
| `SC-BRWN` | 2 | 3 | 4 | 1 · 4 · 1 · 0 |
| `SC-PROT` | 2 | 3 | 4 | 1 · 4 · 2 · 0 |
| `SC-STRC` | 2 (quiz) | 3 | 4 | 0 · 2 · 2 · 0 |
| `SC-EMUL` | 1 | 3 | 3 | 0 · 6 · 0 · 0 |
| `SC-MICR` | 1 | 3 | 3 | 0 · 4 · 0 · 0 |
| `FL-SENS` | 2 | 3 | 4 | 3 · 1 · 1 · 0 |
| `FL-SEAS` | 2 | 3 | 4 | 4 · 2 · 1 · 0 |
| `FL-TEXT` | 1 | 2 | 3 | 4 · 3 · 0 · 0 |
| `FL-ARCH` | – | 2 | 3 | 2 · 6 · 0 · 0 |
| `FL-CORR` | 1 | 3 | 4 | 0 · 2 · 1 · 0 |
| `KM-PLAN` | 2 | 3 | 4 | 2 · 2 · 4 · 0 |
| `KM-MATH` | 2 | 3 | 4 | 1 · 1 · 3 · 0 |
| `KM-PURC` | 1 | 2 | 4 | 3 · 1 · 2 · 0 |
| `KM-WAST` | 2 | 3 | 4 | 2 · 3 · 4 · 0 |
| `KM-EQPT` | 2 | 3 | 3 | 3 · 5 · 0 · 0 |
| `SA-HYG` | 3 | 4 | 5 | 1 · 1 · 5 · 1 |
| `SA-TEMP` | 3 | 4 | 4 | 1 · 3 · 5 · 0 |
| `SA-PHYS` | 3 | 4 | 5 | 0 · 2 · 2 · 2 |
| `SA-ALLG` | 2 | 3 | 4 ✗ | 1 · 2 · 0 · 0 |
| `SA-HRPR` | – | 1 | 4 | 0 · 0 · 6 · 0 |
| `RD-RATO` | – | 2 | 3 | 4 · 5 · 0 · 0 |
| `RD-ADPT` | 1 | 2 | 3 | 2 · 1 · 0 · 0 |
| `RD-TEST` | 1 | 2 | 3 | 4 · 2 · 0 · 0 |
| `RD-WRIT` | 2 | 2 | 3 | 1 · 1 · 0 · 0 |
| `JD-CUES` | 2 | 3 | 4 | 4 · 5 · 4 · 0 |
| `JD-CHSE` | 1 | 2 | 3 | 2 · 1 · 0 · 0 |
| `JD-TRBL` | 1 | 3 | 4 | 1 · 3 · 2 · 0 |
| `JD-REFL` | 2 | 3 | 4 | 4 · 3 · 1 · 0 |
| `CR-COMP` | – | 1 | 3 | 1 · 5 · 0 · 0 |
| `CR-MENU` | – | – | 2 | 1 · 0 · 0 · 0 |
| `CR-IMPV` | – | 1 | 3 | 0 · 1 · 0 · 0 |
| `CR-ORIG` | – | – | 1 | 0 · 0 · 0 · 0 |
<!-- END generated:reachability -->

## 5. Decisions and change log

1. **Stage by stage** (review decision). S1 is written and reviewed first; the format is fixed by that review.
2. **A knowledge check in every module.** Some outline *Assessment* lines name only evidence. Every module whose lessons
   have a [K] or [J] objective still gets a knowledge check, sized by the blueprint. The outline is not changed: its
   *Assessment* line names the evidence, and the course file adds the quiz.
3. **One full check per module now; the bank of three in Phase 6** (review decision).
4. **Exercises are tagged** with the competencies they evidence (§1.2). The Phase 4 mapping blocks still show the
   module-level approximation; the tags are in the course files and in the reachability table.
5. **Quiz-only success rows.** Where a Phase 4 success row rests on the quiz alone (for example `SA-PHYS`, `SC-HEAT`
   and `KM-MATH` at level 2), the module's evidence row names the knowledge check instead of an exercise.
6. **Success rows that span modules.** Some Phase 4 rows combine parts that the outline teaches in different modules or
   courses (for example `TS-STA` level 3: a pilaf, pasta, a mash). Each evidence row then says which part it covers
   and where the rest is shown. The evidence record, not a single module, completes the row.
7. **Evidence-only modules.** A module with only [J] objectives may skip the quiz when its judgments are better shown by
   the evidence. In S1 this applies to `KNF-1.M5`, `HEA-1.M4` and `VEG-1.M6`. `KNF-1.M4` and `EGG-1.M5` have no [K]
   or [J] objective at all.

**Outline changes** (applied, because they follow decisions already reviewed):

| Where | Was | Now | Why |
|---|---|---|---|
| `SAF-1` purpose, `SAF-1.M3.L2` | USDA FSIS chart [S24] | FDA *Food Code* [S25] | Phase 4 stopped using [S24] (it refused automated access), and the review accepted the Food Code as the reference. |
| `SAF-1.M3.L1` objective | growth "between about 7 and 60 °C" | growth between the cold and hot holding limits (5 and 57 °C) | The rounded range disagreed with the figures the gates use ([assessment §9](04-assessment.md#9-safety-figures-used-in-assessment)). |
| [Assessment §9](04-assessment.md#9-safety-figures-used-in-assessment) (S2) | Intact meat only at 63 °C for 15 seconds | A row for whole-muscle, intact beef steaks served rare (Food Code §3-401.11(C)), and a gate rule for it | `MEA-2` teaches steaks to a chosen doneness, and its outline asks for rare to well done. Without the row, every rare steak would be a gate *Fail*. Kept at the S2 review. |
| [Assessment §9](04-assessment.md#9-safety-figures-used-in-assessment) (S2 review) | No figure for raw-bean lectins; no word on the Food Code's roast oven chart | A row for dried common beans (soak at least 5 h, boil at least 30 min, no slow cooker; FDA *Bad Bug Book* [S33]); a rule that the oven chart is not adopted | S2 review answers 2 and 3. `LEG-2.M1.L1`, `LEG-2.M1.L2`, `LEG-2.M1.X2` (now chickpeas, so the unsoaked pots stay within the rule) and `LEG-2.M1.Q4` follow the row. |
| [Assessment §9](04-assessment.md#9-safety-figures-used-in-assessment) (S3) | No figures for low-temperature poultry, cook-chill storage, raw fish, risk groups, acidity, canning, fermentation salt or nitrite | Rows for each, from sources checked on 2026-11-04 ([S25], [S27], [S34]–[S37]); the sous vide rule now names which table applies to which food | S2 recommendations 2 and 3: `SAF-3`, `SLW-3`, `GRD-3`, `PRS-3` and `CUI-3` need these figures, and no figure is written from memory. To be confirmed at the S3 review. |
| `PRS-3.M3.L1` objective | salt by weight (2 %) | salt by weight (2.25–2.5 %) | The checked source [S36] gives 2.25–2.5 %; 2 % is below it. |

No course, module, lesson, hour, exercise or `Moves` entry has been added, removed or moved; only the objective wording above changed.

**Exercise scope notes** (the outline's one-line description is kept; the spec does more):
- `TST-1.M2.X1` runs the acid series of [assessment §5.2](04-assessment.md#52-acid-series) as well as the salt series,
  so that `FL-SENS` level 2 (which asks for both ranking tests) is complete in one module.
- `SAF-1.M2.X1` holds both audits during a meal with raw protein, and films it, because the `SA-HYG` level 3 row asks
  for video of raw-protein handling.
- `KNF-1.M1.X1` adds moving a pot of hot water and a photo of the fire blanket, because the `SA-PHYS` level 3 row asks
  for them.
- `VEG-1.M1.X1` adds preparing four vegetables of different groups, because the `IN-VEG` level 2 row asks for it.
- `EQP-1.M1.X1` ends with pan choices for three recipes, from the `KM-EQPT` level 2 row.
- `EGG-1.M2.X1` and `STA-1.M2.X1` repeat the target dish on later days without a recipe, because level 3 asks for
  independent execution on two occasions.
- `KNF-1` introduced working sizes for *bâtonnet* (5–7 mm square, 5–6 cm long) and *paysanne* (10–12 mm, 2–3 mm
  thick). After the S1 review they are in the Phase 4 tolerance note (`04-competencies/TS.md`).

## 6. Equipment across the developed courses (generated)

<!-- BEGIN generated:equipment -->
| Item | Tier | Courses |
|---|---|---|
| Bowl for ice water | 0 | `HEA-1` |
| Bread knife | 0 | `EQP-1` |
| Cast-iron or carbon-steel skillet | 0 | `EQP-1`, `HEA-1`, `MEA-2`, `FIR-3`, `VEG-3` |
| Chef's knife (20 cm) | 0 | `EQP-1`, `KNF-1`, `KNF-2`, `POU-2` |
| Deep heavy pot for frying | 0 | `HEA-2` |
| Deep pot with a rack | 0 | `PRS-3` |
| Digital scale (1 g) | 0 | `ORI-1`, `TST-1`, `WRK-1`, `BAK-2`, `BRD-2`, `KNF-2`, `SCI-2`, `BRD-3`, `BUT-3`, `FLV-3`, `KMG-3`, `PRS-3` |
| Heavy saucepan with a lid | 0 | `STA-1`, `LEG-2`, `SAU-2`, `PRS-3` |
| Heavy stainless sauté pan | 0 | `EQP-1`, `HEA-1`, `VEG-1`, `POU-2`, `SAU-2`, `VEG-3` |
| Honing steel | 0 | `EQP-1`, `KNF-1` |
| Instant-read thermometer | 0 | `EGG-1`, `EQP-1`, `HEA-1`, `ORI-1`, `SAF-1`, `BRD-2`, `FSH-2`, `HEA-2`, `LEG-2`, `MEA-2`, `POU-2`, `SAU-2`, `SCI-2`, `STK-2`, `BUT-3`, `GRD-3`, `PST-3`, `SAF-3`, `SLW-3` |
| Labels and a marker | 0 | `KMG-3`, `PRS-3`, `SAF-3` |
| Large stable board | 0 | `EQP-1`, `KNF-1` |
| Mixing bowls | 0 | `WRK-1`, `BAK-2`, `SCI-2`, `GRD-3` |
| Oven thermometer | 0 | `EQP-1`, `ORI-1`, `BAK-2`, `BRD-2` |
| Oven thermometer for the grate | 0 | `FIR-3` |
| Paring knife | 0 | `EQP-1`, `KNF-1` |
| Probe thermometer | 0 | `FIR-3` |
| Saucepans | 0 | `EQP-1`, `HEA-1`, `VEG-1`, `SCI-2` |
| Shallow containers | 0 | `SAF-3` |
| Sheet pan as a tray | 0 | `WRK-1` |
| Sheet pans | 0 | `EQP-1`, `HEA-1`, `STA-1`, `VEG-1`, `LEG-2` |
| Sieve | 0 | `STA-1` |
| Small saucepan | 0 | `EGG-1`, `SAU-2` |
| Small saucepans and a skillet | 0 | `FLV-3` |
| Stockpot | 0 | `EQP-1`, `FSH-2`, `POU-2`, `STK-2`, `SAF-3` |
| Stockpot or large pot | 0 | `STA-1` |
| Tongs | 0 | `HEA-1` |
| Baking stone or steel | 1 | `BRD-2`, `PST-3` |
| Bench scraper | 1 | `BAK-2`, `BRD-2`, `BRD-3` |
| Fine sieve | 1 | `EGG-1`, `LEG-2`, `SAU-2`, `SCI-2`, `STK-2`, `SLW-3`, `VEG-3` |
| Frying thermometer | 1 | `HEA-2` |
| Heavy lidded casserole (*braadpan*) | 1 | `BRD-2`, `HEA-2`, `LEG-2`, `MEA-2`, `BRD-3`, `SLW-3`, `VEG-3` |
| Loaf tin | 1 | `BAK-2`, `BRD-2`, `BRD-3`, `GRD-3` |
| Mixer | 1 | `BAK-2` |
| Rolling pin | 1 | `BAK-2`, `PST-3` |
| Tart tin | 1 | `BAK-2`, `PST-3` |
| Whetstone | 1 | `KNF-2` |
| Chimney starter | 2 | `FIR-3` |
| Fermentation jars and weights | 2 | `PRS-3` |
| Heat-resistant gloves | 2 | `FIR-3` |
| Lidded charcoal grill | 2 | `FIR-3` |
| Mandoline | 2 | `SAF-3`, `VEG-3` |
| Pressure cooker | 2 | `SAF-3`, `SLW-3` |
| Scale (0.01 g) | 2 | `PRS-3` |
| Sous vide circulator | 2 | `SLW-3` |
| Zip-top or vacuum bags | 2 | `SLW-3` |
| Baking beans | — | `BAK-2` |
| Balloon whisk | — | `SAU-2` |
| Blender | — | `STK-2`, `VEG-3` |
| Boning knife | — | `KNF-2`, `POU-2`, `BUT-3` |
| Calibration kit: coded cups | — | `TST-1`, `FLV-3`, `VEG-3` |
| Calibration kit: key sheets | — | `TST-1` |
| Camera | — | `ORI-1` |
| Canning jars with new lids | — | `PRS-3` |
| Carving board | — | `MEA-2` |
| Carving knife | — | `MEA-2` |
| Cheese knives and a board | — | `GRD-3` |
| Closed-cell foam tape | — | `SLW-3` |
| Containers with lids | — | `KNF-2` |
| Cut-resistant glove | — | `BUT-3` |
| Deep roasting tin for a water bath | — | `EGG-1`, `GRD-3` |
| Dough container | — | `BRD-2`, `BRD-3` |
| Filleting knife | — | `FSH-2`, `BUT-3` |
| Fire blanket | — | `SAF-1`, `HEA-2`, `FIR-3` |
| First-aid kit | — | `SAF-1` |
| Fish scaler | — | `FSH-2`, `BUT-3` |
| Fish spatula | — | `HEA-1`, `FSH-2` |
| Fish tweezers | — | `FSH-2`, `BUT-3` |
| Freezer containers | — | `LEG-2`, `STK-2`, `KMG-3` |
| Freezer thermometer | — | `GRD-3`, `KMG-3`, `SAF-3` |
| Fridge thermometer | — | `SAF-1` |
| Gratin dish | — | `VEG-3` |
| Grid board or ruler | — | `KNF-1`, `ORI-1`, `KNF-2` |
| Grill brush | — | `FIR-3` |
| Grill pan | — | `HEA-2` |
| Hand towels and dish cloths | — | `SAF-1` |
| Heatproof bowl for a bain-marie | — | `SAU-2`, `PST-3` |
| Jar for the starter | — | `BRD-3` |
| Jar lifter and funnel | — | `PRS-3` |
| Jars with lids | — | `SCI-2` |
| Journal | — | `ORI-1` |
| Kitchen shears | — | `POU-2`, `FIR-3` |
| Kitchen string | — | `MEA-2`, `STK-2`, `BUT-3` |
| Kitchen timer | — | `KNF-1`, `WRK-1` |
| Labels and pen | — | `ORI-1`, `SAF-1`, `LEG-2` |
| Ladle | — | `STA-1` |
| Ladle and skimmer | — | `STK-2`, `SLW-3` |
| Lame or blade | — | `BRD-2`, `BRD-3` |
| Long tongs | — | `FIR-3` |
| Measuring jug and spoons | — | `WRK-1` |
| Measuring jugs or bottles (1 l) | — | `TST-1` |
| Measuring spoons and a pipette | — | `TST-1` |
| Metal ash bucket with a lid | — | `FIR-3` |
| Mortar and pestle | — | `FLV-3` |
| Moulds or rings for mousses | — | `PST-3` |
| Muffin tin | — | `BAK-2` |
| Muslin or cheesecloth | — | `LEG-2`, `STK-2` |
| Non-slip mat or damp towel | — | `KNF-1` |
| Omelette pan (20 cm) | — | `EGG-1` |
| Oven gloves | — | `BRD-2`, `BRD-3` |
| Palette knife | — | `BAK-2` |
| Parchment paper | — | `FSH-2` |
| Peeler | — | `KNF-1` |
| pH strips | — | `SCI-2`, `PRS-3` |
| Phone stand | — | `KNF-1`, `FSH-2`, `MEA-2`, `POU-2`, `SAU-2`, `BUT-3` |
| Pie dish | — | `PST-3` |
| Piping bags and nozzles | — | `PST-3` |
| Planning templates | — | `KMG-3` |
| Potato masher | — | `VEG-1` |
| Precision scale (0.1 g) | — | `TST-1`, `BAK-2`, `GRD-3`, `PST-3` |
| Proofing basket | — | `BRD-2`, `BRD-3` |
| Ramekins | — | `EGG-1`, `BAK-2` |
| Roasting rack | — | `HEA-2` |
| Roasting tin | — | `MEA-2`, `POU-2`, `BUT-3` |
| Room thermometer | — | `BRD-3`, `PRS-3` |
| Round cake tins | — | `BAK-2` |
| Salad spinner | — | `VEG-1`, `GRD-3` |
| Seasonal calendar | — | `VEG-1`, `VEG-3` |
| Second cutting board for raw protein | — | `SAF-1`, `FSH-2`, `KNF-2`, `MEA-2`, `POU-2`, `BUT-3` |
| Shallow containers with lids | — | `SAF-1`, `STA-1`, `MEA-2`, `POU-2`, `KMG-3` |
| Slotted spoon | — | `EGG-1` |
| Slow cooker | — | `SLW-3` |
| Small prep bowls | — | `WRK-1` |
| Spice grinder | — | `FLV-3` |
| Spice jars and labels | — | `FLV-3` |
| Spider or slotted spoon | — | `HEA-2` |
| Steamer | — | `HEA-1`, `VEG-1`, `FSH-2` |
| Stick blender | — | `EGG-1`, `SAU-2`, `PST-3` |
| Stick blender or blender | — | `FLV-3` |
| Tasting cups | — | `ORI-1` |
| Tasting form | — | `TST-1` |
| Vacuum flask | — | `SAU-2` |
| Weights for pressing | — | `GRD-3` |
| Wire rack | — | `BAK-2`, `BRD-2`, `HEA-2`, `FLV-3`, `PST-3` |
| Wood chunks | — | `FIR-3` |
| Wooden spoon | — | `STA-1` |
<!-- END generated:equipment -->

---

## 7. End of the S1 batch

### 7.1 What was delivered

S1 is developed in ten course files in [`05-courses/S1/`](05-courses/S1/). In total:
- 119 lesson outlines, each with a session plan that matches the outline's practical sessions;
- 58 exercise specs, each tagged with the competencies it evidences;
- 314 knowledge-check items in 39 checks, 48 of them safety items;
- 78 evidence rows against the Phase 4 success rows.

The status table (§3) has the counts per course. Every S1 stage target is evidenced by an exercise at that level, or
by the quiz where its success row asks only for the quiz (§4). The milestone `MS-S1` is unchanged
([assessment §7.1](04-assessment.md#71-ms-s1--foundations-7-h)). Each course file says what it feeds into it.

**The indicative ingredient budget** for the S1 exercises is about **€265** beyond normal meals, over about 31 weeks
at 6.5 hours a week (200.5 hours, including `MS-S1`). On top of it come the one-off `ORI-1` pantry baseline (about €50) and the tier 0 equipment that
the learner does not yet own.

**Checks.** The validator now checks every course file against the outline and the competency framework (§1). Its
generated *Validation* block in each course file lists what was checked.

### 7.2 Decisions and assumptions

The decisions are numbered in §5. The assumptions:
- **Prices** are rough Dutch supermarket prices for 2026, not checked against shop data.
- **Produce** follows the Dutch seasons. Each exercise gives a summer and a winter choice where the season matters.
- **One helper:** the household includes at least one person who can code samples and taste blind (`TST-1`,
  [assessment §5](04-assessment.md#5-calibration-tests)).
- **Reference images:** the knowledge checks and the AI-review exercises name the images Phase 6 must produce. None
  exist yet.

### 7.3 Gaps and open questions (review answers, 2026-11-04)

1. **Exercise scope notes** (§5). Seven exercise specs do more than the outline's one-line description, so that their
   module can meet the Phase 4 success row. Hours are unchanged.
   - Option 1: keep the outline's short descriptions and let the specs carry the detail. This is my recommendation.
   - Option 2: rewrite the outline descriptions to match.

   **Answer:** option 1. The outline keeps its short descriptions; the specs carry the detail.
2. **Success rows that span modules** (decision 6). Rows such as `TS-STA` level 3 and `IN-STA` level 2 are completed
   over several modules or courses.
   - Option 1: the evidence rows say which part each module covers. This is what S1 does, and my recommendation.
   - Option 2: split those Phase 4 rows by element.

   **Answer:** option 1. The Phase 4 rows stay as they are.
3. **Burn first aid.** `SAF-1.M4.L2` does not give a cooling time for burns, because the program has no source for it
   yet. Phase 6 should cite the Dutch Red Cross; it is not yet in `sources.md`.
4. **The knowledge-check items have not been reviewed by a second reader.** The validator checks their number, type,
   lesson and safety flag, not whether each answer is right. A read-through during the review is worth it, above all
   for the 48 safety items.
5. **The tolerance note.** Should the working sizes for *bâtonnet* and *paysanne* that `KNF-1` adds be added to the
   Phase 4 tolerance note in `04-competencies/TS.md`? I recommend yes.

   **Answer:** yes; they are added.

### 7.4 Recommendations for S2

1. **Keep the format.** Change it only where the S1 review asks; the validator then holds S2 to it.
2. **Check every module's evidence row against its success row before writing.** In S1, most exercise extensions came
   from success rows that asked for more than the outline's exercise description. Doing this check first shows any
   such extension before the course is written.
3. **Tier 1 equipment** (the Dutch oven, a fine sieve, a whetstone, a mixer) gets its Good and Best where it first
   appears in S2.
4. **`SCI-2` and the protein courses** use the doneness and cooling figures of assessment §9. The sous vide table that
   `SLW-3` needs is a separate S3 question.

## 8. End of the S2 batch

### 8.1 What was delivered

S2 is developed in eleven course files in [`05-courses/S2/`](05-courses/S2/). In total:
- 131 lesson outlines, each with a session plan that matches the outline's practical sessions;
- 71 exercise specs, each tagged with the competencies it evidences;
- 408 knowledge-check items in 51 checks, 62 of them safety items;
- 153 evidence rows against the Phase 4 success rows.

Eight modules have no quiz:
- `KNF-2.M1`, `KNF-2.M2` and `KNF-2.M3`, `POU-2.M2` and `FSH-2.M5` have no [K] or [J] objective;
- `HEA-2.M6`, `BAK-2.M6` and `BRD-2.M5` have only a [J] objective that their evidence shows better than scenario items
  (decision 7).

Every S2 stage target is evidenced by an exercise at that level, or by the quiz where its success row asks only for the
quiz (§4). `**Stages developed:**` now reads S1, S2, so the validator holds both stages to that. The milestone `MS-S2`
is unchanged ([assessment §7.2](04-assessment.md#72-ms-s2--core-techniques-16-h)).

**The indicative ingredient budget** for the S2 exercises is about **€725**, over about 40 weeks at 6.5 hours a week
(260 hours, including `MS-S2`). Meat (€150) and fish (€110) are the largest items. Nearly all of it is household meals,
so the true extra cost is much lower; the course files give Good options throughout.

### 8.2 Decisions and assumptions

1. **A row added to [assessment §9](04-assessment.md#9-safety-figures-used-in-assessment): rare beef steaks.**
   - **What changed:** §9 had only "intact meat, 63 °C for 15 seconds". `MEA-2` teaches steaks to a chosen doneness, and
     its outline asks for rare to well done, so every rare steak would have been a gate *Fail*.
   - **The new row:** the Food Code's rule for whole-muscle, intact beef steaks (§3-401.11(C)). They may be served rare
     when seared on top and bottom to a surface of 63 °C with the colour changed on every outside surface, kept intact,
     and not served to risk groups. A gate rule says how it is checked.
   - **Status:** logged in the outline-changes table of §5, and question 1 below.
2. **What S2 does not teach**, because §9 has no figure for it:
   - a beef roast below the roast table (rare roast beef);
   - fish below 63 °C, raw or lightly cooked;
   - a stock or sauce held below 57 °C for long.

   Hollandaise and *beurre blanc* are made for the meal and thrown away after it.
3. **Rows that span courses** (decision 6) are more common in S2 than in S1, because S2's competencies run across the
   ingredient courses. Where each part is shown:

   | Row | Parts and where they are shown |
   |---|---|
   | `SC-EMUL` 3 | Mayonnaise: `SCI-2.M4.X1`, `SAU-2.M4.X1`. Warm butter emulsion: `SAU-2.M2.X1`. Further foam and buttercream evidence: `BAK-2.M4` |
   | `TS-PRO` 3 | Poultry: `POU-2.M3.X1`, `POU-2.M4.X1`. Meat: `MEA-2.M3`, `MEA-2.M5.X1`. Fish and shellfish: `FSH-2.M3`, `FSH-2.M4.X1` |
   | `TS-FAB` 2 | Portioning: `KNF-2.M2.X1`. Chicken: `POU-2.M2.X1`. Carving and tying: `MEA-2.M4.X1`. Round fish: `FSH-2.M2.X1` |
   | `TS-MOI` 4 | Simmer: `STK-2.M2.X1`. Poultry poach: `POU-2.M5.X1`. Fish poach and steaming: `FSH-2.M3.X1` |
   | `TS-EGG` 4 | *Sabayon* and poached eggs for four: `SAU-2.M5.X1`. Stirred and baked custards: `BAK-2.M5.X1` |
   | `IN-DAI` 3 | The explanation and a cream sauce: `LEG-2.M3.X1`. A stirred custard: `BAK-2.M5.X1` |
   | `TS-SAU` 3 | One element each in `SAU-2.M2` to `SAU-2.M6`, four of the five needed |
   | `KM-WAST` 3 | `STK-2.M2.X2`, `POU-2.M5.X1` (two meals), `LEG-2.M2.X1` |
   | `FL-SEAS` 3 | `STK-2.M3.X1` and the two stews of `MEA-2.M6.X1` |
   | `IN-SEL` 3 | Two trips: `MEA-2.M2.X1`. Seasonal quiz and a third trip: `FSH-2.M1.X1` |
   | `IN-MEA` 3 | Poultry: `POU-2.M3.X1`. Buying: `MEA-2.M2.X1`. Cooking: `MEA-2.M3`, `MEA-2.M6` |
   | `IN-FSH` 3 | Round fish: `FSH-2.M1.X1`. Bivalve: `FSH-2.M4.X1` |
   | `RD-RATO` 2 | Ratios: `STK-2.M5.X1`. Roux sauce: `SAU-2.M3.X2`. Vinaigrette: `SAU-2.M6.X1`. Baking: `BAK-2.M1.X1` |
   | `KM-MATH` 3 | Formulas and scaling: `BAK-2.M1.X1`. The bread formula baked: `BRD-2.M2.X1` |
   | `KM-EQPT` 3 | Thermometer and oven: `HEA-2`. Knife: `KNF-2.M3.X1`. Baking oven, scale and tins: `BAK-2.M1.X1`. Bread oven: `BRD-2.M3.X1` |
   | `SA-HYG` 4 | Raw-protein portioning: `KNF-2.M2.X1`. Raw chicken: `POU-2.M1.X1`. Further evidence in `MEA-2.M5`, `FSH-2.M2` and `FSH-2.M4` |
   | `SC-STRC` 3 | Starches: `SCI-2.M3.X1`. Dough: `BRD-2.M1.X1` |
   | `SC-MICR` 3 | Hurdles of five foods: `SCI-2.M6.X1`. Fermentation: `BRD-2.M1.X1`. The learner's own preserves: `PRS-3` (S3) |

4. **One cross-reference corrected:** `SCI-2` had pointed the warm butter emulsion to `SAU-2.M5`. The module that moves
   `SC-EMUL` is `SAU-2.M2` (mounting with butter), so the reference now points there.
5. **The pressure cooker** stays optional in `LEG-2`. It is tier 2 in Phase 1 §9.4, above the S2 limit, so the course
   never requires it.
6. **Assumptions,** as in S1:
   - prices are rough;
   - produce follows the Dutch seasons, with fish MSC- or ASC-labelled or seasonal North Sea fish;
   - one household member codes samples and introduces faults (`SAU-2.M7.X1`);
   - reference images are to be made in Phase 6.

   Fruit appears only as Dutch seasonal fruit or rhubarb in `BAK-2`, and lemon as a pantry acid.

**Exercise scope notes for S2** (the outline's one-line description is kept, as the S1 review decided; the spec does
more so its module can meet the Phase 4 success row):
- **`SCI-2`:**
  - `M3.X1` includes the short- and long-mixed doughs (`SC-STRC` 3);
  - `M6.X1` logs the batch cooling (`SA-TEMP` 4).
- **`HEA-2`:**
  - `M1.X1` adds a sixth, unfamiliar ingredient;
  - `M2.X1` adds the probe and oven checks (`KM-EQPT` 3);
  - `M4.X1` films the frying station.
- **`KNF-2`:**
  - `M2.X1` films three minutes of raw-protein handling, with a work order (`SA-HYG` 4);
  - `M3.X1` keeps the edge log for four weeks (`JD-REFL` 3).
- **`STK-2`:**
  - `M1.X1` adds a written framework with ten terms (`CK-TERM` 3);
  - `M2.X1` adds the brown stock;
  - `M3.X1` runs the triangle tests in the same week;
  - `M5.X1` reports the fix test.
- **`SAU-2`:**
  - `M1.X1` adds a one-page framework (`CK-TERM` 3);
  - `M2.X1` breaks and repairs a mounted sauce (`SC-EMUL` 3);
  - `M5.X1` adapts the eggs and the sauce for quantity and timing, with a temperature log (`TS-EGG` 4);
  - `M7.X1` runs two sessions, structural and flavour faults, in sauces of the learner's own cooking (`JD-TRBL` 3,
    `FL-CORR` 3).
- **`POU-2`:**
  - `M1.X1` adds the shopping list (`KM-PURC` 2);
  - `M5.X1` poaches the bird with the liquid logged (`TS-MOI` 4);
  - `M6.X1` cools and reheats the leftovers (`SA-TEMP` 4), with the prediction log spread over three sessions.
- **`MEA-2`:**
  - `M1.X1` weighs the pieces for cooking loss (`SC-PROT` 3);
  - `M2.X1` adds a second shopping trip and photos (`IN-SEL` 3);
  - `M4.X1` adds the sear-first against reverse-sear comparison, caramelised onions, and trimming and tying (`SC-BRWN`
    3, `TS-FAB` 2);
  - `M5.X1` films the shaping and cools and reheats the batch (`SA-HYG` 4, `SA-TEMP` 4);
  - `M6.X1` is made twice, with two classic stews (`FL-SEAS` 3).
- **`FSH-2`:**
  - `M2.X1` fillets a whole round fish, films the work and photographs the week's fridge (`TS-FAB` 2, `SA-HYG` 4,
    `IN-STO` 3);
  - `M4.X1` adds a starter of probed prawns, and films the mussel check (`TS-PRO` 3, `SA-HYG` 4).
- **`LEG-2`:**
  - `M3.X1` adds a reduced cream sauce and a written explanation (`IN-DAI` 3);
  - `M4.X1` adds an allergen-free constraint and a predicted substitution (`SA-ALLG` 3, `RD-ADPT` 2).
- **`BAK-2`:**
  - `M1.X1` adds a flour comparison bake, a milk-free batch, and the oven, scale and tin checks (`IN-BAK` 3, `SA-ALLG`
    3, `KM-EQPT` 3);
  - `M4.X2` adds a correcting third bake (`JD-TRBL` 3);
  - `M5.X1` adds a *crème anglaise* and baked custards in two sizes (`TS-EGG` 4, `IN-DAI` 3);
  - `M6.X1` adds the baking-log review (`JD-REFL` 3).
- **`BRD-2`:**
  - `M2.X1` keeps the cue-then-reading entries (`JD-CUES` 3);
  - `M3.X1` logs the oven and preheat times (`KM-EQPT` 3);
  - `M5.X1` adds a dinner as a parallel task and the bread-log review (`KM-PLAN` 3, `JD-REFL` 3).

Hours, lessons and `Moves` entries are unchanged.

### 8.3 Gaps and open questions (review answers, 2026-11-04)

1. **The steak row in §9** (decision 1).
   - Option 1: keep it. Rare and medium-rare steaks are taught, checked by the surface rule, and not served to risk
     groups. This is my recommendation: it is the Food Code's own rule for professional kitchens, and it follows the
     common reasoning that the bacteria of an intact steak are on its surface. The Voedingscentrum page on meat should
     be checked for the Dutch wording before Phase 6.
   - Option 2: remove it, and teach steaks to 63 °C only. `MEA-2.M1.X1` and `MEA-2.M3.X1` would then lose their rare
     and medium-rare targets.

   **Answer:** option 1. The row stays.
2. **The Food Code's oven chart for roasts** (§3-401.11(B)(2): a still oven at 177 °C or more for roasts under 4.5 kg)
   is not in §9. The reverse sear of `MEA-2.M4` uses a low oven and meets §9's core time-temperature table.
   - Option 1: keep §9 as it is, and add a note that the oven chart is not adopted for home roasting. This is my
     recommendation.
   - Option 2: adopt the chart. `MEA-2.M4` would then drop the reverse sear for small joints.

   **Answer:** option 1. §9 now says the chart is not adopted for home roasting; roasts are judged by the core table.
3. **No figure for raw-bean lectins.** `LEG-2.M1.L1` says to boil dried kidney beans hard for several minutes before
   simmering, without a number, because §9 has none. I recommend adding a checked figure, with its source in
   `sources.md`, before Phase 6 writes the lesson.

   **Answer:** added. The FDA *Bad Bug Book* [S33] gives the figure: soaked at least 5 hours with the water poured
   away, then boiled in fresh water for at least 30 minutes, never in a slow cooker. It applies to the common beans
   (*Phaseolus vulgaris*), not to chickpeas, lentils or split peas. The source differs from the "several minutes" the
   draft assumed, which is why no figure was written from memory. `LEG-2.M1.X2` now compares chickpeas, because its
   unsoaked pots would break the rule for common beans.
4. **The knowledge-check items have not been reviewed by a second reader** (as in S1). There are 408 items, 62 of them
   safety items. A read-through of the safety items is worth it.

   **Answer:** done for all 62 safety items, each checked against §9 and its *Why* line. Four were changed:
   - `LEG-2.M1.Q4`: the bean figure above;
   - `HEA-2.M4.Q3`: the ordering item put "call 112" after "leave it covered until cold", which made no sense. It now
     orders three steps, and the *Why* says when to leave and call 112;
   - `SCI-2.M6.Q7`: the model answer also names the 6-hour step of the cooling rule;
   - `FSH-2.M4.Q3`: the *Why* now gives the reason for discarding mussels that stay shut.

   No other answer disagreed with §9. This was a careful reading by the author, not an independent check. A second
   human reader is still advised for the safety items of every stage before Phase 6 publishes them.
5. **The budget.** About €725 for S2, against €265 for S1, mostly meat and fish that replace household meals.
   - Is that acceptable?
   - Or should the Good options become the default in the specs (for example frozen MSC fish, *kogelbiefstuk*, chicken
     legs), which would bring it to roughly €550?

   **Answer:** €725 is acceptable. The specs stay as they are, with the cheaper options as alternatives.

### 8.4 Recommendations for S3

1. **Keep mapping before writing.** Checking each module's evidence row against its success row first caught every
   exercise extension above before the course was written.
2. **Settle the low-temperature figures before `SLW-3`.** Sous vide, confit and slow cooking need tested
   pasteurisation tables in §9, and possibly the lightly cooked fish of `CUI-3`. They should be agreed before S3 is
   written, as the S1 recommendations already noted.
3. **`PRS-3` and `SA-HRPR`:** the home-preserving rule of §9 ("tested methods only") needs named, checkable sources
   for each method taught (water-bath canning, fermentation, curing salts).
4. **Tier 2 equipment** (a circulator, fermentation jars, a 0.01 g scale, a mandoline, the `FIR-3` grill) gets its Good
   and Best where it first appears in S3.
5. **Write `CUI-3` (34 lessons) as its own batch.** It is the largest course of the program.
6. **A small validator addition:** check that every exercise ID named in an evidence row's text exists. The S2 rows
   name exercises in other courses (§8.2, decision 3); the check would keep those references from drifting.
