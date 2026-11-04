# Phase 5 — Course development

Status: **S1 reviewed; answers applied** (2026-11-04). S2–S5 follow one stage at a time, in the format agreed on S1.
The end of the S1 batch, with its decisions and the review answers, is in §7.

**Stages developed:** S1

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
each level. ✗ marks a stage target that no exercise up to that stage evidences; *(quiz)* marks a target whose success
row rests on the quiz alone.

<!-- BEGIN generated:reachability -->
| Competency | S1 | S2 (in progress) | Exercises at L2 · L3 · L4 · L5 |
|---|---|---|---|
| `CK-TERM` | 2 (quiz) | 3 | 0 · 1 · 0 · 0 |
| `CK-METH` | 2 | 3 | 1 · 2 · 0 · 0 |
| `CK-RCPT` | 3 | 3 | 0 · 2 · 0 · 0 |
| `CK-CLAS` | – | 2 ✗ | 0 · 0 · 0 · 0 |
| `CK-CUIS` | – | 1 | 0 · 0 · 0 · 0 |
| `IN-SEL` | 2 | 3 ✗ | 1 · 0 · 0 · 0 |
| `IN-STO` | 3 | 3 | 1 · 4 · 0 · 0 |
| `IN-VEG` | 2 | 3 | 3 · 2 · 0 · 0 |
| `IN-STA` | 2 | 3 ✗ | 4 · 0 · 0 · 0 |
| `IN-DAI` | 2 | 3 ✗ | 2 · 0 · 0 · 0 |
| `IN-MEA` | 1 | 3 ✗ | 0 · 0 · 0 · 0 |
| `IN-FSH` | 1 | 3 ✗ | 0 · 0 · 0 · 0 |
| `IN-ARO` | 1 | 2 | 1 · 0 · 0 · 0 |
| `IN-BAK` | – | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-MEP` | 3 | 4 | 1 · 1 · 1 · 0 |
| `TS-KNF` | 3 | 4 | 4 · 3 · 2 · 0 |
| `TS-DRY` | 2 | 3 | 3 · 4 · 0 · 0 |
| `TS-MOI` | 3 | 4 | 0 · 2 · 1 · 0 |
| `TS-CMB` | – | 2 | 1 · 0 · 0 · 0 |
| `TS-EGG` | 3 | 4 ✗ | 1 · 3 · 0 · 0 |
| `TS-VEG` | 3 | 4 | 0 · 4 · 3 · 0 |
| `TS-STA` | 3 | 3 | 1 · 2 · 0 · 0 |
| `TS-STK` | – | 3 | 0 · 5 · 0 · 0 |
| `TS-SAU` | – | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-PRO` | 1 | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-FAB` | – | 2 | 1 · 0 · 0 · 0 |
| `TS-BAK` | – | 3 ✗ | 0 · 0 · 0 · 0 |
| `TS-BRD` | – | 2 ✗ | 0 · 0 · 0 · 0 |
| `TS-PLT` | 1 | 2 ✗ | 0 · 0 · 0 · 0 |
| `SC-HEAT` | 2 | 3 | 3 · 2 · 0 · 0 |
| `SC-BRWN` | 2 | 3 | 1 · 3 · 0 · 0 |
| `SC-PROT` | 2 | 3 | 1 · 2 · 0 · 0 |
| `SC-STRC` | 2 (quiz) | 3 | 0 · 1 · 0 · 0 |
| `SC-EMUL` | 1 | 3 | 0 · 1 · 0 · 0 |
| `SC-MICR` | 1 | 3 | 0 · 1 · 0 · 0 |
| `FL-SENS` | 2 | 3 | 3 · 1 · 0 · 0 |
| `FL-SEAS` | 2 | 3 | 4 · 1 · 0 · 0 |
| `FL-TEXT` | 1 | 2 | 1 · 0 · 0 · 0 |
| `FL-ARCH` | – | 2 | 1 · 0 · 0 · 0 |
| `FL-CORR` | 1 | 3 | 0 · 1 · 0 · 0 |
| `KM-PLAN` | 2 | 3 ✗ | 2 · 0 · 0 · 0 |
| `KM-MATH` | 2 | 3 ✗ | 1 · 0 · 0 · 0 |
| `KM-PURC` | 1 | 2 ✗ | 0 · 0 · 0 · 0 |
| `KM-WAST` | 2 | 3 | 2 · 1 · 0 · 0 |
| `KM-EQPT` | 2 | 3 | 3 · 3 · 0 · 0 |
| `SA-HYG` | 3 | 4 | 1 · 1 · 1 · 0 |
| `SA-TEMP` | 3 | 4 | 1 · 3 · 2 · 0 |
| `SA-PHYS` | 3 | 4 | 0 · 2 · 1 · 0 |
| `SA-ALLG` | 2 | 3 ✗ | 1 · 0 · 0 · 0 |
| `SA-HRPR` | – | 1 | 0 · 0 · 0 · 0 |
| `RD-RATO` | – | 2 | 1 · 0 · 0 · 0 |
| `RD-ADPT` | 1 | 2 | 1 · 0 · 0 · 0 |
| `RD-TEST` | 1 | 2 | 2 · 0 · 0 · 0 |
| `RD-WRIT` | 2 | 2 | 1 · 0 · 0 · 0 |
| `JD-CUES` | 2 | 3 ✗ | 4 · 0 · 0 · 0 |
| `JD-CHSE` | 1 | 2 | 2 · 0 · 0 · 0 |
| `JD-TRBL` | 1 | 3 ✗ | 1 · 0 · 0 · 0 |
| `JD-REFL` | 2 | 3 | 4 · 1 · 0 · 0 |
| `CR-COMP` | – | 1 | 0 · 0 · 0 · 0 |
| `CR-IMPV` | – | 1 | 0 · 0 · 0 · 0 |
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

No course, module, lesson, hour, exercise or `Moves` entry has changed.

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
| Cast-iron or carbon-steel skillet | 0 | `EQP-1`, `HEA-1` |
| Chef's knife (20 cm) | 0 | `EQP-1`, `KNF-1`, `KNF-2` |
| Deep heavy pot for frying | 0 | `HEA-2` |
| Digital scale (1 g) | 0 | `ORI-1`, `TST-1`, `WRK-1`, `KNF-2`, `SCI-2` |
| Heavy saucepan with a lid | 0 | `STA-1` |
| Heavy stainless sauté pan | 0 | `EQP-1`, `HEA-1`, `VEG-1` |
| Honing steel | 0 | `EQP-1`, `KNF-1` |
| Instant-read thermometer | 0 | `EGG-1`, `EQP-1`, `HEA-1`, `ORI-1`, `SAF-1`, `HEA-2`, `SCI-2`, `STK-2` |
| Large stable board | 0 | `EQP-1`, `KNF-1` |
| Mixing bowls | 0 | `WRK-1`, `SCI-2` |
| Oven thermometer | 0 | `EQP-1`, `ORI-1` |
| Paring knife | 0 | `EQP-1`, `KNF-1` |
| Saucepans | 0 | `EQP-1`, `HEA-1`, `VEG-1`, `SCI-2` |
| Sheet pan as a tray | 0 | `WRK-1` |
| Sheet pans | 0 | `EQP-1`, `HEA-1`, `STA-1`, `VEG-1` |
| Sieve | 0 | `STA-1` |
| Small saucepan | 0 | `EGG-1` |
| Stockpot | 0 | `EQP-1`, `STK-2` |
| Stockpot or large pot | 0 | `STA-1` |
| Tongs | 0 | `HEA-1` |
| Fine sieve | 1 | `EGG-1`, `SCI-2`, `STK-2` |
| Frying thermometer | 1 | `HEA-2` |
| Heavy lidded casserole (*braadpan*) | 1 | `HEA-2` |
| Whetstone | 1 | `KNF-2` |
| Blender | — | `STK-2` |
| Boning knife | — | `KNF-2` |
| Calibration kit: coded cups | — | `TST-1` |
| Calibration kit: key sheets | — | `TST-1` |
| Camera | — | `ORI-1` |
| Containers with lids | — | `KNF-2` |
| Deep roasting tin for a water bath | — | `EGG-1` |
| Fire blanket | — | `SAF-1`, `HEA-2` |
| First-aid kit | — | `SAF-1` |
| Fish spatula | — | `HEA-1` |
| Freezer containers | — | `STK-2` |
| Fridge thermometer | — | `SAF-1` |
| Grid board or ruler | — | `KNF-1`, `ORI-1`, `KNF-2` |
| Grill pan | — | `HEA-2` |
| Hand towels and dish cloths | — | `SAF-1` |
| Jars with lids | — | `SCI-2` |
| Journal | — | `ORI-1` |
| Kitchen string | — | `STK-2` |
| Kitchen timer | — | `KNF-1`, `WRK-1` |
| Labels and pen | — | `ORI-1`, `SAF-1` |
| Ladle | — | `STA-1` |
| Ladle and skimmer | — | `STK-2` |
| Measuring jug and spoons | — | `WRK-1` |
| Measuring jugs or bottles (1 l) | — | `TST-1` |
| Measuring spoons and a pipette | — | `TST-1` |
| Muslin or cheesecloth | — | `STK-2` |
| Non-slip mat or damp towel | — | `KNF-1` |
| Omelette pan (20 cm) | — | `EGG-1` |
| Peeler | — | `KNF-1` |
| pH strips | — | `SCI-2` |
| Phone stand | — | `KNF-1` |
| Potato masher | — | `VEG-1` |
| Precision scale (0.1 g) | — | `TST-1` |
| Ramekins | — | `EGG-1` |
| Roasting rack | — | `HEA-2` |
| Salad spinner | — | `VEG-1` |
| Seasonal calendar | — | `VEG-1` |
| Second cutting board for raw protein | — | `SAF-1`, `KNF-2` |
| Shallow containers with lids | — | `SAF-1`, `STA-1` |
| Slotted spoon | — | `EGG-1` |
| Small prep bowls | — | `WRK-1` |
| Spider or slotted spoon | — | `HEA-2` |
| Steamer | — | `HEA-1`, `VEG-1` |
| Stick blender | — | `EGG-1` |
| Tasting cups | — | `ORI-1` |
| Tasting form | — | `TST-1` |
| Wire rack | — | `HEA-2` |
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

