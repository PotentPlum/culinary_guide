# Phase 3 — Curriculum validation and optimization

Status: **ready for review** (2026-11-04). This phase checks the Phase 2 outline critically: against the benchmarks,
against the nine checks the brief asks for, and against the time a home learner really needs. It then revises the
curriculum. Every change is logged in §6 and applied in [Phase 1](01-vision-architecture.md) (budgets, competency
table) and [Phase 2](02-curriculum-outline.md) (the outline files). Sources `[S#]` are in [`sources.md`](sources.md).

**The result in one table:**

| | Phase 2 | Phase 3 |
|---|---|---|
| Core hours (active) | 1,020 | **1,277.5** |
| Duration at 6.5 h/week, 46 weeks a year | about 3.4 years | **about 4.3 years** (3.5 years at 8 h/week) |
| Courses | 44 | **45** (`FIR-3` *Barbecue and live fire* added) |
| Competencies | 66 | **67** (`TS-FIRE` added) |
| Modules / lessons and components | 221 / 526 | **227 / 559** |
| Practical sessions | not counted | **722**, and every level claimed has enough of them behind it (§4.3) |
| Competencies re-tested at a milestone or the capstone | 42 of 66 | **67 of 67** |

---

## 1. Method

The review answers of 2026-11-04 set the frame (§2). The outline was then checked in four ways:

1. **Benchmarks** (§3): the CIA catalog, credit by credit [S1][S32]; the chapter areas of *The Professional Chef* [S8]
   and Gisslen [S10]; the work processes of the MBO *Keuken* qualification [S7]; the other schools of Phase 1 §3.1.
2. **Time** (§4): every one of the 526 lessons was re-estimated with an explicit time model, separating active hours from
   waiting time, and counting the practical sessions each lesson needs.
3. **The brief's nine checks** (§5): missing subjects, missing foundations, progression, dependencies, overlap,
   practical training, theory, assessment, and level of difficulty per stage.
4. **Automated checks** in `tools/validate_curriculum.py`, extended in this phase (R11). It now proves, on every change,
   that IDs, hours, prerequisites, coverage, practice and milestone re-tests are consistent. What it cannot judge
   (whether an hour estimate is realistic, whether a subject is missing) is the work of §3–§5.

What is judgment and what is checked:

| Claim | How it is supported |
|---|---|
| Every competency reaches its stage target | Checked by the validator (coverage). |
| Every level claimed has enough practice behind it | Checked by the validator (practice rule, §4.3). |
| Every competency is re-tested at a milestone or the capstone | Checked by the validator. |
| The hours are realistic | Judgment, using the time model of §4.2; the model is explicit so that it can be challenged. |
| No important subject is missing | Judgment, against the benchmarks in §3. |

---

## 2. Review answers applied

| # | Question (Phase 2, end of phase) | Answer (2026-11-04) | Applied |
|---|---|---|---|
| 1 | Thin benchmark subjects: cuisines of the Americas, smoking, cheesemaking, barbecue and live fire: core or specialization? | "Add barbecue, leave the rest as is." | New course `FIR-3` *Barbecue and live fire* (28.5 h, S3) and competency `TS-FIRE` (R2). The Americas, smoking as a preservation method and cheesemaking beyond a fresh cheese stay outside the core, as they were. |
| 2 | Separate active hours from elapsed time? | Agreed. | Lesson rows give active hours, practical sessions and elapsed time separately (R1). |
| 3 | Thinly taught competencies, and `TS-KNF` / `TS-VEG` at level 5 in one module: is the time enough? | It should be a realistic CIA-level program in time per topic. Don't shorten it to fit; keep it practical for someone learning at home. | Every lesson re-estimated (R3); a practice rule enforced for every competency (R4); the core grows from 1,020 to 1,277.5 h. |

**Where barbecue stops and smoking begins.** Barbecue cannot be taught without wood smoke, so `FIR-3.M3.L2` teaches smoke
*as a flavour* during cooking. Smoking as a *preservation* method (cold smoking, smoked cures, a smokehouse) is the
part that stays outside the core, in line with answer 1.

---

## 3. Benchmark comparison

### 3.1 The CIA, credit by credit

Phase 1 §3.3 estimated the CIA's on-campus hours at 1,500–2,000 and left the figure to Phase 3. The catalog gives no
contact hours per course, but it does give the rule: the CIA uses the U.S. federal credit-hour definition, **at least
45 hours of student work per credit** (one hour of instruction plus two of out-of-class work a week for about 15 weeks,
or the equivalent in lab and practice work) [S32]. The 64-credit Culinary Arts AOS [S1] is therefore at least about
2,900 h of student work, plus the 550-hour externship.

The CIA courses whose content this program covers, with their minimum workload (credits × 45 h):

| CIA course [S1] | Credits | ≥ hours | This program | Hours | Verdict |
|---|---|---|---|---|---|
| CULS-100 Culinary Fundamentals | 6 | 270 | The S1 courses, plus `HEA-2`, `KNF-2`, `STK-2`, `SAU-2` | 285.5 | Comparable. |
| CULS-115 Meat + CULS-116 Seafood identification and fabrication | 3 | 135 | `MEA-2`, `POU-2`, `FSH-2`, `BUT-3`, `PRO-4` | 130 | Comparable, without carcass work (Phase 1 §3.2). |
| BAKE-241 Baking and Pastry Skill Development | 3 | 135 | `BAK-2`, `BRD-2`, `PST-3`, `BRD-3`, `PST-4` | 147.5 | Comparable. |
| CULP-225 Garde Manger | 3 | 135 | `GRD-3`, `PRS-3` | 49 | Lower. Charcuterie in depth is a specialization; terrines and cures are introduced only. |
| CULP-311 Mediterranean + CULP-321 Asia | 6 | 270 | `CUI-3`, `CUI-4` (and `CLS-4` for France) | 141 (176.5) | Lower: the CIA courses run a restaurant kitchen every day. Up from 64 h in Phase 2 (R5). |
| CULP-301 Cuisines of the Americas | 3 | 135 | Mexico in `CUI-3.M9`; barbecue in `FIR-3` | 40.5 | Left outside the core (§2, answer 1). |
| HOSP-355 Wine and Beverage Studies | 1.5 | 67.5 | `MNU-4.M4` | 8.5 | Reduced on purpose: the basics only (Phase 1 §13.2 Q6). |
| CUSC-100 Food Safety | 1.5 | 67.5 | `SAF-1`, `SAF-3`, and safety lessons in 10 other courses | 22.5 + thread | Reduced: no professional certification content (Phase 1 §3.2). |
| CUSC-110 Nutrition | 1.5 | 67.5 | `NUT-3` | 14 | Reduced to applied nutrition (Phase 1 §3.2). |
| APFS-110 Food Systems; APFS-150 Gastronomy; MTSC-100 Kitchen Math | 4.5 | 202.5 | Integrated: `VEG-1.M2`, `VEG-3.M1`, `KMG-3.M2`; `CUI-3`, `CLS-4`; `WRK-1.M3`, `BAK-2.M1`, `KMG-3.M3`, `RDV-4.M1` | — | Integrated, not stand-alone (Phase 1 §4.2). |
| MGMT-250 Menus and Managing Profitability | 3 | 135 | `KMG-3`, `MNU-4` | 53 | Adapted to the household. |
| **Comparable content** | **36** | **≥ 1,620** | | | |
| *without the Americas* | *33* | *≥ 1,485* | | | |

**Not in this program:** professional production and operations (CULA-260, CULP-115, CULP-135, the back- and
front-of-house restaurant courses, MGMT-100: 22.5 credits), college writing (3 credits) and the externship.

**Only in this program:** structured tasting and calibration (`TST-1`, `FLV-3`), a food-science lab course (`SCI-2`),
vegetable-led cooking (`VEG-3`), modern techniques (`MOD-4`), plating (`PLT-4`), recipe development (`RDV-4`, `RDV-5`),
cooking without recipes (`IND-5`), and four milestones and a capstone: together about 340 h.

**Conclusion.** Where the program teaches the same thing as the CIA, it now gives it about the same time
(fundamentals, meat and fish, baking and pastry), except where Phase 1 or the reviewer reduced the subject on purpose
(beverage, safety, nutrition, the Americas). The two clear gaps are world cuisines and garde manger: the CIA teaches
both as daily production for a dining room, which a home learner can't reproduce and doesn't need at that volume. The
hours are a floor, not a proof of equivalence: a learner without a teacher needs more repetition for the same level,
which is why the practice rule (§4.3) exists.

A caveat on the source: the catalog page's per-course rows add up to less than its stated culinary subtotal (50.5
credits), so the per-course credits above are read as listed and should be re-checked against the catalog PDF before
anyone relies on a single figure.

### 3.2 *The Professional Chef* [S8] and Gisslen [S10]

Both books are compared by their standard part and chapter areas (bibliographic, not page-checked):

| Area | Where it is taught | Verdict |
|---|---|---|
| The profession, kitchen safety, equipment, menus and recipes, yields | `SAF-1`, `EQP-1`, `WRK-1`, `KMG-3` | Covered. |
| Identification: meat, poultry, fish, produce, herbs, dairy and eggs, dry goods | `MEA-2.M2`, `POU-2.M1`, `FSH-2.M1`, `VEG-1`, `FLV-3.M1`–`M2`, `EGG-1.M1`, `LEG-2.M3`, `STA-1`, `LEG-2` | Covered. |
| Stocks, sauces, soups | `STK-2`, `SAU-2`, `CLS-4` | Covered. |
| Fabrication; grilling and broiling, roasting; sautéing, pan-frying, deep-frying; steaming and submersion; braising and stewing | `KNF-2.M2`, `BUT-3`, `HEA-1`, `HEA-2`, `SLW-3`, and now `FIR-3` | Covered. Grilling over fire was thin (`HEA-2.M3` only): **added** (R2). |
| Vegetables, potatoes, grains and legumes | `VEG-1`, `VEG-3`, `STA-1`, `LEG-2` | Covered. |
| **Pasta and dumplings** | Dried pasta `STA-1.M3`, fresh pasta `CUI-3.M3.L2` | Filled pasta, *gnocchi* and dumplings were missing: **added** (`CUI-3.M3.L3`, `CUI-3.M7.L3`; R5). |
| Breakfast and eggs | `EGG-1` | Covered. |
| Salads and dressings; **sandwiches**; **hors d'oeuvre and appetizers**; charcuterie | `GRD-3`, `SAU-2.M6` | Sandwiches, toasts and canapés were missing: **added** (`GRD-3.M5.L3`; R6). Charcuterie in depth: specialization. |
| Yeast breads; pastry doughs and batters; custards, creams, mousses | `BRD-2`, `BRD-3`, `BAK-2`, `PST-3` | Covered. |
| **Fillings, frostings and dessert sauces**; plated desserts | `PST-3.M4`, `PST-4` | Buttercream and finishing a cake were missing: **added** (`BAK-2.M4.L4`; R6). |
| Cheese | `LEG-2.M3`, `GRD-3.M5.L1` | Left as is (§2, answer 1). |

### 3.3 MBO *Kwalificatiedossier Keuken* [S7]

The Dutch qualification is organised by work processes. In paraphrase:

| Work process (paraphrased) | Where it is taught | Verdict |
|---|---|---|
| Receiving, checking and storing goods | `SAF-1.M3`, `VEG-1.M2.L2`, `KMG-3.M2`, `KMG-3.M4.L2` | Covered. |
| Preparing the mise en place | `WRK-1.M1`, `KNF-1`, `KMG-3.M1.L2` | Covered. |
| Preparing dishes and components | All `TS` courses | Covered. |
| Presenting and serving dishes | `PLT-4`, `MNU-4.M3.L3`, `MNU-4.M5` | Covered, for home service. |
| Working hygienically (HACCP) and cleaning | `SAF-1.M2`, `SAF-3.M2` | Covered, adapted to the home. |
| Ordering and stock control | `KMG-3.M2`, `KMG-3.M4` | Covered. |
| Level 4: developing dishes and menus | `RDV-4`, `RDV-5`, `MNU-4` | Covered. |
| Level 4: planning and supervising staff, running the kitchen as a business | — | Out of scope (Phase 1 A8). |

The qualification's ladder of *independence* is already the backbone of the proficiency scale (Phase 1 §5.2). No gap.

### 3.4 Other benchmarks

- **Le Cordon Bleu** [S3][S4] and **Leiths** [S5]: progressive levels, classical technique as the backbone, creativity
  at the top. The stage structure matches; no change.
- **Ballymaloe** [S6]: producers and seasons (`VEG-1.M2`, `VEG-3.M1`), preserving (`PRS-3`), bread (`BRD-2`, `BRD-3`)
  are core. Smoking and cheesemaking stay outside the core (§2).

---

## 4. Time realism

### 4.1 What was wrong in Phase 2

The Phase 2 hours were fitted into the Phase 1 budgets. Lesson by lesson, that showed:

- **Repetition was promised but not counted.** Exercises asked for weekly knife drills "for the rest of S1", a hygiene
  audit "repeated after two weeks", "one dish per group across two weeks", but the lesson hours held one session.
- **Levels were claimed after one session.** `TS-SAU→5` in a single 2.5-hour lesson; a whole cuisine (history, pantry,
  technique and a meal) in 4 h; game and offal in 4 h each; an original dish developed, tested and written in 3 h;
  6 h of "targeted practice" to close the learner's remaining mastery gaps. Phase 1 §10.3 asks for 5 successful
  demonstrations over 8 weeks for level 5; one lesson cannot hold that.
- **Waiting time was folded in or left out**, unevenly. A brown stock (7 h on the stove, 3 h of work) and a sourdough
  loaf (a day, 3 h of work) had hours that matched neither.
- **The capstone had no test cooks.** A learner was expected to serve 6–8 guests untried original dishes.

### 4.2 The time model

Every lesson now has three numbers:

| Field | Meaning |
|---|---|
| **h** (active hours) | Time the learner is busy: studying, shopping for the lesson, preparing, cooking, tasting, photographing, writing up. |
| **Practice** | The number of separate practical sessions: cooking, tasting, experiments or applied tasks. A repeated drill counts once per session. |
| **Elapsed** | Calendar time beyond the active hours, when it matters for planning (`7 h`, `1 d`, `4 wk`). |

Active hours were estimated per lesson from these building blocks:

| Block | Time |
|---|---|
| Theory: reading, video, quiz | 0.5–1.5 h per lesson, by depth (science and safety at the top) |
| Drill session (cuts, emulsions, shaping) | 0.5–1 h |
| A dish cooked as a household meal | 1–1.5 h, including mise en place and clearing up |
| Comparison or experiment | 1.5–2 h |
| Long production (stock, braise, bread, a dinner for guests) | 2.5–6 h active, the rest as elapsed time |
| Evaluation: photos, tasting form, journal | about 0.25 h per session |

The average lesson goes from 1.9 h to 2.2 h of active time; the longest lessons (a dinner for guests, an original dish
through three test cycles) are 5–6 h. 60 lessons and components need a day or more of elapsed time; the generated *Weekly load* table
in Phase 2 counts them per stage.

### 4.3 The practice rule

A level is only *taught* when enough practical sessions stand behind it. The validator sums the sessions of every
lesson that moves a competency and requires, for the highest level the lessons claim:

| Level claimed | Sessions needed | Why |
|---|---|---|
| 2 Guided | 1 | One guided attempt. |
| 3 Independent | 3 | Phase 1 §10.3: 2 demonstrations without instructions, plus 1 learning session. |
| 4 Adaptation | 5 | 3 demonstrations under 2 conditions, plus 2 learning sessions. |
| 5 Mastery | 8 | 5 demonstrations over 8 weeks, plus 3 learning sessions. |

Milestone components re-test competencies but don't count, just as they don't count for coverage. The rule is a
floor, not a target: `TS-KNF` has 34 sessions behind its level 5.

When the rule was first run on the re-estimated outline, it flagged 8 competencies (9 lessons fixed). They are the "thinly taught"
competencies of Phase 2, made exact:

| Competency | Shortfall | Fix (R4) |
|---|---|---|
| `IN-DAI` | Level 2 in S1 with 0 sessions | `EGG-1.M1.L1`: test and grade a box of eggs. |
| `SC-MICR` | Level 3 in S2 with 2 sessions | `SCI-2.M6.L2`: a hurdle plan for three of the learner's own foods. |
| `SA-ALLG` | Level 3 in S2 with 2 sessions | `LEG-2.M4.L1`: replace milk, egg or gluten for an allergic eater and check every label. |
| `CK-RCPT` | Level 4 in S3 with 4 sessions | `CUI-3.M1.L2`: read and source two recipes from other traditions, not one. |
| `SA-PHYS` | Level 5 in S3 with 7 sessions | `FIR-3.M1.L2`: handling hot grates, coals and ash. |
| `RD-WRIT` | Level 3 in S3 with 2 sessions; level 4 in S4 | `KMG-3.M5.L1`: a household member cooks a house recipe from the page; `RDV-4.M4.L2`: written iterations. |
| `CK-TERM` | Level 4 in S4 with 4 sessions | `CLS-4.M3.L2`: how the derivative names are built. |
| `CK-METH` | Level 5 in S5 with 7 sessions | `IND-5.M1.L2`: justify the method for each unfamiliar ingredient. |

**`TS-KNF` and `TS-VEG` at level 5** (Phase 2 open question). Both are claimed in `CLS-4.M2`, but they now rest on 34
and 32 practical sessions across S1–S4, including 6 weekly drill sessions in `KNF-1.M5` and a 4-week precision-cut
series in `CLS-4.M2.L1`. The level 5 evidence itself (5 demonstrations over 8 weeks, one for guests, one without a
recipe) comes from `CLS-4`, `MS-S4`, `IND-5` and `CAP-5`. That is enough.

### 4.4 Result per course

| Stage | Course | Title | Phase 2 h | Phase 3 h | Change | % | Lessons | Practical sessions |
|---|---|---|---|---|---|---|---|---|
| S1 | `ORI-1` | Orientation and baseline | 8 | 10.5 | +2.5 | +31 % | 7 → 7 | 5 |
| S1 | `SAF-1` | Food safety and hygiene I | 12 | 15.5 | +3.5 | +29 % | 10 → 10 | 9 |
| S1 | `WRK-1` | Mise en place, workflow and recipe literacy | 14 | 16 | +2 | +14 % | 11 → 11 | 10 |
| S1 | `EQP-1` | Equipment, heat sources and tools | 12 | 12 | ±0 | ±0 % | 9 → 9 | 6 |
| S1 | `KNF-1` | Knife skills I | 22 | 28.5 | +6.5 | +30 % | 15 → 15 | 28 |
| S1 | `TST-1` | Tasting and seasoning I | 16 | 17.5 | +1.5 | +9 % | 13 → 13 | 14 |
| S1 | `HEA-1` | Heat and cooking methods I | 26 | 30.5 | +4.5 | +17 % | 17 → 17 | 18 |
| S1 | `EGG-1` | Eggs and breakfast cookery | 16 | 18.5 | +2.5 | +16 % | 11 → 11 | 13 |
| S1 | `VEG-1` | Vegetables I | 24 | 27.5 | +3.5 | +15 % | 15 → 15 | 18 |
| S1 | `STA-1` | Rice, grains and pasta I | 14 | 17 | +3 | +21 % | 11 → 11 | 11 |
| S1 | `MS-S1` | Milestone S1 | 6 | 7 | +1 | +17 % | 4 → 5 | 3 |
| S2 | `SCI-2` | Food science II | 20 | 20.5 | +0.5 | +2 % | 11 → 11 | 12 |
| S2 | `HEA-2` | Cooking methods II | 22 | 26.5 | +4.5 | +20 % | 12 → 12 | 13 |
| S2 | `KNF-2` | Knife skills II | 8 | 10.5 | +2.5 | +31 % | 5 → 5 | 9 |
| S2 | `STK-2` | Stocks and soups | 24 | 27 | +3 | +12 % | 14 → 14 | 14 |
| S2 | `SAU-2` | Sauces I | 26 | 28 | +2 | +8 % | 12 → 12 | 20 |
| S2 | `POU-2` | Poultry | 22 | 24.5 | +2.5 | +11 % | 13 → 13 | 16 |
| S2 | `MEA-2` | Meat I | 28 | 29 | +1 | +4 % | 14 → 14 | 14 |
| S2 | `FSH-2` | Fish and shellfish I | 24 | 23.5 | −0.5 | −2 % | 12 → 12 | 16 |
| S2 | `LEG-2` | Legumes, grains II and dairy | 18 | 19.5 | +1.5 | +8 % | 10 → 10 | 13 |
| S2 | `BAK-2` | Baking science and pastry I | 30 | 37 | +7 | +23 % | 16 → 17 | 23 |
| S2 | `BRD-2` | Bread I | 22 | 23 | +1 | +5 % | 11 → 11 | 18 |
| S2 | `MS-S2` | Milestone S2 | 16 | 16 | ±0 | ±0 % | 6 → 6 | 3 |
| S3 | `SAF-3` | Food safety II | 6 | 7 | +1 | +17 % | 5 → 5 | 3 |
| S3 | `FLV-3` | Flavour architecture | 26 | 30 | +4 | +15 % | 14 → 14 | 18 |
| S3 | `BUT-3` | Home butchery and fabrication | 18 | 21.5 | +3.5 | +19 % | 9 → 9 | 13 |
| S3 | `SLW-3` | Braising, slow cooking and sous vide | 22 | 25.5 | +3.5 | +16 % | 12 → 12 | 14 |
| S3 | `FIR-3` | Barbecue and live fire | — | 28.5 | +28.5 | new | 0 → 12 | 14 |
| S3 | `GRD-3` | Cold kitchen (*garde manger*) | 22 | 25.5 | +3.5 | +16 % | 10 → 11 | 15 |
| S3 | `PRS-3` | Preservation | 22 | 23.5 | +1.5 | +7 % | 10 → 10 | 13 |
| S3 | `VEG-3` | Vegetables II: vegetable-led cooking | 20 | 24.5 | +4.5 | +22 % | 10 → 10 | 16 |
| S3 | `PST-3` | Pastry II | 30 | 33.5 | +3.5 | +12 % | 14 → 14 | 23 |
| S3 | `BRD-3` | Bread II | 20 | 24 | +4 | +20 % | 11 → 11 | 20 |
| S3 | `CUI-3` | World cuisines I | 34 | 99 | +65 | +191 % | 18 → 34 | 50 |
| S3 | `KMG-3` | Kitchen management | 18 | 19.5 | +1.5 | +8 % | 9 → 9 | 11 |
| S3 | `NUT-3` | Applied nutrition and allergens | 12 | 14 | +2 | +17 % | 6 → 6 | 9 |
| S3 | `MS-S3` | Milestone S3 | 20 | 20 | ±0 | ±0 % | 7 → 7 | 4 |
| S4 | `CLS-4` | Classical French repertoire | 30 | 35.5 | +5.5 | +18 % | 13 → 13 | 20 |
| S4 | `CUI-4` | World cuisines II: deep dives | 30 | 42 | +12 | +40 % | 12 → 12 | 18 |
| S4 | `PRO-4` | Advanced proteins | 24 | 31.5 | +7.5 | +31 % | 11 → 11 | 17 |
| S4 | `PST-4` | Advanced pastry and plated desserts | 26 | 30 | +4 | +15 % | 11 → 11 | 18 |
| S4 | `MOD-4` | Modern techniques | 16 | 17 | +1 | +6 % | 7 → 7 | 11 |
| S4 | `PLT-4` | Presentation and plating | 14 | 14 | ±0 | ±0 % | 6 → 6 | 8 |
| S4 | `RDV-4` | Recipe development | 22 | 25 | +3 | +14 % | 10 → 10 | 16 |
| S4 | `MNU-4` | Menu design and multi-course execution | 28 | 33.5 | +5.5 | +20 % | 14 → 14 | 16 |
| S4 | `MS-S4` | Milestone S4 | 20 | 20 | ±0 | ±0 % | 7 → 7 | 3 |
| S5 | `IND-5` | Cooking without recipes | 40 | 43.5 | +3.5 | +9 % | 15 → 15 | 29 |
| S5 | `RDV-5` | Original recipe portfolio | 24 | 38 | +14 | +58 % | 9 → 9 | 19 |
| S5 | `REV-5` | Consolidation | 6 | 15 | +9 | +150 % | 3 → 4 | 9 |
| S5 | `CAP-5` | Capstone | 40 | 51 | +11 | +28 % | 14 → 15 | 9 |
| | | **Total** | **1,020** | **1,277.5** | **+257.5** | **+25 %** | **526 → 559** | **722** |

Per stage: S1 170 → 200.5, S2 260 → 285, S3 270 → 396, S4 210 → 248.5, S5 110 → 147.5. The biggest single change
is `CUI-3`: a tradition now gets 12 h (history and pantry, two technique lessons, a meal) instead of 4 h (R5).

### 4.5 What this means for the learner

| Weekly time | Weeks (46 a year) | Years |
|---|---|---|
| 5 h | 256 | about 5.6 |
| **6.5 h** (Phase 1 A2) | **197** | **about 4.3** |
| 8 h | 160 | about 3.5 |

Ways to keep it practical without cutting content:
- **Most practice is dinner.** About 70 % of the practical sessions are household meals (Phase 1 A3), so much of the
  weekly time replaces cooking that happens anyway.
- **Plan around elapsed time.** Stocks, ferments, sourdough and low-and-slow cooks run in the background: start them
  on a weekend morning, and fit their active steps into the week.
- **S3 is the long stage** (about 61 weeks at 6.5 h). Its courses mostly need only `MS-S2`, so they can be interleaved:
  one `CUI-3` tradition a month alongside the technique courses, a bread or preserving project running in parallel.
- **Testing out.** The `ORI-1` diagnostic (Phase 1 A1) still lets an experienced learner skip demonstrated parts of S1.

---

## 5. The brief's nine checks

| # | Check | Finding | Action |
|---|---|---|---|
| 1 | Missing subjects | Barbecue and live fire; filled pasta, *gnocchi*, dumplings and tortillas; sandwiches, toasts and canapés; buttercream and finishing a cake (§3.2). The Americas, smoking as preservation and cheesemaking: left outside the core by decision. | R2, R5, R6 |
| 2 | Missing foundations | **Gelatin** was used in `PST-3` mousses (S3) but only taught in `MOD-4.M3` (S4). **Buttercream** was needed for `PST-4` layered desserts but taught nowhere. | R7 (gelatin taught in `PST-3.M4.L3`); R6 |
| 3 | Progression | Level 5 claimed in single lessons (`TS-SAU`, `TS-PRO`, `SA-HYG`, `SA-PHYS`) without the practice §10.3 needs. Now each is backed by at least 8 sessions. The buttercream lesson uses Swiss meringue (whites warmed with sugar, then whipped), not Italian, which needs sugar stages from `PST-4`. | R3, R4 |
| 4 | Overlooked dependencies | The new tagine lesson needs braising, but `SLW-3` is not a `CUI-3` prerequisite: it builds on `MEA-2.M6.L1` (S2) instead. `FIR-3` needs `SLW-3` (collagen) and `SAF-3` (long-cook safety): declared. The validator's transitive check (since Phase 2) found no other hidden same-stage dependency. Exercise texts were scanned for techniques not named in prerequisites; Phase 5 repeats this per course while writing the exercises. | R2, R5 |
| 5 | Redundancy and overlap | Reviewed pairs: `KNF-1.M5` / `KNF-2.M1` (speed at level 3, then volume at level 4); `STK-2.M3` / `CLS-4.M4` (seasoning a broth, then clarification); `EGG-1.M4` / `BAK-2.M5` / `PST-3.M4.L1` (custard as egg science, as pastry base, as variations); `SCI-2.M2.L2` / `POU-2.M6.L1` (brining as science, then applied); `HEA-2.M5` / `MEA-2.M6` / `SLW-3` (braising as a method, stewing cuts, braising in depth). All are **spiral by design** (Phase 1 §2.5): each later lesson raises the level and adds a variable. The risk is in Phase 5, where the content must not repeat; the outline needs no cut. | Note for Phase 5 |
| 6 | Insufficient practical training | Repetition not counted; levels without practice (§4.1). | R3, R4 |
| 7 | Gaps in theory | Gelatin (above); fire, smoke and charring compounds now in `FIR-3.M1.L1`. Beverage, nutrition and safety theory are below the CIA's on purpose (§3.1). | R2, R7 |
| 8 | Inadequate assessment | `MS-S1` had no reflection, although every milestone should have one (Phase 1 §6.5). **24 of the 66 Phase 2 competencies were re-tested at no milestone and not in the capstone** (25 with the new `TS-FIRE`), among them every ingredient-knowledge competency and `TS-EGG`, `TS-GRD`, `JD-CUES`, `RD-ADPT`. The capstone had no test cooks. | R8, R9, R10 |
| 9 | Too advanced for the stage | Checked: aspic, tempering, a *ballotine* and laminated dough are only *introduced* in S3–S4; lobster and game come in S4 after cheaper practice; water-bath canning comes after `SAF-3`. No move needed. Without outdoor space, `FIR-3` can't teach fire control: `TS-FIRE` then stops at level 2, and the record says so. | R2 |

---

## 6. Change log (Phase 3)

Lesson and module IDs are not renumbered: new lessons take the next free number, and no ID was retired.

| # | Change | Where | Reason |
|---|---|---|---|
| R1 | Lesson rows get two columns: **Practice** (sessions) and **Elapsed** (waiting time); **h** is active time. Lessons may be 0.5–6 h, modules about 1–14 h. | All outline files; Phase 2 *Conventions* | Answer 2; §4.2. |
| R2 | New course **`FIR-3` *Barbecue and live fire*** (S3, 28.5 h, 6 modules, 12 lessons, prerequisites `MS-S2`, `SAF-3`, `SLW-3`) and competency **`TS-FIRE`** (S3 3, S4 3, S5 4; target 4). Grill items added to equipment tier 2, with the no-outdoor-space alternative. `IND-5.M5.L2` adds live fire as an equipment constraint (`TS-FIRE→4`). | `S3-integration.md`; Phase 1 §5.3, §6.4, §7.3, §9.4 | Answer 1. |
| R3 | **Every lesson re-estimated** with the time model; course budgets in Phase 1 §7 replaced by the new hours; §6.1, §8 updated. Core 1,020 → 1,277.5 h. | All outline files; Phase 1 §3.3, §6.1, §7, §8 | Answer 3; §4. |
| R4 | **Practice rule** (§4.3) enforced; 9 lessons gained a practical task or a competency move to meet it. | §4.3 table | Answer 3 (thinly taught competencies). |
| R5 | **`CUI-3`**: every tradition gets a second technique lesson and a meal lesson (16 lessons; 4 → 12 h per tradition), including fresh filled pasta and *gnocchi*, dumplings and steaming, tortillas, a *tortilla española* and tapas, a tagine with couscous, dal and flatbreads, *nimono* and *yakimono*. `CUI-3.M1.L2` reads two recipes. | `S3-integration.md` | §3.1, §3.2. |
| R6 | New lessons **`BAK-2.M4.L4`** *Buttercreams, fillings and finishing a cake* and **`GRD-3.M5.L3`** *Sandwiches, toasts and canapés*, each with an exercise. | `S2-core-techniques.md`, `S3-integration.md` | §3.2; check 2. |
| R7 | **Gelatin** taught in `PST-3.M4.L3` (blooming, dosing, setting and melting points); `MOD-4.M3` deepens it. | `S3-integration.md` | Check 2. |
| R8 | **`MS-S1.M1.L5`** *Stage reflection* (1 h); `MS-S1` 6 → 7 h. | `S1-foundations.md`; Phase 1 §10.4 | Check 8. |
| R9 | **Milestone re-tests** for the 25 competencies (including `TS-FIRE`) that had none, attached to the components where they are naturally shown (e.g. `IN-MEA` and `IN-FSH` in the `MS-S2` plate, `TS-GRD` and `TS-FIRE` in the `MS-S3` evening, `CK-CUIS` and `RD-ADPT` in the `MS-S4` menu). The validator now requires a re-test for every competency. | All milestone sections | Check 8. |
| R10 | **`CAP-5.M1.L5`** *Test cooks* (6 h, 3 sessions, 2 weeks); `CAP-5` prep days 2.5 → 5 h each. **`REV-5.M1.L4`** a third targeted-practice block. `RDV-5` development cycles 3 → 5 h each (3 test cycles over 3 weeks). | `S5-mastery.md` | §4.1. |
| R11 | **Validator**: 8-column lesson rows, elapsed-time format, the practice rule, milestone re-tests, the 0.5–6 h lesson range, practical sessions in the course summary, and a generated *Weekly load* table. | `tools/validate_curriculum.py` and its tests | Keeps R1–R10 true on every later edit. |
| R12 | Source **[S32]**: the CIA's credit-hour definition. | `sources.md` | §3.1. |

---

## 7. End of phase

### Decisions and assumptions

1. **Hours are estimates of active time**, built from an explicit model (§4.2), not fitted to a budget. Elapsed time is
   recorded but not counted.
2. **The core is 1,277.5 h.** It is still below the CIA's minimum workload for comparable content (about 1,485 h without
   the Americas, §3.1),
   for reasons stated per subject: production volume, and reductions agreed in Phase 1 or by the reviewer.
3. **The practice rule** (1 / 3 / 5 / 8 sessions for levels 2–5) is a design assumption derived from Phase 1 §10.3. It is
   a floor, and Phase 4 may raise it for individual competencies.
4. **Spiral repetition is kept.** No lesson was removed as redundant; Phase 5 must make each repetition add a variable.
5. **Barbecue is core; smoking as preservation is not.** Smoke as a flavour is taught inside `FIR-3`.
6. **Every competency is re-tested** at a milestone or the capstone, and the validator keeps it that way.

### Gaps and open questions for the review

1. **Duration.** At the planned 6.5 h/week the core now takes about **4.3 years** (3.5 years at 8 h/week). Do you
   accept that, or should the planning assumption move to 8 h/week?
2. **No outdoor space.** Without a garden or balcony, `FIR-3` teaches browning but not fire control, and `TS-FIRE`
   stops at level 2 for that learner. Is that acceptable, or should `TS-FIRE` be optional for them?
3. **`CUI-3` is now 99 h**, the largest course by far. Keep it as one course, or split it into two courses (for
   example European and Mediterranean traditions, then Asian and Mexican) so that progress shows sooner?

### Recommendations for Phase 4

1. Write the full competency framework (Phase 1 §5.3 plus `TS-FIRE`): knowledge, practical ability, independence,
   assessment method and success criteria per competency, using the lesson and milestone mappings the validator now
   guarantees.
2. Turn each milestone's components into briefs and weighted rubrics, starting from the re-tests added in R9. Check
   that every milestone mixes objective, partly verifiable and self-assessed evidence (Phase 1 §10.2).
3. Define how practical sessions become *evidence*: what a successful demonstration looks like for each competency, so
   that the practice rule can later be checked against the learner's record instead of the curriculum.
4. Re-check the per-course CIA credits against the catalog PDF (§3.1 caveat), and cite exact safety figures for
   `FIR-3.M4.L1` (long cooks), `SAF-3` and `SLW-3.M5` from the primary authorities [S21][S22][S24].
