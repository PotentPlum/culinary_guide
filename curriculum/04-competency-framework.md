# Phase 4 — Competency framework

Status: **ready for review** (2026-11-04); the open questions are in §6.2. This phase sets out the complete competency framework:
- what each competency asks the learner to know and do at every level;
- how each competency is assessed, and what counts as a successful demonstration;
- where it is taught, practised and re-tested;
- when a learner may move on to the next stage.

The assessment design (methods, rubrics, the milestones and the capstone) is in [`04-assessment.md`](04-assessment.md).
This phase builds on [Phase 1](01-vision-architecture.md) (the competency list, scale and targets),
[Phase 2](02-curriculum-outline.md) (the outline) and [Phase 3](03-validation.md) (the realistic hours and the practice
rule). It does not repeat them.

| File | Content |
|---|---|
| this file | How the framework works, the evidence model, the stage progression criteria, and the traceability summary |
| [`04-competencies/`](04-competencies/) | One file per domain, with an entry for each of the 67 competencies |
| [`04-assessment.md`](04-assessment.md) | The assessment methodology, calibration tests, module checks, and the milestone and capstone briefs and rubrics |

**Review decisions (2026-11-04).**
- **No new competency IDs.** A broad competency lists its *elements* (for example, the six dry-heat methods of `TS-DRY`),
  and its success criteria say how many elements each level needs. The 67 IDs of Phase 1 stay the unit of tracking, so no
  lesson needs re-tagging.
- **A stage gate with a small allowance** (§3). Up to two non-safety competencies may be one level short when the
  learner moves on, if the gap is closed before the next milestone.

The blocks marked *generated* are produced by `tools/validate_curriculum.py --write` from the outline and Phase 1, so the
mapping cannot drift from the curriculum. CI checks that they are current.

---

## 1. How the framework works

### 1.1 Competencies, elements and levels

A **competency** is a durable ability the program wants the learner to have, such as `TS-SAU` *Sauces*. It is defined
independently of the courses: courses teach competencies, but competencies describe the graduate (Phase 1 §5).

The **elements** of a competency are the distinct things it covers. `TS-SAU`, for example, covers pan sauces,
starch-thickened sauces, emulsified sauces, reductions and vinaigrettes. Elements make a broad competency assessable
without splitting it: a level names how many elements, or which ones, it needs.

**Levels** use the 1–5 scale of Phase 1 §5.2, interpreted per dimension (K, S, J, M). In short:

| Level | What it means for every competency |
|---|---|
| 1 Awareness | Names and explains it. Shown by passing the module quiz items on it; no entry row is needed. |
| 2 Guided | Does it with a recipe, framework or checklist, with an acceptable result. |
| 3 Independent | Does it without instructions, with an acceptable result, and makes the routine decisions. |
| 4 Adaptation | Gets good results with new ingredients, equipment, quantities or constraints, and explains why. |
| 5 Mastery | Consistent, efficient and refined; diagnoses faults from little evidence; could teach it. |

Each entry turns this generic scale into **success criteria** for that competency: what a successful demonstration at
level 2, 3, 4 or 5 looks like, with tolerances where something can be measured. Levels above the core target are left
to specializations and are not defined.

### 1.2 The entry format

Every competency has one entry in [`04-competencies/<domain>.md`](04-competencies/). The validator checks that every
field is present and that there is a success row for each level from 2 up to the target.

| Field | Content |
|---|---|
| Heading | `### TS-SAU — Sauces`: the ID and a short name. |
| **Elements** | What the competency covers. Levels refer to these. |
| **Knowledge** | What the learner must understand: concepts, science, rules and vocabulary, with the level at which each is needed. |
| **Practical ability and judgment** | What the learner must be able to do [S] and decide [J], by level. |
| **Independence** | How much support is acceptable at each stage target: recipe, framework, brief, or nothing at all. |
| **Assessment** | The methods used, and which programme assessments re-test it (module checks, milestones, the capstone). |
| **Success criteria** | A table with one row per level from 2 to the target: what a successful demonstration looks like, and the evidence it needs (§1.3). |
| **Not yet** | Typical signs that a demonstration does *not* meet the level. These help self-assessment and AI review. |
| *Generated mapping* | The Phase 1 description and targets; then, per stage, the lessons that move it and the level each reaches, the practical sessions, the exercises in those modules, and the milestone or capstone components that re-test it. |

The **mapping counts an exercise for every competency moved by a lesson in its module**. Exercises carry no competency
tags of their own in the outline. This is an approximation, and Phase 5 may tag exercises directly.

### 1.3 Evidence methods

Every success row names its evidence from a fixed list of methods. Each method falls into one of the three categories of
Phase 1 §10.2: **O** objective, **P** partly verifiable, **S** self-assessed.

| Method | What it is | Category |
|---|---|---|
| `quiz` | Auto-graded knowledge check: recall, scenarios, calculations, image identification | O |
| `measure` | A reading that can be checked: weight, temperature, dimension against a grid, yield, cost | O |
| `calibration` | A blind calibration test with a known answer (salt or acid series, triangle test) ([assessment §5](04-assessment.md#5-calibration-tests)) | O |
| `log` | A timestamped record: a timeline against a plan, a temperature log, a fermentation log | O |
| `photo` | A photo against a reference (grid, ruler, cross-section, side-by-side), reviewed by AI against the exercise rubric | P |
| `video` | A short clip of technique or posture, reviewed by AI | P |
| `written` | A written explanation, plan, recipe, or reflection against prompts, reviewed against a rubric | P |
| `form` | A structured sensory form (taste, aroma, texture, balance) filled in by the learner | S |
| `checklist` | A self-assessment checklist during or right after cooking (hygiene, organisation, sequence) | S |
| `tasters` | Structured cards from household tasters or guests, blind where possible | S |

**Rule:** every level needs at least one **O** or **P** method. Self-assessment and tasters strengthen the evidence, but
they never carry a level alone (the validator checks this). For taste, the objective anchor is the learner's calibration
result (§2.4).

---

## 2. The evidence model

The curriculum teaches; the **evidence record** proves. This section defines how a practical session becomes evidence,
and how the mastery criterion of Phase 1 §10.3 is counted. Phase 7 turns it into a data model.

### 2.1 The evidence record

Each time the learner demonstrates a competency (in an exercise, a module check, a milestone or a free cook), the record
gets one entry:

| Field | Content |
|---|---|
| Date | The day of the demonstration. |
| Competency and level claimed | One competency per entry; a dish that shows several competencies creates several entries. |
| Activity | The exercise, check or milestone component (its ID), or *free cook*. |
| Conditions | What varied compared with earlier entries (Phase 1 §9.2): ingredient, quantity, equipment, or constraint (time pressure, guests, no recipe). |
| Support level | Recipe, framework, brief, or none. |
| Evidence | The methods used (§1.3), with the photos, readings, logs and forms attached. |
| Outcome | *Meets* or *not yet*, against the success row for the level claimed, with a one-line reason. |
| Review | AI review result where used, and the learner's agreement or disagreement (Phase 1 §10.1). |

### 2.2 When a demonstration counts

A demonstration **counts** towards a level when:
1. it meets every point of that level's success row;
2. it uses the support the level allows:
   - level 2: any support, including a full recipe;
   - level 3: no step-by-step recipe; a framework or brief at most;
   - level 4: a brief, or a framework applied to a new ingredient, quantity or piece of equipment;
   - level 5: at least one demonstration with no recipe at all;
3. its evidence includes the methods the success row names, with at least one O or P method;
4. no other evidence contradicts it. If a self-assessed claim is contradicted by a photo, a reading or the tasters, the
   entry is marked *not yet*.

### 2.3 Counting a level

The mastery criterion of Phase 1 §10.3, counted from the record:

| Level | Successful demonstrations | Conditions | Spread | Also needed |
|---|---|---|---|---|
| 2 | 1 | — | — | — |
| 3 | 2 | — | on different days | — |
| 4 | 3 | at least 2 different conditions | — | A written note reasoning the adaptation (`written`). |
| 5 | 5 | at least 3 different conditions | over at least 8 weeks | At least 1 without a recipe, and at least 1 under time pressure or for guests; a short written explanation of the competency's variables and faults. |

A demonstration at a higher level also counts for the lower levels. Where a competency has elements, the success row says
how many elements the demonstrations must cover together. For example, `TS-DRY` level 4 needs good results with at least
four of the six methods.

Knowledge-heavy competencies (most of CK, SC and parts of SA) are shown by quizzes and written explanations rather than by
cooking. For them, a "demonstration" is a passed quiz section or an accepted written answer, and the same counts apply.

### 2.4 Taste: making self-assessment usable

Taste, aroma and seasoning cannot be verified from a photo (Phase 1 §10.2). Three measures make the learner's own
judgment usable as evidence:
1. **Calibration.** A self-assessed sensory claim (`form`) counts only if the learner passed the matching calibration
   test ([assessment §5](04-assessment.md#5-calibration-tests)) in the last 6 months.
2. **Blind tasters.** At levels 4–5, flavour claims need household tasters as well, blind where the exercise allows.
3. **Consistency.** Many entries that agree with each other and with the tasters are stronger than one strong claim.

### 2.5 Ageing and review

Evidence ages. A competency not demonstrated for about **6 months** becomes **due for review** (spaced repetition;
Phase 1 §10.3):
- it keeps its level; nothing is taken away;
- the next review asks for one demonstration at the level held, under any condition;
- a competency still due at the next milestone must be reviewed before that milestone can be passed.

A review that fails is recorded as *not yet*. The competency then needs two fresh demonstrations at its level before it
counts again.

### 2.6 The practice rule and the evidence record

The Phase 3 **practice rule** (1 / 3 / 5 / 8 sessions for levels 2–5) is a *design* check on the curriculum: it proves
that the lessons give enough opportunities to reach each level. The evidence record is what counts for the *learner*.
A learner who needs more attempts takes them, using the extra practice suggested in the course (Phase 5). A
learner who meets the criteria sooner moves on sooner.

---

## 3. Stage progression criteria

### 3.1 The stage gate

A learner **completes a stage** and may start the next one when all of the following hold:

1. **Courses.** Every course of the stage is complete, which means every module check is passed (§3.2).
2. **Milestone.** The stage milestone (`MS-S1`–`MS-S4`) is passed by the rules of
   [assessment §3](04-assessment.md#3-scoring-and-pass-rules): every food-safety gate passed, no weighted row at
   *Not yet*, and a weighted score of at least 66.7 (Competent on average).
3. **Competencies.** Every competency with a target in this stage has reached it in the evidence record (§2.3).
   **Allowance:**
   - up to **two** non-safety competencies may be **one level short**;
   - each is recorded as *carried* and must be closed before the next milestone (or before `CAP-5`);
   - a competency cannot be carried twice in a row;
   - **safety competencies (`SA-*`) have no allowance.**
4. **Reviews.** No competency is overdue for review (§2.5).
5. **Reflection.** The stage reflection is written (it is part of every milestone).

`CAP-5` closes the core program (S5). Its rules are in [assessment §8](04-assessment.md#8-the-capstone-cap-5). After it,
every competency must be at its core target, with no allowance.

### 3.2 Within a stage

- **Course order** follows the course prerequisites of Phase 2. A course may start once its prerequisite courses are
  complete; courses without a dependency between them may run in parallel.
- **Module checks** gate the course, not the next module. A learner may move on to the next module before the check of
  the previous one is passed, but cannot complete the course until every check is passed. This stops one hard drill from
  blocking the theory that follows.
- **A course's "competencies on completion"** (Phase 2) are the levels the course is designed to reach. They are not a
  gate of their own: the stage gate checks them, so a learner who is slow on one competency is not held back course by
  course.
- **Repeats.** A failed module check is retaken after at least one further practice session on the failed items.

### 3.3 What changes at each gate (generated)

How many competencies have a target at each stage, and how many targets rise there:

<!-- BEGIN generated:progression -->
| Stage | Competencies with a target | Raised in this stage | Of which safety (`SA-*`) | At level 5 | Milestone |
|---|---|---|---|---|---|
| S1 | 46 | 46 | 4 | 0 | `MS-S1` (7 h) |
| S2 | 60 | 56 | 5 | 0 | `MS-S2` (16 h) |
| S3 | 67 | 52 | 4 | 2 | `MS-S3` (20 h) |
| S4 | 67 | 28 | 1 | 9 | `MS-S4` (20 h) |
| S5 | 67 | 14 | 0 | 20 | `CAP-5` (51 h) |
<!-- END generated:progression -->

### 3.4 The gates in words

| Gate | The learner can … | What the gate guards against |
|---|---|---|
| S1 → S2 | Work safely and cleanly; cut, measure and organise; cook vegetables, eggs and starches from a recipe; taste and season deliberately (calibrated). | Starting the classical techniques with unsafe habits, an uncalibrated palate, or no mise en place. |
| S2 → S3 | Make stocks, the sauce families, soups and doughs; cook meat, poultry and fish to a stated doneness; bake reliably; explain why with science. | Integration work (menus, preservation, world cuisines) built on techniques that only work with a recipe. |
| S3 → S4 | Cook a two-course evening without full recipes, plan a costed week, preserve safely, cook from several traditions, and correct flavour faults. | Advanced and classical refinement without the independence and safety that S3 builds. |
| S4 → S5 | Plan and cook a three-course dinner for guests to a theme, with menu, costing, timeline and post-mortem; develop and write recipes. | A capstone attempt without the planning, development and guest experience it needs. |
| End of S5 | Everything above, independently, consistently and under pressure: the core targets. | Calling the program complete on one good evening; the capstone and the evidence record both count. |

---

## 4. Traceability (generated)

One row per competency, linked to its entry:
- **Courses / Modules / Lessons:** the outline lessons that move the competency, and the modules and courses they sit in.
- **Exercises:** the exercises in those modules.
- **Practical sessions:** the sessions in those lessons.
- **Re-tests:** the milestone and capstone components that re-test it.

Each entry gives the per-stage detail.

<!-- BEGIN generated:traceability -->
| Competency | Target | Courses | Modules | Lessons | Exercises | Practical sessions | Re-tests |
|---|---|---|---|---|---|---|---|
| [`CK-TERM`](04-competencies/CK.md#ck-term--kitchen-terminology) | 4 | 5 | 6 | 7 | 8 | 6 | 1 |
| [`CK-METH`](04-competencies/CK.md#ck-meth--cooking-methods) | 5 | 5 | 8 | 11 | 9 | 9 | 1 |
| [`CK-RCPT`](04-competencies/CK.md#ck-rcpt--reading-recipes-critically) | 4 | 2 | 2 | 4 | 3 | 5 | 1 |
| [`CK-CLAS`](04-competencies/CK.md#ck-clas--classical-repertoire) | 4 | 3 | 7 | 9 | 10 | 12 | 1 |
| [`CK-CUIS`](04-competencies/CK.md#ck-cuis--world-culinary-traditions) | 4 | 4 | 14 | 32 | 28 | 40 | 1 |
| [`CK-BEVR`](04-competencies/CK.md#ck-bevr--beverages-and-pairing) | 3 | 2 | 2 | 5 | 3 | 5 | 2 |
| [`IN-SEL`](04-competencies/IN.md#in-sel--selection-quality-and-seasonal-buying) | 4 | 6 | 6 | 6 | 6 | 7 | 1 |
| [`IN-STO`](04-competencies/IN.md#in-sto--storage) | 4 | 8 | 9 | 10 | 12 | 9 | 1 |
| [`IN-VEG`](04-competencies/IN.md#in-veg--vegetables-and-fruit) | 5 | 7 | 10 | 12 | 11 | 15 | 1 |
| [`IN-STA`](04-competencies/IN.md#in-sta--grains-legumes-pasta-rice-and-potatoes) | 4 | 4 | 9 | 10 | 13 | 9 | 1 |
| [`IN-DAI`](04-competencies/IN.md#in-dai--eggs-and-dairy) | 4 | 5 | 6 | 8 | 7 | 7 | 1 |
| [`IN-MEA`](04-competencies/IN.md#in-mea--meat-and-poultry) | 4 | 5 | 8 | 10 | 9 | 9 | 1 |
| [`IN-FSH`](04-competencies/IN.md#in-fsh--fish-and-shellfish) | 4 | 4 | 6 | 7 | 9 | 9 | 1 |
| [`IN-ARO`](04-competencies/IN.md#in-aro--herbs-spices-aromatics-and-condiments) | 4 | 5 | 8 | 10 | 13 | 12 | 1 |
| [`IN-BAK`](04-competencies/IN.md#in-bak--baking-ingredients) | 4 | 5 | 7 | 7 | 7 | 8 | 1 |
| [`TS-MEP`](04-competencies/TS.md#ts-mep--mise-en-place) | 5 | 5 | 5 | 7 | 8 | 8 | 5 |
| [`TS-KNF`](04-competencies/TS.md#ts-knf--knife-skills) | 5 | 3 | 7 | 17 | 11 | 34 | 4 |
| [`TS-DRY`](04-competencies/TS.md#ts-dry--dry-heat-methods) | 5 | 9 | 14 | 19 | 21 | 30 | 1 |
| [`TS-MOI`](04-competencies/TS.md#ts-moi--moist-heat-methods) | 4 | 5 | 5 | 8 | 10 | 10 | 1 |
| [`TS-CMB`](04-competencies/TS.md#ts-cmb--combination-methods) | 4 | 5 | 6 | 9 | 8 | 10 | 1 |
| [`TS-FIRE`](04-competencies/TS.md#ts-fire--live-fire-and-barbecue) | 4 | 2 | 7 | 11 | 8 | 15 | 1 |
| [`TS-EGG`](04-competencies/TS.md#ts-egg--egg-cookery) | 4 | 5 | 7 | 12 | 10 | 19 | 1 |
| [`TS-VEG`](04-competencies/TS.md#ts-veg--vegetable-preparation-and-cookery) | 5 | 6 | 12 | 18 | 17 | 32 | 4 |
| [`TS-STA`](04-competencies/TS.md#ts-sta--starch-cookery) | 4 | 3 | 9 | 15 | 15 | 26 | 1 |
| [`TS-STK`](04-competencies/TS.md#ts-stk--stocks-and-soups) | 4 | 3 | 6 | 12 | 10 | 13 | 1 |
| [`TS-SAU`](04-competencies/TS.md#ts-sau--sauces) | 5 | 8 | 12 | 21 | 16 | 36 | 4 |
| [`TS-PRO`](04-competencies/TS.md#ts-pro--protein-doneness-control) | 5 | 8 | 16 | 20 | 21 | 37 | 3 |
| [`TS-FAB`](04-competencies/TS.md#ts-fab--fabrication) | 3 | 6 | 8 | 12 | 8 | 21 | 2 |
| [`TS-BAK`](04-competencies/TS.md#ts-bak--baking-and-pastry) | 4 | 3 | 13 | 25 | 17 | 43 | 3 |
| [`TS-BRD`](04-competencies/TS.md#ts-brd--bread) | 3 | 2 | 7 | 13 | 8 | 26 | 3 |
| [`TS-GRD`](04-competencies/TS.md#ts-grd--cold-kitchen-garde-manger) | 3 | 1 | 3 | 6 | 3 | 8 | 1 |
| [`TS-PRS`](04-competencies/TS.md#ts-prs--preservation) | 3 | 1 | 4 | 8 | 5 | 11 | 1 |
| [`TS-MOD`](04-competencies/TS.md#ts-mod--modern-techniques) | 2 | 2 | 5 | 5 | 6 | 9 | 1 |
| [`TS-PLT`](04-competencies/TS.md#ts-plt--plating-and-presentation) | 4 | 7 | 10 | 11 | 10 | 14 | 2 |
| [`SC-HEAT`](04-competencies/SC.md#sc-heat--heat-transfer-and-equipment) | 4 | 5 | 8 | 11 | 10 | 11 | 1 |
| [`SC-BRWN`](04-competencies/SC.md#sc-brwn--browning) | 4 | 8 | 8 | 9 | 11 | 11 | 1 |
| [`SC-PROT`](04-competencies/SC.md#sc-prot--proteins) | 4 | 8 | 9 | 12 | 10 | 9 | 1 |
| [`SC-STRC`](04-competencies/SC.md#sc-strc--starches-and-gluten) | 4 | 6 | 7 | 8 | 7 | 8 | 1 |
| [`SC-EMUL`](04-competencies/SC.md#sc-emul--fats-emulsions-foams-and-gels) | 4 | 7 | 9 | 11 | 11 | 18 | 1 |
| [`SC-MICR`](04-competencies/SC.md#sc-micr--microbiology-and-preservation-hurdles) | 3 | 5 | 6 | 7 | 7 | 4 | 1 |
| [`FL-SENS`](04-competencies/FL.md#fl-sens--sensory-evaluation) | 5 | 6 | 9 | 10 | 13 | 13 | 2 |
| [`FL-SEAS`](04-competencies/FL.md#fl-seas--seasoning-and-balance) | 5 | 7 | 9 | 16 | 14 | 20 | 6 |
| [`FL-TEXT`](04-competencies/FL.md#fl-text--texture-and-contrast) | 4 | 10 | 12 | 13 | 17 | 17 | 2 |
| [`FL-ARCH`](04-competencies/FL.md#fl-arch--flavour-composition) | 4 | 10 | 18 | 24 | 26 | 36 | 1 |
| [`FL-CORR`](04-competencies/FL.md#fl-corr--diagnosing-and-correcting-faults) | 5 | 5 | 5 | 5 | 6 | 10 | 1 |
| [`KM-PLAN`](04-competencies/KM.md#km-plan--planning-and-timing) | 5 | 11 | 15 | 17 | 18 | 20 | 5 |
| [`KM-MATH`](04-competencies/KM.md#km-math--culinary-math) | 4 | 5 | 6 | 8 | 6 | 8 | 3 |
| [`KM-PURC`](04-competencies/KM.md#km-purc--purchasing-and-budgeting) | 4 | 7 | 8 | 9 | 9 | 9 | 3 |
| [`KM-WAST`](04-competencies/KM.md#km-wast--using-ingredients-fully) | 4 | 9 | 9 | 10 | 11 | 12 | 1 |
| [`KM-EQPT`](04-competencies/KM.md#km-eqpt--equipment) | 4 | 9 | 12 | 18 | 15 | 19 | 1 |
| [`SA-HYG`](04-competencies/SA.md#sa-hyg--hygiene-and-cross-contamination) | 5 | 6 | 8 | 11 | 8 | 12 | 3 |
| [`SA-TEMP`](04-competencies/SA.md#sa-temp--temperature-control) | 5 | 10 | 11 | 13 | 14 | 11 | 5 |
| [`SA-PHYS`](04-competencies/SA.md#sa-phys--physical-safety) | 5 | 6 | 7 | 9 | 8 | 9 | 2 |
| [`SA-ALLG`](04-competencies/SA.md#sa-allg--allergens) | 4 | 5 | 5 | 6 | 5 | 7 | 2 |
| [`SA-HRPR`](04-competencies/SA.md#sa-hrpr--high-risk-processes) | 4 | 6 | 7 | 9 | 9 | 5 | 2 |
| [`RD-RATO`](04-competencies/RD.md#rd-rato--ratios-and-formulas) | 4 | 9 | 11 | 12 | 13 | 19 | 1 |
| [`RD-ADPT`](04-competencies/RD.md#rd-adpt--adaptation-and-substitution) | 5 | 7 | 10 | 14 | 11 | 23 | 1 |
| [`RD-TEST`](04-competencies/RD.md#rd-test--systematic-testing) | 4 | 9 | 9 | 11 | 10 | 17 | 1 |
| [`RD-WRIT`](04-competencies/RD.md#rd-writ--writing-recipes-and-technique-notes) | 4 | 4 | 6 | 6 | 6 | 10 | 1 |
| [`JD-CUES`](04-competencies/JD.md#jd-cues--reading-cues) | 5 | 13 | 15 | 16 | 21 | 24 | 1 |
| [`JD-CHSE`](04-competencies/JD.md#jd-chse--choosing-an-approach-from-first-principles) | 5 | 8 | 15 | 16 | 24 | 16 | 2 |
| [`JD-TRBL`](04-competencies/JD.md#jd-trbl--troubleshooting-and-recovery) | 5 | 8 | 8 | 9 | 11 | 14 | 1 |
| [`JD-REFL`](04-competencies/JD.md#jd-refl--self-assessment-and-reflection) | 5 | 12 | 13 | 17 | 14 | 26 | 7 |
| [`CR-COMP`](04-competencies/CR.md#cr-comp--composing-a-dish) | 4 | 9 | 12 | 14 | 16 | 18 | 2 |
| [`CR-MENU`](04-competencies/CR.md#cr-menu--designing-menus) | 4 | 2 | 3 | 4 | 3 | 5 | 3 |
| [`CR-IMPV`](04-competencies/CR.md#cr-impv--improvising-under-constraints) | 4 | 4 | 5 | 8 | 5 | 13 | 1 |
| [`CR-ORIG`](04-competencies/CR.md#cr-orig--original-dishes-and-a-personal-point-of-view) | 4 | 4 | 6 | 11 | 7 | 24 | 2 |
<!-- END generated:traceability -->

---

## 5. The competency entries

| Domain | File |
|---|---|
| CK Culinary knowledge | [`04-competencies/CK.md`](04-competencies/CK.md) |
| IN Ingredient knowledge | [`04-competencies/IN.md`](04-competencies/IN.md) |
| TS Technical skills | [`04-competencies/TS.md`](04-competencies/TS.md) |
| SC Food science | [`04-competencies/SC.md`](04-competencies/SC.md) |
| FL Flavour | [`04-competencies/FL.md`](04-competencies/FL.md) |
| KM Kitchen management | [`04-competencies/KM.md`](04-competencies/KM.md) |
| SA Food safety | [`04-competencies/SA.md`](04-competencies/SA.md) |
| RD Recipe development | [`04-competencies/RD.md`](04-competencies/RD.md) |
| JD Independent judgment | [`04-competencies/JD.md`](04-competencies/JD.md) |
| CR Creativity and adaptability | [`04-competencies/CR.md`](04-competencies/CR.md) |

---

## 6. End of phase

### 6.1 Decisions and assumptions

1. **No new competency IDs** (review decision). Broad competencies list their elements, and the success criteria say how
   many elements each level needs. The 67 Phase 1 IDs remain the unit of tracking.
2. **Stage gate with an allowance** (review decision, §3.1):
   - up to two non-safety competencies may be one level short, if the gap is closed before the next milestone;
   - safety competencies have no allowance;
   - module checks gate the course, not the next module;
   - a course's "competencies on completion" are checked at the stage gate, not per course.
3. **Evidence:**
   - every level needs at least one objective or partly verifiable method; self-assessment never carries a level alone
     (validator);
   - sensory self-assessment counts only with a calibration passed in the last 6 months;
   - when the learner and the outside evidence differ by two bands or more, the lower band stands (assessment §3.4).
4. **Rubrics:**
   - four bands; a pass needs every gate passed, no row at *Not yet*, and a score of at least 66.7;
   - the capstone gives a distinction from 85;
   - food safety is always a gate, in every milestone and in the capstone (validator);
   - at most 10 % of any rubric's weight rests on self-assessed evidence only (assessment §10).
5. **Safety figures:**
   - Dutch home advice (Voedingscentrum) wherever it gives a rule;
   - the FDA *Food Code* 2022 as the measurable standard where it doesn't;
   - the stricter of the two always applies (assessment §9).
6. **The mapping counts an exercise for every competency of its module** (§1.2). This is an approximation until
   Phase 5 tags exercises.
7. **Levels above the core target are not defined.** They belong to specializations (Phase 1 §11).
8. **The CIA credits of Phase 3 §3.1 are confirmed.** The catalog page [S1] was re-read. Its culinary rows add up to
   the stated 50.5 credits once the two "complete one" groups of restaurant courses (6 credits each) are counted, and
   every per-course credit Phase 3 used matches. The Phase 3 caveat is resolved, and the comparison stands.
9. **No change to the outline.** Phase 4 changed no course, module, lesson, hour or `Moves` entry. It added the
   competency entries, the assessment design and validator checks, and updated `sources.md` (S21, S22, S24, S25).

### 6.2 Gaps and open questions for the review

1. **Re-test levels.** 29 of the 67 competencies are never re-tested at their **core target** in a milestone or the
   capstone. Their final level rests on the evidence record, which the stage gate and the core completion rule require
   anyway (§3.1; assessment §8.5).
   - For 20 of them the target is level 4. Course work and the record are an adequate basis for that.
   - For 9, the target is **level 5**: `CK-METH`, `IN-VEG`, `TS-DRY`, `FL-SENS`, `FL-CORR`, `SA-PHYS`, `RD-ADPT`,
     `JD-CUES` and `JD-TRBL`. Mastery should be seen in the capstone.

   **Proposal** (not applied, because it changes the outline). Add these re-tests to capstone components that already
   ask for the work. Hours stay the same, and each re-test is scored by an existing rubric row:

   | Component | Add | Rubric row |
   |---|---|---|
   | `CAP-5.M1.L1` Analysing the brief | `CK-METH→5` | `CAP-5.R1` |
   | `CAP-5.M1.L2` Menu and rationale (the dietary requirement) | `RD-ADPT→5` | `CAP-5.R1` |
   | `CAP-5.M2.L4` Prep day 2 | `SA-PHYS→5` | `CAP-5.R3` (gate) |
   | `CAP-5.M2.L5` Service: first and second course (the vegetable-led course) | `IN-VEG→5`, `FL-CORR→5` | `CAP-5.R6`, `CAP-5.R7` |
   | `CAP-5.M2.L6` Service: main course and dessert | `TS-DRY→5`, `JD-CUES→5` | `CAP-5.R6` |
   | `CAP-5.M2.L7` Evidence (recoveries logged) | `JD-TRBL→5` | `CAP-5.R12` |
   | `CAP-5.M3.L2` Self-evaluation (calibrated form against the guests) | `FL-SENS→5` | `CAP-5.R7` |

   Should these be applied (recommended), extended to all 29, or left to the evidence record?
2. **No human examiner.** Scoring rests on the learner, AI review and household tasters. The cautious rule for
   disagreements (assessment §3.4) is the main safeguard. Is that acceptable, or would you like an optional outside
   review for the capstone, such as a cook or a chef among friends?
3. **Safety figures.**
   - The Voedingscentrum gives no core temperatures, so gates use the FDA *Food Code* figures (a U.S. professional
     standard).
   - The USDA FSIS chart cited in Phase 1 [S24] refused automated access when re-checked; its figures are not used.

   Is a U.S. professional standard acceptable as the measurable reference, or would you prefer a Dutch or EU source
   (for example an NVWA hygiene code [S26]) once one is found?

### 6.3 Recommendations for Phase 5

1. Write the **quiz item banks** to the blueprint (assessment §4.1), and the **reference images and short rubrics** for
   every exercise and rubric row that uses AI review (assessment §6).
2. **Tag each exercise** with the competencies it evidences, and the level, replacing the module approximation (§1.2).
   Let the validator check that every success row of every competency is reachable through at least one exercise.
3. Turn each module's *Assessment* line into an **evidence check**: which success rows, which evidence, which
   exercise.
4. Cite a **tested sous vide pasteurisation table** in `SLW-3`, and an NCHFP [S27] page for each preservation method in
   `PRS-3`. Phase 4 could verify neither on the pages it read.
5. Write the **calibration kit** into the equipment lists of `TST-1` and `FLV-3` (cups, codes, a key sheet, 1 g or
   0.1 g scale).
