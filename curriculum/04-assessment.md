# Phase 4 — Assessment design

Status: **reviewed; answers applied** (2026-11-04). This document defines how the program assesses the learner:
- the assessment types and criteria;
- how rubrics are scored and passed;
- the module checks;
- the calibration tests that make taste judgments usable;
- the role of AI review;
- the full briefs and weighted rubrics of the four milestones and the capstone;
- the safety figures that assessments check against.

It applies the principles of [Phase 1 §10](01-vision-architecture.md#10-assessment-approach-overview) and uses the
evidence methods and the evidence model of the [competency framework](04-competency-framework.md) (§1.3, §2).

**No qualification.** Passing these assessments gives no diploma or professional certification (README). They exist to
tell the learner, honestly, what they can do.

---

## 1. Assessment types

| Type | When | What it decides | Who scores |
|---|---|---|---|
| **Practical exercise** | In every module (Phase 2 `X` items) | Nothing on its own; each attempt can add an entry to the evidence record (framework §2). | Learner, with AI review of photos and video, and tasters where named |
| **Module check** | At the end of every module (Phase 2 *Assessment* line) | Whether the module counts as passed (§4). | Auto-graded quiz; evidence against the module's success rows |
| **Calibration test** | Once in S1 (`TST-1`, `MS-S1`), then every 6 months, and a fault-diagnosis test at `MS-S3` (§5) | Whether the learner's own sensory forms count as evidence (framework §2.4). | Objective: the answers are known |
| **Milestone** | At the end of S1–S4 (§7) | Part of the stage gate (framework §3.1). | Rubric (§3), scored as in §3.4 |
| **Capstone** | At the end of S5 (§8) | Completion of the core program, with the evidence record. | Weighted rubric (§3, §8) |
| **Review** | When a competency is due (about 6 months without evidence; framework §2.5) | Whether the competency's level stays current. | As a practical exercise |

---

## 2. Assessment criteria

The brief asks the assessment framework to measure ten things. Three more are needed for milestones and the capstone.
Each rubric row scores one of them.

| Criterion | What it means | Typical evidence | Verifiable? |
|---|---|---|---|
| technical execution | The technique is done correctly and the product meets its standard (doneness, structure, emulsion, crumb, clarity) | `photo`, `measure`, `video` | Partly: the visible and measured parts |
| accuracy | Results are within tolerance: cut dimensions, weights, temperatures, yields, costs, calibration answers | `measure`, `calibration`, `quiz` | Yes |
| timing | Things are ready at the planned time; the timeline holds | `log` (timestamps against the plan) | Yes |
| consistency | The same standard every time: across plates of one course, and across repeated demonstrations | `photo` of every plate; the evidence record (framework §2.3) | Partly |
| texture | Texture and contrast are as intended (crisp, tender, smooth, set) | `photo` (cross-section), `form`, `tasters` | Partly |
| flavour | Seasoning, balance, depth and aroma | `form` (calibrated), `tasters`, `calibration` | Self-assessed, made usable by calibration |
| presentation | Composition, colour, height, cleanliness, temperature on the plate | `photo` | Partly |
| organisation | Mise en place, sequence, clean as you go, use of equipment | `log`, `photo`, `checklist`, `video` | Partly |
| food safety | Hygiene, temperatures, cooling, storage, allergens, physical safety | `measure`, `log`, `checklist`, `video`, `quiz` | Partly: readings and quizzes yes, practice in the kitchen only by checklist and video |
| independent decisions | Choices made from principles, without a recipe, and adapted when something changes | `written`, the decisions visible in the result | Partly |
| knowledge | Understanding of principles, terms and rules | `quiz`, `written` | Yes (quiz) |
| planning | Menus, quantities, costing, timelines, before cooking starts | `written`, `measure` (costing against receipts) | Partly |
| reflection | An honest, evidence-based review, with next steps | `written` | Partly |

**Consistency** is assessed in two ways:
- across the plates of one course, at `MS-S4` and the capstone;
- above all in the **evidence record**, where levels 4 and 5 need repeated demonstrations under varied conditions over
  time (framework §2.3).

One good evening never proves consistency on its own.

---

## 3. Scoring and pass rules

### 3.1 Bands

Every weighted rubric row is scored on four bands. The row's *Competent* column states the standard. The other bands
are relative to it:

| Band | Score | Meaning |
|---|---|---|
| Not yet | 0 | Below the standard in a way that matters: a fault a guest would notice, a tolerance missed by a wide margin, or missing evidence. |
| Developing | 1 | Close to the standard, with one clear fault. |
| Competent | 2 | Meets the standard as stated in the row. |
| Excellent | 3 | Meets it with control and refinement: better than the tolerance, no fault at all, and choices that clearly add something. |

**Gate rows** (food safety) are scored **Pass** or **Fail**. They carry no weight and cannot be compensated by other rows.

### 3.2 Combining the rows

- The **row score** is the band divided by 3, times the row's weight. The weights of a rubric add up to 100.
- The **milestone score** is the sum of the row scores (0–100). A rubric with every row at *Competent* scores 66.7.

### 3.3 Pass rules

| Assessment | Pass | Also |
|---|---|---|
| Milestone (`MS-S1`–`MS-S4`) | Every gate passed, no weighted row at *Not yet*, and a score of at least **66.7** (Competent on average). | — |
| Capstone (`CAP-5`) | The same rules. | **Distinction:** at least 85, and no row below *Competent*. |

**Resits.**
- **One or two components below the standard** (a gate failed, or a row at *Not yet*): only those components are
  retaken, after at least one week with targeted practice. The rest of the result stands.
- **More than two components below the standard, or a score below 66.7:** the whole milestone is retaken, no earlier
  than four weeks later.
- **The capstone:** it is retaken with a new brief. The evidence of passed rows may be kept for up to 6 months.
- **No limit:** there is no limit on resits. Every attempt goes into the record.

### 3.4 Who scores, and how disagreements are settled

1. **The learner** scores every row first, against the *Competent* standard, with the evidence attached.
2. **AI review** scores the rows that rest on photos or video (§6).
3. **Tasters** fill in their cards for the flavour and texture rows.
4. **Disagreements:**
   - When the learner's band and the AI band or the tasters' consensus differ by **one band**, the learner's band
     stands.
   - When they differ by **two bands or more**, the **lower** band stands, unless the learner adds evidence that settles
     it, for example a second photo, a reading or a blind re-taste. The learner writes down the disagreement in the
     journal.
5. **Gates** are scored from the readings and checklists. A missing reading counts as a Fail.

This rule errs on the side of caution, because no human examiner is assumed (Phase 1, review answer Q4). The Phase 4
review confirmed it: there is no outside examiner, not even an optional one for the capstone.

---

## 4. Module checks

Every module of the outline ends with a **check**: a quiz, an evidence check, or both, as named in its *Assessment*
line. A module counts as passed when every part of its check passes.

### 4.1 Quiz blueprint

The quiz items themselves are written in Phase 5. Phase 4 fixes the blueprint:

| Rule | Value |
|---|---|
| Items per check | 2 per [K] objective of the module, plus 1 per [J] objective that can be put as a scenario. At least 8 and at most 20 items. |
| Item types | Multiple choice; scenario ("what do you do next, and why"); calculation (scaling, baker's percentages, yield, cost); image identification (cut, doneness, fault, crumb); ordering (steps, priorities). |
| Pass mark | **80 %**, and **every safety item correct**: items on objectives that move an `SA-*` competency, or on a safety rule. A wrong safety item means a retake, whatever the total. |
| Item bank | At least three times the items per check, so that a retake draws different items. |
| Retake | After reviewing the lessons the failed items came from; no fixed wait. |

### 4.2 Evidence check

An evidence check asks for the evidence named in the module's *Assessment* line, such as photos, logs, forms or tasters'
cards, from at least one of the module's exercises. It passes when, for each competency the module's lessons move, the
evidence meets the competency's success row for the **highest level the module reaches**
([`04-competencies/`](04-competencies/)). Level 1 is covered by the quiz.

---

## 5. Calibration tests

Taste cannot be checked from a photo, but a palate can be calibrated. These tests have known answers, so they are
objective evidence (`calibration`). They follow the standard methods of sensory science [S20]: ranking tests and
triangle tests with coded, randomised samples. Someone else in the household prepares and codes the samples. If no one
can, the learner prepares them in advance and lets them cool and mix up, using numbered cups and a hidden key.

### 5.1 Salt series

- **Base:** unsalted vegetable stock or water with a little sugar-free stock base, at serving temperature.
- **Samples:** 6, 8, 10, 12 and 15 g of salt per litre (0.6–1.5 %). A full litre per sample keeps the weighing
  accurate enough on the 1 g kitchen scale of the basic equipment tier (Phase 1 §9.4); a 0.1 g scale is better.
- **Ranking test:** the five samples, coded, in random order. The learner puts them in order.
  - **Pass:** at most one swap between neighbouring samples.
- **Triangle test:** three cups, two the same and one different (10 g/l against 12 g/l). The learner picks the odd one
  out. Six trials, with the odd cup's position and concentration varied.
  - **Pass:** at least 4 of 6. Guessing alone passes in about 10 % of attempts.
- **Preference:** the learner names the concentration they would serve, and the tasters do the same. This is recorded
  but not scored. Large and stable differences from the tasters are worth knowing when cooking for others.

### 5.2 Acid series

- **Base:** the same stock, salted at 10 g/l, so that acid is judged against a seasoned food.
- **Samples:** 0, 5, 10, 15 and 20 ml of white-wine vinegar (about 6 % acidity) per litre.
- **Tests and pass marks:** as for salt. The triangle test compares 10 against 15 ml/l.

### 5.3 Fault diagnosis (from S3)

- **Samples:** one base dish, such as a soup or a sauce, in five coded versions. Four have one fault each, and one is
  correct. The faults: under-salted (flat), over-salted, too acidic, too sweet, bitter or burnt-tasting.
- **Task:** the learner names the fault in each sample and writes a correction. Then they correct two of the samples,
  and blind tasters compare them with the correct version.
- **Pass:** at least four of five faults named. Both corrections are judged at least as good as the correct version by
  most tasters, or not distinguished from it.
- **Used in:** the flavour-correction component of `MS-S3` (`MS-S3.M1.L4`), and as a level-4 demonstration for
  `FL-CORR`.

### 5.4 Taster calibration

Household tasters take the salt triangle test once. Their cards still count if they fail it, but those cards cannot
settle a two-band disagreement (§3.4). This keeps the outside view honest.

### 5.5 Validity

The learner's sensory forms (`form`) count as evidence only while a salt and an acid test passed in the last **6 months**
stand behind them (framework §2.4). A failed calibration is retaken after a week of tasting practice. In the meantime,
flavour claims rest on tasters and on the other evidence.

---

## 6. AI review

AI review of photos and short videos is a standard evidence source (Phase 1 §10.1, review answer Q4). It is
**evidence, not a verdict**.

| It may score | It may not score |
|---|---|
| Cut size and uniformity against a grid or ruler; browning colour and evenness; doneness in a cross-section; crumb, rise and lamination; emulsion sheen or break; sauce coating (*nappe*) on a spoon; plating composition, colour, cleanliness and portion; posture, grip and motion in video (knife, whisk, shaping) | Taste, aroma or seasoning; temperatures, unless a thermometer reading is in the frame; hygiene beyond what the frame shows; anything the photo does not show clearly. In that case it answers *cannot judge*, and the row rests on other evidence. |

**How it works:**
- **Reference images.** Each exercise or rubric row that uses AI review gets reference images in Phase 5 (Competent and
  Not yet examples) and a short rubric. The AI scores against those, not against general taste.
- **Output.** A band per row, a confidence (high, medium or low) and a one-sentence reason. A low-confidence score
  counts as *cannot judge*.
- **Disagreement.** The learner records agreement or disagreement in the journal, and §3.4 settles it. Patterns of
  disagreement are reviewed at every stage reflection: they show either a blind spot of the learner or a weakness of the
  reference images.
- **Photo standards.** Daylight or neutral light, the reference object in the frame (a grid board, ruler or spoon), a
  cross-section where doneness or crumb matters, and the whole plate from above and at 45°.

---

## 7. Milestones

Each milestone re-tests competencies from its stage in combination (Phase 1 §10.4). The components are the rows of
the milestone module in the outline. The rubric scores every competency each component re-tests: the validator checks
that each one is scored, and that each rubric mixes objective, partly verifiable and self-assessed evidence.

**How to read a rubric.**
- **Components** are milestone lesson IDs.
- **Competencies** are what the row re-tests ("—" for a row that is not tied to one competency).
- **Weight** is the share of 100, or *gate* for a food-safety row.

Every milestone ends with the **stage gate** (framework §3.1).

### 7.1 `MS-S1` — Foundations (7 h)

**Purpose.** Show that the foundations are safe, accurate and calibrated before the classical techniques start.

**Brief.**
1. **Timed mise en place** (`MS-S1.M1.L1`, 45 minutes):
   - from a list given at the start: 200 g of carrot in julienne, 200 g of onion in small dice, 100 g of carrot in
     brunoise, chopped parsley, crushed garlic;
   - each cut photographed on a grid board, and 10 random pieces of each measured;
   - the station filmed for 2 minutes from the side.
2. **Two-component meal** (`MS-S1.M1.L2`, from a recipe, 90 minutes):
   - a vegetable side, cooked by a moist or dry method as the recipe states;
   - an egg dish or a starch (an omelette, a rice pilaf or boiled potatoes);
   - both served at the same time;
   - hygiene checklist, photos, and tasters' cards.
3. **Salt and acid calibration** (`MS-S1.M1.L3`): §5.1 and §5.2.
4. **Safety and knowledge quiz** (`MS-S1.M1.L4`, 20 items).
5. **Stage reflection** (`MS-S1.M1.L5`, about 600 words): what improved since the `ORI-1` diagnostic cook (repeated as
   part of the meal), what is still weak, and the plan for S2.

| ID | Criterion | Competent means | Components | Competencies | Evidence | Weight |
|---|---|---|---|---|---|---|
| MS-S1.R1 | Accuracy | Brunoise 2–3 mm, small dice 5–7 mm, julienne 2–3 mm thick: 8 of 10 measured pieces within the range, finished within the time | MS-S1.M1.L1 | TS-KNF | measure, photo, video | 15 |
| MS-S1.R2 | Organisation | Station set up before cutting, waste and product separated, clean as you go; the list finished in 45 minutes | MS-S1.M1.L1 | TS-MEP | log, photo, checklist | 10 |
| MS-S1.R3 | Food safety | Claw grip and a secured board throughout the video; no unsafe knife handling | MS-S1.M1.L1 | SA-PHYS | video, checklist | gate |
| MS-S1.R4 | Technical execution | The vegetable cooked to the stated texture with even colour; the egg or starch set and cooked through as specified; the right vegetable prepared the right way for the method | MS-S1.M1.L2 | TS-DRY, TS-MOI, TS-VEG, TS-EGG, IN-VEG | photo, form | 20 |
| MS-S1.R5 | Flavour | Both components salted at the right stages and balanced; the tasters call neither under- nor over-seasoned | MS-S1.M1.L2 | FL-SEAS | form, tasters | 10 |
| MS-S1.R6 | Food safety | Hand washing, separate boards for raw and ready-to-eat food, eggs handled safely, leftovers into the fridge within 2 hours (§9) | MS-S1.M1.L2 | SA-HYG | checklist, photo | gate |
| MS-S1.R7 | Accuracy | Salt and acid series passed (§5.1, §5.2) | MS-S1.M1.L3 | FL-SENS, FL-SEAS | calibration | 15 |
| MS-S1.R8 | Knowledge | The knowledge items at 80 % or more | MS-S1.M1.L4 | SC-HEAT, CK-METH, CK-TERM, IN-STO | quiz | 15 |
| MS-S1.R9 | Food safety | Every safety item correct: temperatures, the fridge, allergens | MS-S1.M1.L4 | SA-TEMP, SA-ALLG | quiz | gate |
| MS-S1.R10 | Reflection | Names at least two specific improvements and two weak competencies, each with evidence from the journal, and a concrete plan | MS-S1.M1.L5 | JD-REFL | written | 15 |

### 7.2 `MS-S2` — Core techniques (16 h)

**Purpose.** Show the classical core: stock-based or emulsified sauces, proteins to a stated doneness, garnishes,
baking, and the science behind them.

**Brief.**
1. **Plan** (`MS-S2.M1.L1`). The brief names:
   - a protein: chicken thigh, pork chop, or a skin-on fish fillet;
   - its doneness;
   - a sauce family: pan sauce, velouté or warm emulsion;
   - a season.

   The learner writes a framework-based plan: a timeline, a prep list, a shopping list, and notes on the recipe or
   framework used.
2. **Cook and plate** (`MS-S2.M1.L2`, 2 hours from mise en place to plate):
   - the protein with a probe reading and a cross-section photo;
   - the sauce;
   - a vegetable and a starch garnish;
   - two plates, with timestamps and tasters' cards.
3. **A baked item** (`MS-S2.M1.L3`), the learner's choice: a shortcrust tart, a creamed or whisked cake, or a lean loaf.
   The evidence:
   - the weights;
   - a cross-section;
   - the core temperature, for the loaf.
4. **Knife and safety re-test** (`MS-S2.M1.L4`):
   - timed cuts (the `MS-S1` list plus *paysanne* and a chiffonade, 30 minutes);
   - a 3-minute hygiene video while handling raw chicken.
5. **Science and safety quiz** (`MS-S2.M1.L5`, 20 items).
6. **Stage reflection** (`MS-S2.M1.L6`).

| ID | Criterion | Competent means | Components | Competencies | Evidence | Weight |
|---|---|---|---|---|---|---|
| MS-S2.R1 | Planning | A realistic timeline with parallel tasks and control points, a complete prep list and shopping list; the framework read critically, with gaps filled | MS-S2.M1.L1 | KM-PLAN, TS-MEP, CK-RCPT | written, log | 10 |
| MS-S2.R2 | Technical execution | The protein at the stated doneness: the core within 3 °C of the target and an even cross-section; the cut prepared for its method | MS-S2.M1.L2 | TS-PRO, JD-CUES, IN-MEA, IN-FSH | measure, photo | 15 |
| MS-S2.R3 | Technical execution | The sauce at the right consistency (coats the spoon, *nappe*; or a stable emulsion), from a ratio | MS-S2.M1.L2 | TS-SAU, RD-RATO | photo, form | 10 |
| MS-S2.R4 | Texture | The vegetable and starch garnish evenly cut and cooked to the stated texture | MS-S2.M1.L2 | TS-VEG, TS-STA, IN-STA | photo, form | 10 |
| MS-S2.R5 | Flavour | Each element seasoned and the plate balanced; the tasters agree | MS-S2.M1.L2 | FL-SEAS | form, tasters | 10 |
| MS-S2.R6 | Timing | Both plates out within 5 minutes of the planned time, hot | MS-S2.M1.L1, MS-S2.M1.L2 | KM-PLAN | log | 5 |
| MS-S2.R7 | Food safety | Raw protein kept apart; poultry or minced meat to its safe core temperature (§9), probed and logged | MS-S2.M1.L2 | — | measure, checklist | gate |
| MS-S2.R8 | Technical execution | The baked item meets its standard: even bake, the expected structure (short, tender, open or even crumb), weights as planned; the loaf's core at least 93 °C | MS-S2.M1.L3 | TS-BAK, TS-BRD, IN-BAK, IN-DAI | photo, measure | 15 |
| MS-S2.R9 | Accuracy | Timed cuts within the `MS-S1` ranges, 8 of 10 pieces, within 30 minutes | MS-S2.M1.L4 | TS-KNF | measure, video | 10 |
| MS-S2.R10 | Food safety | The hygiene video shows no cross-contamination, and safe handling of knife and heat | MS-S2.M1.L4 | SA-HYG, SA-PHYS | video, checklist | gate |
| MS-S2.R11 | Knowledge | The science items at 80 % or more | MS-S2.M1.L5 | SC-PROT, SC-STRC, SC-EMUL, SC-MICR, SC-BRWN | quiz | 10 |
| MS-S2.R12 | Food safety | Every safety item correct | MS-S2.M1.L5 | SA-TEMP | quiz | gate |
| MS-S2.R13 | Reflection | As at `MS-S1`, plus how the weak competencies of `MS-S1` developed | MS-S2.M1.L6 | JD-REFL | written | 5 |

### 7.3 `MS-S3` — Integration (20 h over about a week)

**Purpose.** Show independence: plan and cost a week, preserve and store safely, cook a two-course evening from
frameworks, and correct flavour faults.

**Brief.**
1. **Week plan and costing** (`MS-S3.M1.L1`): a menu for 4–6 days for the household that uses:
   - a whole bird or a whole fish;
   - a preserve made in S3;
   - a braise;
   - a vegetable-led main.

   With quantities, cost per portion, a plan for leftovers, and the receipts afterwards.
2. **Prep day** (`MS-S3.M1.L2`): break down the bird or fish, make stock, prepare components in batches, cool and store
   them, with labels and a temperature log.
3. **Two-course evening** (`MS-S3.M1.L3`, from frameworks): a cold starter and a main.
   - One element is cooked over live fire, or in the oven grill where there is no outdoor space (Phase 3, review
     answer 2).
   - Evidence: timestamps against the plan, photos, and tasters' cards.
4. **Flavour-correction test** (`MS-S3.M1.L4`): §5.3.
5. **Bread or pastry** (`MS-S3.M1.L5`): whichever was not chosen at `MS-S2` (a sourdough loaf, or a pastry item).
6. **Safety quiz** (`MS-S3.M1.L6`, 15 items): high-risk processes and allergens.
7. **Written review** (`MS-S3.M1.L7`).

| ID | Criterion | Competent means | Components | Competencies | Evidence | Weight |
|---|---|---|---|---|---|---|
| MS-S3.R1 | Planning | The week uses every required element, leftovers are planned in, and the menu is seasonal and varied; when one item is unavailable, a substitute is chosen and reasoned | MS-S3.M1.L1 | KM-PLAN, CR-MENU, KM-WAST, CR-IMPV | written | 10 |
| MS-S3.R2 | Accuracy | Quantities and cost within 10 % of the receipts and actual yields; seasonal produce chosen and judged on quality | MS-S3.M1.L1 | KM-PURC, KM-MATH, IN-SEL | measure, written | 10 |
| MS-S3.R3 | Technical execution | The bird or fish broken down cleanly, with the yield recorded; batch components prepared and labelled to plan | MS-S3.M1.L2 | TS-FAB, TS-MEP | photo, measure | 10 |
| MS-S3.R4 | Food safety | Stock and batches cooled within the limits (§9), labelled and stored at 4 °C or below, with a temperature log | MS-S3.M1.L2 | SA-TEMP | log, checklist | gate |
| MS-S3.R5 | Technical execution | The braise tender and glossy; the vegetable, the cold starter and the live-fire element each to standard | MS-S3.M1.L3 | TS-CMB, TS-VEG, TS-GRD, TS-FIRE | photo, measure, form | 15 |
| MS-S3.R6 | Flavour | Each course built on a clear aromatic base and flavour logic, with a composition of main element, sauce, garnish and contrast; the tasters agree | MS-S3.M1.L3 | FL-ARCH, CR-COMP | form, tasters | 10 |
| MS-S3.R7 | Timing | Both courses out within 10 minutes of the planned times | MS-S3.M1.L1, MS-S3.M1.L3 | KM-PLAN | log | 10 |
| MS-S3.R8 | Independent decisions | Fault diagnosis passed (§5.3): each fault named and corrected, with the reason written down | MS-S3.M1.L4 | FL-CORR, FL-SENS, JD-TRBL, IN-ARO | calibration, form, written | 15 |
| MS-S3.R9 | Technical execution | The loaf or the pastry meets its standard: rise, crumb or lamination, and bake, in the cross-section; weights and fermentation log | MS-S3.M1.L5 | TS-BRD, TS-BAK | photo, measure, log | 10 |
| MS-S3.R10 | Food safety | Every safety item correct: high-risk processes, the 14 allergen groups, and cross-contact | MS-S3.M1.L6 | SA-HRPR, SA-ALLG | quiz | gate |
| MS-S3.R11 | Reflection | A review of the week: decisions, faults and their causes, and the plan for S4 | MS-S3.M1.L7 | JD-REFL | written | 10 |

### 7.4 `MS-S4` — Advanced (20 h over about two weeks)

**Purpose.** Show that the learner can host: a three-course dinner for guests within a theme, planned, costed, cooked,
plated and reviewed.

**Brief.**
1. **Menu and rationale** (`MS-S4.M1.L1`):
   - a theme: a tradition from `CUI-4`, or a season;
   - three courses for 4–6 guests, one of whom has a dietary need or an allergen;
   - drink pairings, alcoholic and non-alcoholic;
   - a written rationale.
2. **Plan** (`MS-S4.M1.L2`):
   - quantities and a costing;
   - a timeline from T−7 days;
   - the recipes written so that someone else could cook them.
3. **Prep days** (`MS-S4.M1.L3`): make-ahead components, with a temperature log.
4. **Service** (`MS-S4.M1.L4`):
   - three courses, every plate photographed;
   - at least one modern element where it adds value;
   - timestamps.
5. **Guest evaluation** (`MS-S4.M1.L5`): a structured card per course from each guest.
6. **Post-mortem** (`MS-S4.M1.L6`): against the plan and this rubric.
7. **Knife and knowledge re-test** (`MS-S4.M1.L7`):
   - precision cuts: *tourné*, a fine brunoise, *chiffonade*;
   - a classical-repertoire quiz (15 items).

| ID | Criterion | Competent means | Components | Competencies | Evidence | Weight |
|---|---|---|---|---|---|---|
| MS-S4.R1 | Planning | The menu fits the theme and the season, progresses and balances across courses, and adapts for the dietary need without a lesser dish; the pairings are reasoned | MS-S4.M1.L1 | CR-MENU, CK-BEVR, CK-CUIS, RD-ADPT | written | 15 |
| MS-S4.R2 | Planning | Quantities, costing and a T−7 timeline that hold; the recipes are reproducible | MS-S4.M1.L2 | KM-PLAN, KM-PURC, KM-MATH, RD-WRIT | written, measure | 5 |
| MS-S4.R3 | Organisation | Make-ahead work done as planned; the equipment chosen and used well; the station calm at service | MS-S4.M1.L3 | TS-MEP, KM-EQPT | log, photo, checklist | 10 |
| MS-S4.R4 | Food safety | Make-ahead food cooled, stored and reheated within the limits (§9); the allergen kept apart from shopping to plate | MS-S4.M1.L3 | SA-TEMP | log, checklist | gate |
| MS-S4.R5 | Technical execution | Proteins at exact doneness (probe and cross-section); sauces at the right consistency and shine; the modern element works and adds something | MS-S4.M1.L4 | TS-PRO, TS-SAU, TS-MOD | measure, photo, form | 15 |
| MS-S4.R6 | Presentation | Every course composed with intent: focal point, colour, height, clean rims, the right temperature | MS-S4.M1.L4 | TS-PLT, CR-COMP | photo | 10 |
| MS-S4.R7 | Consistency | The plates of each course match the first plate in portion, placement and doneness | MS-S4.M1.L4 | TS-PLT, TS-PRO | photo | 5 |
| MS-S4.R8 | Timing | Every course out within 5 minutes of the plan | MS-S4.M1.L2, MS-S4.M1.L4 | KM-PLAN | log | 10 |
| MS-S4.R9 | Flavour | Seasoning and balance right in every course, by the guests' cards and the learner's calibrated form | MS-S4.M1.L5 | FL-SEAS | tasters, form | 10 |
| MS-S4.R10 | Texture | Every course has the intended textures and at least one contrast | MS-S4.M1.L5 | FL-TEXT | tasters, form, photo | 5 |
| MS-S4.R11 | Reflection | The post-mortem compares the plan with the log, scores itself on this rubric with evidence, and names what to change | MS-S4.M1.L6 | JD-REFL | written | 5 |
| MS-S4.R12 | Accuracy | *Tourné* with seven even faces, brunoise 1–2 mm, a fine *chiffonade*; the classical quiz at 80 % or more | MS-S4.M1.L7 | TS-KNF, CK-CLAS | measure, video, quiz | 10 |

---

## 8. The capstone (`CAP-5`)

**Purpose.** Show, without a recipe, everything the core program builds. The learner plans, cooks and defends a
four-course dinner for 6–8 guests, from a seasonal brief that is unknown until the planning window opens (Phase 1 §10.5).

### 8.1 The brief

The brief is drawn at the start of the **planning window, 21 days before the dinner**. Three elements are drawn at
random (by the app later; by lot until then):

| Element | Drawn from |
|---|---|
| Season | The current season; the featured ingredient must be in season in the Netherlands (Phase 1 A4). |
| Budget | A range per guest, set for the household's region and checked against the costing. |
| Dietary requirement | One guest with one of: a listed EU allergen group [S23] (for example gluten, milk, egg, nuts or fish), vegetarian, or pescatarian. |
| Featured ingredient | A seasonal vegetable, fish, cut of meat or fruit that must lead one course. |

**The menu must include** (Phase 1 §10.5; checked before the plan is accepted):
- a sauce based on a stock or an emulsion;
- a protein cooked to an exact doneness;
- a bread or pastry element made from scratch;
- a preserved or fermented element prepared at least 7 days ahead;
- a vegetable-led course.

A menu that misses one is returned before any cooking. Dishes copied from course exercises don't count towards these
requirements; all recipes are the learner's own.

### 8.2 Timeline and deliverables

| When | What | Component |
|---|---|---|
| T−21 to T−14 | Brief analysis; menu and written rationale with drink pairings; shopping, quantities and costing | `CAP-5.M1.L1`–`L3` |
| T−14 to T−7 | Prep schedule with control points; test cooks of every untried dish, with notes | `CAP-5.M1.L4`, `L5` |
| T−7 | The preserved or fermented element | `CAP-5.M2.L1` |
| T−2 to T−1 | The bread or pastry element; prep days 1 and 2 | `CAP-5.M2.L2`–`L4` |
| T | Service of four courses, every plate photographed, a timestamped log and temperature records | `CAP-5.M2.L5`–`L7` |
| T+1 to T+7 | Guest cards; self-evaluation on this rubric; a written or recorded defence (about 15 minutes, or 1,500 words) | `CAP-5.M3.L1`–`L3` |

### 8.3 Rubric

| ID | Criterion | Competent means | Components | Competencies | Evidence | Weight |
|---|---|---|---|---|---|---|
| CAP-5.R1 | Independent decisions | Every constraint of the brief met; the menu's progression, balance and technique choices reasoned from principles; pairings fit; the dishes are recognisably the learner's own; the dietary requirement met by an adaptation that loses nothing; the defence answers "why" for each decision, including each choice of method | CAP-5.M1.L1, CAP-5.M1.L2, CAP-5.M3.L3 | JD-CHSE, CR-MENU, CR-ORIG, CK-BEVR, CK-METH, RD-ADPT | written | 15 |
| CAP-5.R2 | Planning | Quantities right for the guests with little waste; costing within the budget and within 10 % of the receipts; a T−7 schedule with control points | CAP-5.M1.L3, CAP-5.M1.L4 | KM-PURC, KM-MATH, KM-PLAN | written, measure | 10 |
| CAP-5.R3 | Food safety | Control points in the plan; the preserve made by a tested safe method; cooling, storage and reheating within the limits (§9); the allergen kept apart from shopping to plate; safe knife, hot-oil and equipment work through the prep days, with no injury | CAP-5.M1.L4, CAP-5.M2.L1, CAP-5.M2.L4 | SA-HYG, SA-HRPR, SA-TEMP, SA-PHYS | log, checklist, measure | gate |
| CAP-5.R4 | Organisation | Every untried dish test-cooked and adjusted, with notes; the prep days run to schedule, with the station organised | CAP-5.M1.L5, CAP-5.M2.L3 | RD-TEST, CR-ORIG, TS-MEP | log, written, photo | 5 |
| CAP-5.R5 | Technical execution | The foundations to standard: the preserve, the bread or pastry, the stock and sauce, the fabrication and knife work | CAP-5.M2.L1, CAP-5.M2.L2, CAP-5.M2.L3, CAP-5.M2.L4 | TS-PRS, TS-BAK, TS-BRD, TS-STK, TS-SAU, TS-FAB, TS-KNF | photo, measure | 15 |
| CAP-5.R6 | Technical execution | At service: the vegetable-led course showing the vegetables at their best; the protein browned and at the exact doneness (probe and cross-section), judged by its cues; the sauce at the right consistency and temperature | CAP-5.M2.L5, CAP-5.M2.L6 | TS-VEG, TS-PRO, TS-SAU, IN-VEG, TS-DRY, JD-CUES | measure, photo | 15 |
| CAP-5.R7 | Flavour | Every course seasoned and balanced, with depth, and corrected at the pass where needed; the guests' cards and the calibrated form agree | CAP-5.M2.L5, CAP-5.M3.L1, CAP-5.M3.L2 | FL-SEAS, FL-CORR, FL-SENS | tasters, form | 10 |
| CAP-5.R8 | Texture | The intended textures in every course, with contrast across the menu | CAP-5.M3.L1 | FL-TEXT | tasters, form, photo | 5 |
| CAP-5.R9 | Presentation | Every course composed with intent and served at the right temperature | CAP-5.M2.L5 | TS-PLT | photo | 5 |
| CAP-5.R10 | Consistency | All 6–8 plates of each course match in portion, placement and doneness | CAP-5.M2.L5, CAP-5.M2.L6 | TS-PLT, TS-PRO | photo | 5 |
| CAP-5.R11 | Timing | Every course out within 5 minutes of the plan, with no course waiting for another | CAP-5.M2.L6 | KM-PLAN | log | 5 |
| CAP-5.R12 | Reflection | Complete evidence; a self-evaluation on every row with evidence that the reviews confirm; every recovery from a problem logged with its cause; a defence that names what went wrong, why, and what the learner would change | CAP-5.M2.L7, CAP-5.M3.L2, CAP-5.M3.L3 | JD-REFL, JD-TRBL | written, log | 10 |

### 8.4 Band descriptors

The capstone rows have full descriptors, because the result closes the program. *Developing* and *Excellent* are written
relative to *Competent*, which is in the rubric above.

| Row | Not yet (0) | Developing (1) | Excellent (3) |
|---|---|---|---|
| `CAP-5.R1` | A constraint missed, or decisions explained only as "I like it" | All constraints met; some decisions reasoned, others only described | Every decision reasoned, with alternatives weighed; a clear personal point of view that still serves the guests |
| `CAP-5.R2` | Over budget by more than 10 %, or quantities clearly wrong (shortage or much waste) | Within budget; the schedule has gaps that caused a scramble | Costing within 5 %; waste planned into other dishes; the schedule absorbed a surprise |
| `CAP-5.R4` | No test cooks, or the prep days out of control | Test cooks done but the notes are thin; one prep task late | Test cooks with controlled variables and changes recorded; prep ahead of schedule |
| `CAP-5.R5` | A foundation failed (split sauce served, unrisen bread, an unsafe preserve) | Every foundation present, one clearly below standard | Every foundation at a level that would pass in a professional kitchen |
| `CAP-5.R6` | The protein outside the doneness range, or a broken sauce at service | One element off (a few degrees over, a sauce slightly thick) | Exact doneness on every plate; every element at its best moment |
| `CAP-5.R7` | A course the guests call under- or over-seasoned, or one-dimensional | Balanced, but one course flat or heavy | Every course balanced and layered; the guests single out the flavour |
| `CAP-5.R8` | Textures wrong (soggy, tough, grainy) in a course | Correct but without contrast in one course | Deliberate contrast in every course, and across the menu |
| `CAP-5.R9` | Messy, or served at the wrong temperature | Clean but without a clear focal point in one course | Every plate composed and photographed as intended |
| `CAP-5.R10` | Plates visibly different in portion or doneness | Small differences in one course | Plates indistinguishable |
| `CAP-5.R11` | A course more than 15 minutes late, or courses colliding | One course 5–15 minutes late | Every course on time, with time to plate calmly |
| `CAP-5.R12` | Evidence missing, or a self-evaluation the reviews contradict | Complete, but the defence describes rather than explains | Candid, precise, with a concrete plan for specialization or further practice |

`CAP-5.R3` is a gate: **Pass** when every control point is logged and within the limits, and **Fail** otherwise.

### 8.5 Completion of the core program

The core program is complete when:
- `CAP-5` is passed (§3.3);
- every competency is at its **core target** in the evidence record, with no allowance and none overdue for review
  (framework §3.1).

A capstone with distinction is recorded as such. A failed capstone is retaken with a new brief (§3.3).

---

## 9. Safety figures used in assessment

Gates and safety quiz items check against these figures. They follow two kinds of source:
- for home practice, the Dutch Voedingscentrum [S21][S22];
- where the Voedingscentrum gives no number, the U.S. FDA *Food Code* 2022 [S25]. It is written for professional
  kitchens and is used here only as the measurable standard, because a gate needs a number. The Phase 4 review accepted
  it as the reference.

The figures were checked on the pages and in the sections named on 2026-11-04.

| Topic | Figure | Source |
|---|---|---|
| Fridge | Set to 4 °C | Voedingscentrum, *Veilig bewaren* [S22] |
| Freezer | At least −18 °C | [S22] |
| Leftovers out of the fridge | Not longer than 2 hours | [S22] |
| Leftovers in the fridge | At most 2 days, covered; freeze them to keep longer | [S22] |
| Cooling leftovers at home | Let them cool before the fridge, faster in a pan standing in cold water, within the 2 hours above | [S22] |
| Reheating leftovers at home | Until steaming hot throughout (*door en door … stomend heet*); not reheated a second time | Voedingscentrum, *Verhitten* [S21] |
| Cooling cooked food (measurable, for stocks, braises and batches) | From 57 °C to 21 °C within 2 hours, and to 5 °C or below within 6 hours in total | FDA Food Code 2022 §3-501.14 [S25] |
| Cooling methods | Shallow pans, smaller portions, an ice-water bath with stirring, ice as an ingredient | §3-501.15 [S25] |
| Hot and cold holding | 57 °C or above; 5 °C or below | §3-501.16 [S25] |
| Reheating for hot holding | Every part at least 74 °C for 15 seconds, reached within 2 hours | §3-403.11 [S25] |
| Cooking poultry, and stuffed foods | 74 °C, instantaneous | §3-401.11(A)(3) [S25] |
| Cooking minced meat and minced fish | 68 °C for 17 seconds, or 63 °C for 3 minutes, 66 °C for 1 minute, 70 °C instantaneous | §3-401.11(A)(2) [S25] |
| Cooking fish and intact meat | 63 °C for 15 seconds | §3-401.11(A)(1) [S25] |
| Whole-muscle, intact beef steaks served rare | Cooked on top and bottom to a surface temperature of 63 °C or above, with a cooked colour change on all outside surfaces; the steak kept intact (not pierced, needle-tenderised or rolled); not served to risk groups | §3-401.11(C) [S25] |
| Whole meat roasts | A time-temperature table, from 54.4 °C held for 112 minutes to 62.8 °C held for 4 minutes (the holding time may include the rise after the oven) | §3-401.11(B) [S25] |
| Garlic or herbs in oil | Needs time and temperature control (refrigeration) unless modified to stop pathogen growth | Food Code definition of TCS food, §1-201.10 [S25] |
| Home preserving | Use tested methods and recipes only | National Center for Home Food Preservation [S27] |

**Rules for the program's assessments:**
- A probe reading below the figure for the food is a gate **Fail**, even if the dish looks done.
- A whole-muscle beef steak served below 63 °C at the core is checked against the steak row above (seared on both
  faces, colour changed on every outside surface, kept intact), not against a core reading. Minced, tenderised or
  rolled beef is not a steak under this rule.
- Sous vide and low-temperature cooking follow the time-temperature tables above. A cook below them needs a tested
  pasteurisation table, which Phase 5 cites in `SLW-3`.
- The Dutch home advice wins where it is stricter, for example 4 °C rather than 5 °C in the fridge.

---

## 10. Criteria and evidence across the milestones (generated)

The weight of each criterion per assessment ("gate" for food safety; "—" when it is not scored):

<!-- BEGIN generated:criteria -->
| Criterion | `MS-S1` | `MS-S2` | `MS-S3` | `MS-S4` | `CAP-5` |
|---|---|---|---|---|---|
| technical execution | 20 | 40 | 35 | 15 | 30 |
| accuracy | 30 | 10 | 10 | 10 | — |
| timing | — | 5 | 10 | 10 | 5 |
| consistency | — | — | — | 5 | 5 |
| texture | — | 10 | — | 5 | 5 |
| flavour | 10 | 10 | 10 | 10 | 10 |
| presentation | — | — | — | 10 | 5 |
| organisation | 10 | — | — | 10 | 5 |
| food safety | gate | gate | gate | gate | gate |
| independent decisions | — | — | 15 | — | 15 |
| knowledge | 15 | 10 | — | — | — |
| planning | — | 10 | 10 | 20 | 10 |
| reflection | 15 | 5 | 10 | 5 | 10 |
| **rows** | **10** | **13** | **11** | **12** | **12** |
<!-- END generated:criteria -->

How much of each rubric's weight rests on objective evidence, on partly verifiable evidence at best, or on self-assessed
evidence only. A row counts under the strongest category among its evidence methods:

<!-- BEGIN generated:evidence-mix -->
| Assessment | Rows | Gates | Weight on rows with objective evidence | … partly verifiable at best | … self-assessed only |
|---|---|---|---|---|---|
| `MS-S1` | 10 | 3 | 55 | 35 | 10 |
| `MS-S2` | 13 | 3 | 65 | 25 | 10 |
| `MS-S3` | 11 | 2 | 70 | 20 | 10 |
| `MS-S4` | 12 | 1 | 50 | 40 | 10 |
| `CAP-5` | 12 | 1 | 60 | 30 | 10 |
<!-- END generated:evidence-mix -->
