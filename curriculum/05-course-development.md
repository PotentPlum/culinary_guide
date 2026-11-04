# Phase 5 — Course development

Status: **in progress**. Stage S1 is being written and comes up for review first (review decision, 2026-11-04). S2–S5
follow one stage at a time, in the format agreed on S1.

**Stages developed:** —

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
| **Evidence** | The evidence methods first (`photo, measure — …`), then what to capture. Where AI review scores a photo or video, the field names the **reference images** Phase 6 must produce (*Competent* and *Not yet*) and the short rubric the AI scores against ([assessment §6](04-assessment.md#6-ai-review)). |
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
objectives has no quiz. The items are written out:

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
- the exercises that supply the evidence, each tagged at that level;
- the evidence methods and what must be shown.

The standard is the competency's Phase 4 success row at that level ([assessment §4.2](04-assessment.md#42-evidence-check)).
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
| S1 | `SAF-1` | 0 / 10 | — | — | — | — | — |
| S1 | `WRK-1` | 0 / 11 | — | — | — | — | — |
| S1 | `EQP-1` | 0 / 9 | — | — | — | — | — |
| S1 | `KNF-1` | 0 / 15 | — | — | — | — | — |
| S1 | `TST-1` | 0 / 13 | — | — | — | — | — |
| S1 | `HEA-1` | 0 / 17 | — | — | — | — | — |
| S1 | `EGG-1` | 0 / 11 | — | — | — | — | — |
| S1 | `VEG-1` | 0 / 15 | — | — | — | — | — |
| S1 | `STA-1` | 0 / 11 | — | — | — | — | — |
| S2 | `SCI-2` | 0 / 11 | — | — | — | — | — |
| S2 | `HEA-2` | 0 / 12 | — | — | — | — | — |
| S2 | `KNF-2` | 0 / 5 | — | — | — | — | — |
| S2 | `STK-2` | 0 / 14 | — | — | — | — | — |
| S2 | `SAU-2` | 0 / 12 | — | — | — | — | — |
| S2 | `POU-2` | 0 / 13 | — | — | — | — | — |
| S2 | `MEA-2` | 0 / 14 | — | — | — | — | — |
| S2 | `FSH-2` | 0 / 12 | — | — | — | — | — |
| S2 | `LEG-2` | 0 / 10 | — | — | — | — | — |
| S2 | `BAK-2` | 0 / 17 | — | — | — | — | — |
| S2 | `BRD-2` | 0 / 11 | — | — | — | — | — |
| S3 | `SAF-3` | 0 / 5 | — | — | — | — | — |
| S3 | `FLV-3` | 0 / 14 | — | — | — | — | — |
| S3 | `BUT-3` | 0 / 9 | — | — | — | — | — |
| S3 | `SLW-3` | 0 / 12 | — | — | — | — | — |
| S3 | `FIR-3` | 0 / 12 | — | — | — | — | — |
| S3 | `GRD-3` | 0 / 11 | — | — | — | — | — |
| S3 | `PRS-3` | 0 / 10 | — | — | — | — | — |
| S3 | `VEG-3` | 0 / 10 | — | — | — | — | — |
| S3 | `PST-3` | 0 / 14 | — | — | — | — | — |
| S3 | `BRD-3` | 0 / 11 | — | — | — | — | — |
| S3 | `CUI-3` | 0 / 34 | — | — | — | — | — |
| S3 | `KMG-3` | 0 / 9 | — | — | — | — | — |
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
each level. ✗ marks a stage target that no exercise up to that stage evidences.

<!-- BEGIN generated:reachability -->
| Competency | S1 (in progress) | Exercises at L2 · L3 · L4 · L5 |
|---|---|---|
| `CK-TERM` | 2 ✗ | 0 · 0 · 0 · 0 |
| `CK-METH` | 2 ✗ | 0 · 0 · 0 · 0 |
| `CK-RCPT` | 3 ✗ | 0 · 0 · 0 · 0 |
| `IN-SEL` | 2 ✗ | 0 · 0 · 0 · 0 |
| `IN-STO` | 3 ✗ | 0 · 0 · 0 · 0 |
| `IN-VEG` | 2 ✗ | 0 · 0 · 0 · 0 |
| `IN-STA` | 2 ✗ | 0 · 0 · 0 · 0 |
| `IN-DAI` | 2 ✗ | 0 · 0 · 0 · 0 |
| `IN-MEA` | 1 | 0 · 0 · 0 · 0 |
| `IN-FSH` | 1 | 0 · 0 · 0 · 0 |
| `IN-ARO` | 1 | 0 · 0 · 0 · 0 |
| `TS-MEP` | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-KNF` | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-DRY` | 2 ✗ | 0 · 0 · 0 · 0 |
| `TS-MOI` | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-EGG` | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-VEG` | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-STA` | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-PRO` | 1 | 0 · 0 · 0 · 0 |
| `TS-PLT` | 1 | 0 · 0 · 0 · 0 |
| `SC-HEAT` | 2 ✗ | 0 · 0 · 0 · 0 |
| `SC-BRWN` | 2 ✗ | 0 · 0 · 0 · 0 |
| `SC-PROT` | 2 ✗ | 0 · 0 · 0 · 0 |
| `SC-STRC` | 2 ✗ | 0 · 0 · 0 · 0 |
| `SC-EMUL` | 1 | 0 · 0 · 0 · 0 |
| `SC-MICR` | 1 | 0 · 0 · 0 · 0 |
| `FL-SENS` | 2 ✗ | 0 · 0 · 0 · 0 |
| `FL-SEAS` | 2 ✗ | 0 · 0 · 0 · 0 |
| `FL-TEXT` | 1 | 0 · 0 · 0 · 0 |
| `FL-CORR` | 1 | 0 · 0 · 0 · 0 |
| `KM-PLAN` | 2 ✗ | 0 · 0 · 0 · 0 |
| `KM-MATH` | 2 ✗ | 0 · 0 · 0 · 0 |
| `KM-PURC` | 1 | 0 · 0 · 0 · 0 |
| `KM-WAST` | 2 ✗ | 0 · 0 · 0 · 0 |
| `KM-EQPT` | 2 ✗ | 0 · 0 · 0 · 0 |
| `SA-HYG` | 3 ✗ | 0 · 0 · 0 · 0 |
| `SA-TEMP` | 3 ✗ | 0 · 0 · 0 · 0 |
| `SA-PHYS` | 3 ✗ | 0 · 0 · 0 · 0 |
| `SA-ALLG` | 2 ✗ | 0 · 0 · 0 · 0 |
| `RD-ADPT` | 1 | 0 · 0 · 0 · 0 |
| `RD-TEST` | 1 | 0 · 0 · 0 · 0 |
| `RD-WRIT` | 2 ✗ | 0 · 0 · 0 · 0 |
| `JD-CUES` | 2 ✗ | 0 · 0 · 0 · 0 |
| `JD-CHSE` | 1 | 0 · 0 · 0 · 0 |
| `JD-TRBL` | 1 | 0 · 0 · 0 · 0 |
| `JD-REFL` | 2 | 2 · 0 · 0 · 0 |
<!-- END generated:reachability -->

## 5. Decisions and change log

1. **Stage by stage** (review decision). S1 is written and reviewed first; the format is fixed by that review.
2. **A knowledge check in every module.** Some outline *Assessment* lines name only evidence. Every module whose lessons
   have a [K] or [J] objective still gets a knowledge check, sized by the blueprint. The outline is not changed: its
   *Assessment* line names the evidence, and the course file adds the quiz.
3. **One full check per module now; the bank of three in Phase 6** (review decision).
4. **Exercises are tagged** with the competencies they evidence (§1.2). The Phase 4 mapping blocks still show the
   module-level approximation; the tags are in the course files and in the reachability table.

No outline change has been made in this phase so far.

## 6. Equipment across the developed courses (generated)

<!-- BEGIN generated:equipment -->
| Item | Tier | Courses |
|---|---|---|
| Digital scale (1 g) | 0 | `ORI-1` |
| Instant-read thermometer | 0 | `ORI-1` |
| Oven thermometer | 0 | `ORI-1` |
| Camera | — | `ORI-1` |
| Grid board or ruler | — | `ORI-1` |
| Journal | — | `ORI-1` |
| Labels and pen | — | `ORI-1` |
| Tasting cups | — | `ORI-1` |
<!-- END generated:equipment -->
