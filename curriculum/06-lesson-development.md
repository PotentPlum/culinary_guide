# Phase 6 — Lesson development

Status: **S3 in progress** (2026-11-05). Phase 5 is complete ([its overview](05-course-development.md)). The second
reader's worksheet is open ([`06-safety-review.md`](06-safety-review.md)). This phase runs in parallel with it (S5
review answer 4): lesson files are drafts until the reader's log is in (§3). The pilot (`ORI-1` and `SAF-1`, §6) was
reviewed on 2026-11-05, and its answers are applied (§2, §5, §6.3). All ten S1 lesson files are written, and their
review answers are applied (§7). All eleven S2 lesson files are written, and their review answers are applied (§8).

**Courses developed:** ORI-1, SAF-1, WRK-1, EQP-1, KNF-1, TST-1, HEA-1, EGG-1, VEG-1, STA-1, SCI-2, HEA-2, KNF-2, STK-2, SAU-2, POU-2, MEA-2, FSH-2, LEG-2, BAK-2, BRD-2, SAF-3, FLV-3, BUT-3, SLW-3, FIR-3, GRD-3

This phase writes the lessons that the course files of Phase 5 outline. For each course it gives one lesson file with:
- every lesson, written for the learner: why it matters, what is needed, the explanation, the practice sessions step
  by step, what to look for, what goes wrong and how to fix it, safety, and what to record;
- every exercise, written to its support level: a full recipe, a framework or only the brief (Phase 1 §6.6).

What this phase does **not** contain:
- **New structure.** Lessons, sessions, exercises, knowledge checks and evidence rows stay as Phase 5 set them. A
  lesson that needs more is raised as a question and logged (§5), never added silently.
- **New safety figures.** A temperature or time that keeps food safe comes only from
  [assessment §9](04-assessment.md#9-safety-figures-used-in-assessment) or from the course file. The validator checks
  temperatures (§1.4).
- **The knowledge checks.** They are in the course files. A lesson file links to its module's check and does not
  repeat it.

---

## 1. The lesson file

There is one file per course, `06-lessons/<stage>/<course>.md`, for example `06-lessons/S1/SAF-1.md`. It is written
in English for the learner, with the Dutch (NL) and French (FR) names the README asks for. Parts marked *generated*
are produced by `tools/validate_curriculum.py --write`.

### 1.1 Layout

| Part | Content | Source |
|---|---|---|
| Title | `# <course ID> — <course title>` | written |
| Header | Stage, hours, lessons, exercises, and a link to the course file | generated |
| Status | `**Status:** draft — …` or `**Status:** final` (§3) | written |
| Before you start | What the course is for, in a few lines, and what to have ready | written |
| Module | `## <module ID> — <title>`, with a line naming its knowledge check in the course file | written |
| Lesson | `### <lesson ID> — <outline title>`, its generated line (hours, sessions, objectives), then the lesson fields (§1.2) | generated and written |
| Exercise | `### <exercise ID> — <title in the course file>`, its generated line (type, support, description), then the exercise fields (§1.3) | generated and written |

### 1.2 Lesson fields

Every lesson has these fields, in this order:

| Field | What it holds |
|---|---|
| **Why it matters** | Two or three sentences: what the learner can do after the lesson, and where it is used later. |
| **You need** | Equipment, within the stage's tier, and ingredients in metric quantities. "Nothing" for a theory lesson. |
| **Explanation** | One part for each Concept of the course file, written out: the principle, the reason, and an example. |
| **Practice** | Numbered sessions, **as many as the course file's Sessions**, each with its time and its steps. "None: theory only" for a lesson with no sessions. |
| **Look for** | The cues of the course file, as the learner sees, hears, smells or feels them. |
| **If it goes wrong** | A table of fault, cause and fix, from the course file's Faults. |
| **Safety** | The rules that apply, each naming its §9 row or lesson, or "No special risks beyond the kitchen basics (`SAF-1`)." |
| **Record** | What goes into the journal (`ORI-1.M1.L2`): readings, photos, notes. |

### 1.3 Exercise fields

| Field | What it holds |
|---|---|
| **Task** | What the learner does, in the words of the course file's Aim and Conditions. |
| **You need** | Equipment and ingredients. For a *brief*, only the limits: budget, season, equipment. |
| **Recipe** | Only for support level *recipe*: quantities and numbered steps with times and cues. |
| **Framework** | Only for support level *framework*: the ratio or structure and the decisions left to the learner, without every quantity. |
| **Show** | The evidence, as the course file's Evidence asks, and how it is scored. |
| **Time** | The time, as in the course file. |

An exercise at support level *brief* has neither **Recipe** nor **Framework**: the brief is the whole support.

### 1.4 House rules

1. **Safety comes first, and only from checked figures.** Every temperature in a lesson file (`N °C`) must appear in
   §9's Figure column or in the course file. The validator reports any other, so a figure from memory can't slip in.
   Where a figure is under a review question (§2), the lesson uses §9 as it stands and says so.
2. **Produce is Dutch and seasonal** where the lesson chooses it, with a winter and a summer option where the season
   matters; fish is MSC-labelled or a seasonal North Sea species.
3. **Metric quantities**, by weight where it matters (grams, millilitres, °C, centimetres).
4. **Equipment stays within the stage's tier** (the course file's Materials and equipment).
5. **One vocabulary**: the terms of the course file, with NL and FR names the first time.

## 2. Source re-checks

The Phase 5 reviews left a list of sources to read or re-read before lessons rest on them (Phase 5 §7.3, §9.3, §10.3
and §11.3). Each was read once on 2026-11-05; no page that failed or refused before was tried again.

| Source | Result | Effect |
|---|---|---|
| Voedingscentrum, raw egg [S44], soft-boiled egg in pregnancy [S45] | Read. Risk groups avoid dishes "waarin ei rauw blijft"; in pregnancy an egg is eaten only fully set (8 minutes); dishes with raw egg are kept "maximaal 24 uur" in the fridge | The risk-group row for soft whole eggs is confirmed. The 24-hour limit is stricter than §9's 2 days: question 2 |
| Voedingscentrum, pregnancy [S46] | Read | Smoked fish, raw sprouts and liver are not yet in the risk-group row; hard raw-milk cheese is safe: question 3 |
| Voedingscentrum, meat and core temperatures [S47] | Read (the encyclopedia page on meat only in part; it gives no figures) | "Vlees en kip: minimaal 75" °C at the core; fish 60 °C; rosé whole beef or pork 55 °C. Stricter than §9 for poultry (74 °C) and minced meat (70 °C): question 1 |
| Voedingscentrum, *Schijf van Vijf* [S48] | Read; the amounts are in a separate tool | `NUT-3` cites the page; no change |
| Dutch Red Cross, burns [S49] | Read: cool at once for 10 to 20 minutes under lukewarm, gently running tap water | Closes S1 review question 3; `SAF-1.M4.L2` cites it |
| Dutch rules on nitrous oxide [S50] | Read (VNG factsheet): the ban does not apply to food use; for private persons more than 10 chargers is a strong sign of other use | The siphon rule stands: question 4 |
| *Alcoholwet* article 20 | The page loaded, but the article itself was not in the part that could be read | The alcohol row stays on [S43] ("Geen 18, geen alcohol"); no change |
| NVWA guidance on game and *Trichinella* | No NVWA page found; a 2012 parliamentary answer refused automated access | The wild-boar row stands; it is stricter (inspected supplier only); no change |
| FSIS poultry and egg pages | Not tried again: the site refused automated access before | The poultry table stays on [S34], the egg rule on [S38]; no change |

**Questions for the review** (§6 collects them at the end of the pilot; answered 2026-11-05):
1. **Core temperatures.** The Voedingscentrum gives at least 75 °C for meat and chicken [S47]. §9 uses the Food Code:
   poultry 74 °C, minced meat 70 °C instantaneous. §9's own rule says the Dutch home advice wins where it is stricter.
   I recommend 75 °C as the instantaneous figure for poultry and for minced meat in §9. Keep the Food Code's
   time-and-temperature tables (minced meat, roasts, low-temperature poultry) as the measured alternative taught from
   S2 and S3, keep intact meat and fish at 63 °C (stricter than the Voedingscentrum's 60 °C for fish), and keep the
   steak row. "74 °C" appears about 150 times in 26 course files, in about 26 safety items, and 70 °C about 50 times;
   the reheating row (74 °C for 15 seconds) would stay, so each mention is checked by hand.
   **Answer:** as recommended. Applied to §9 (poultry, stuffed foods, wild game, and the instantaneous figure for minced
   meat, minced fish and offal) and to 141 mentions in the course files and `SAF-1`; reheating, doneness and egg
   figures, and wrong answer options that stay wrong, were left as they are.
2. **Dishes with raw egg.** The Voedingscentrum keeps them at most 24 hours [S44]; §9 applies the 2-day leftovers row
   (`SAU-2` mayonnaise). I recommend 24 hours, also for pasteurised egg, because the page makes no exception.
   **Answer:** as recommended. Added to the raw-egg row of §9; `SAU-2`, `PST-3` and `PST-4` changed.
3. **The risk-group row.** I recommend adding cold-smoked fish, raw sprouts and, in pregnancy, liver [S46], and noting
   that hard raw-milk cheese is safe.
   **Answer:** as recommended. The row also names cold cuts made from raw meat, soft raw-milk cheese and raw milk,
   which the same page lists; smoked fish is any smoked fish "served cold", since the page includes chilled smoked
   salmon. `GRD-3` and `PRO-4` changed (liver pâté and liver in pregnancy, smoked mackerel, the cheese board).
4. **Chargers for the siphon.** I recommend adding one line to the tier 3 rule: chargers bought for food use, a few at
   a time.
   **Answer:** as recommended. Added to the tier 3 rule of §9 and to `MOD-4`.

Lessons rest on §9 as it now stands. A later source finding that would change a figure is raised here as a question
first, and the lessons that depend on it say so in their Safety field until it is answered.

## 3. From draft to final

A lesson file is a **draft** until all of these hold:
1. the second reader's log covers every safety item of the course, and none is open;
2. every item the reader disagreed with is fixed in the course file, and the fix is in the log;
3. the review questions its lessons rest on (§2) are answered and applied;
4. the lesson file has been read again against the course file.

Then its status becomes `**Status:** final`. The validator refuses `final` while any safety item of the course is
unread or marked *disagrees*.

## 4. Order of work

1. **The pilot:** `ORI-1` and `SAF-1`, then a review of the format and the length before more is written.
2. **Then stage by stage,** course by course in outline order, with a review after each stage, as in Phase 5.
3. **Each course is written from its course file:**
   - Concepts become the Explanation;
   - Sessions become the Practice;
   - Cues become Look for;
   - Faults become If it goes wrong.

   Equipment and ingredients come from its Materials and equipment.

## 5. Decisions and change log

Changes to an agreed structure are logged here.

| Where | Was | Now | Why |
|---|---|---|---|
| [Assessment §9](04-assessment.md#9-safety-figures-used-in-assessment), poultry, stuffed foods and wild game | 74 °C instantaneous (Food Code) | 75 °C instantaneous | The Voedingscentrum's *minimaal 75* for meat and chicken [S47] is stricter; pilot review question 1 |
| §9, minced meat, minced fish and offal | 70 °C instantaneous, or the time table | 75 °C instantaneous, or the time table | As above; the Food Code's times stay as the measured alternative, because the Voedingscentrum gives none |
| §9, raw-egg row | Kept as leftovers (2 days) | At most 24 hours, also with pasteurised egg | [S44]; pilot review question 2 |
| §9, risk-group row | Raw or lightly cooked fish, rare meat, soft eggs | Also smoked fish served cold, cold cuts made from raw meat, soft raw-milk cheese and raw milk, raw sprouts; no liver in pregnancy; hard raw-milk cheese is safe | [S46]; pilot review question 3 |
| §9, tier 3 rule | "The Dutch rules on nitrous oxide are to be checked" | Chargers bought for food use, a few at a time; never inhaled | [S50]; pilot review question 4 |
| 26 course files and `06-lessons/S1/SAF-1.md` | 74 °C and 70 °C as safe core figures | 75 °C; the 24 worksheet rows whose text changed copied again, their verdicts empty | Follows the §9 change |
| `BAK-2.M4.L4`, Swiss meringue buttercream | "hot, about 70 °C on the probe" | every part at least 71 °C on the probe | It contradicted the egg-thickened desserts row of §9 (71 °C); found while checking the 70 °C mentions |
| `tools/validate_curriculum.py`, the temperature check (§1.4) | Only the last number of a list ("150, 180 and 220 °C") was read | Every number of a list or range is read, in lesson files and in the allowed figures | A list in a lesson ("62, 65 and 70 °C") would otherwise slip past the check; found with `EQP-1` |
| `EQP-1.M3.L3`, the thermometer check | "checked in ice water" | "checked in ice water, where it reads 0 °C" | The lesson needs the reading, and figures in lessons come from the course file or §9 |
| `EGG-1.M4.L1`, the baked custard | "in a moderate oven" | "in a moderate oven (about 150 °C)" | The lesson's recipe needs an oven setting, and figures in lessons come from the course file or §9 |
| `STA-1.M4.X1`, Inputs | "a vinaigrette" | "a vinaigrette (about 3 parts oil to 1 part vinegar or lemon, salted, balanced by taste as in `TST-1.M3`)" | No S1 course teaches a vinaigrette; S1 review answer 2 |
| `TST-1.M2.X2`, Procedure 2 | "estimated by weighing the salt and the water" | "estimated as about half of its drained weight times the water's strength (about 1 g for 250 g at 10 g/l)" | The method the lesson uses; S1 review answer 2 |
| `TST-1.M2.X1`, Inputs | 5 litres of stock and 51 g of salt | Also 1 litre and 16 g of salt for the "just right" walk of `TST-1.M2.L2` | The lesson's walk needs its own litre; S1 review answer 2 |
| `EGG-1.M4.L1`, the Safety concept | Only the stirred custard named | The baked custard probed in the centre, every part at least 71 °C (§9, egg-thickened desserts) | As the lesson already does; S1 review answer 2 |
| `HEA-2.M4.X1`, the thermometer check | "in boiling water" | "in boiling water, where it reads about 100 °C" | The lesson needs the reading, as for `EQP-1.M3.L3` |
| `HEA-2.M4.L3`, double-fried chips | "a first fry at a lower temperature" | "a first fry at a lower temperature (about 150 °C)" | The chip recipe needs the figure; 150 °C is the course's own "too cool for a crust" point |
| `HEA-2.M5.L1`, the braise | "often in a low oven" | "often in a low oven (about 150 °C)" | The braise and the methods lab need an oven setting |
| `SAU-2`, Assessment criteria | "or kept beyond 2 days" | "or kept beyond 24 hours" | The §9 raw-egg row and `SAU-2.M4.L2` keep a raw-yolk sauce at most 24 hours; the criteria said 2 days |
| `STK-2.M4.L2`, the *liaison* | "stirred in off the heat" | Also "brought back to at least 71 °C on the probe without boiling (the egg-thickened row of assessment §9)" | The lesson's safety step; S2 review answer 2 |
| `STK-2.M3.L1`, the *heldere soep* | "meatballs" | "meatballs, simmered until the probe reads 75 °C (minced meat, assessment §9)" | As the lesson does; S2 review answer 2 |
| `POU-2.M5.L2`, the giblets | "the liver for a pâté or a pan sauce" | Also "cooked to 75 °C (poultry offal, assessment §9)" | As the lesson does; S2 review answer 2 |
| `BAK-2.M3.X1` and `BAK-2.M5.L2`, the quiche and the baked custard | Judged by the wobble only | Also probed in the centre to at least 71 °C (the egg-thickened row of §9, whose source covers dishes containing eggs) | As `EGG-1.M4.L1` does; S2 review answer 2 |
| `BRD-2.M3.L1`, Safety | Gloves, paper, no water on glass | Also the lid lifted away from you, the water poured with the arm clear of the steam, a metal or ovenproof knob | The lesson's loading rules; S2 review answer 2 |
| `POU-2.M1.L1`, labels | "free range (*scharrel*, *vrije uitloop*)" | "free range (*vrije uitloop*); *scharrel*, indoor housing with more space than standard" | *Scharrel* is not free range; S2 review answer 3 |
| `FSH-2.M4.L2`, grey shrimp | "*garnalenkroketten* filling, shrimp on toast, or a stuffed tomato" | "shrimp on toast, or a stuffed tomato" | A croquette needs a thick roux and deep-frying; as the lesson; S2 review answer 3 |
| `SCI-2.M4.L2` and the `SCI-2` budget | "a quick mayonnaise from one yolk"; "eggs" | "one pasteurised yolk (the raw-egg row of assessment §9)"; "pasteurised egg yolks" | The §9 raw-egg rule; S2 review answer 3 |
| `SAU-2.M4.L1`, by blender | "a whole egg" | "a whole pasteurised egg (from a carton, about 50 g)" | The §9 raw-egg rule; S2 review answer 3 |
| `KNF-2.M1.L1` and the `TS` tolerances | No size for *chiffonade* | Ribbons 1–2 mm wide | `TS-KNF` level 4 asks for it "within range"; S2 review answer 3 |
| `KNF-2.M3.L2`, the edge log | "a self-score" | "a self-score in four bands: 1 tears the paper, 2 catches, 3 slices with a few catches, 4 slices cleanly" | The bands the lesson and the AI review use; S2 review answer 3 |

## 6. End of the pilot

### 6.1 What was delivered

- **The safety worksheet** [`06-safety-review.md`](06-safety-review.md): all 278 safety items of S1–S5, word for word,
  each mapped by hand to the §9 rows it rests on (or to the lesson that teaches its course rule), with reading
  instructions and empty verdict columns. The validator keeps it in step with the course files and counts the log.
- **The source re-checks** (§2): seven new sources [S44]–[S50]; four questions that would change §9.
- **The method** (§1) and its checks in `tools/validate_curriculum.py`: headings, fields, practice counts, support
  levels, temperatures, and the draft-to-final rule.
- **Two lesson files**, both drafts:

  | Course | Lessons | Exercises | Words | Words per lesson or exercise (range) |
  |---|---|---|---|---|
  | [`ORI-1`](06-lessons/S1/ORI-1.md) | 7 | 5 | about 5,300 | 185–725, about 430 on average |
  | [`SAF-1`](06-lessons/S1/SAF-1.md) | 10 | 5 | about 7,600 | 225–725, about 490 on average |

### 6.2 Decisions

1. **One file per course,** like Phase 5, so a course is reviewed and finalised as one unit.
2. **Course rules stay in the lessons, figures stay in §9.** The burn-cooling time [S49] is a first-aid rule, not a
   food-safety figure, so it is written into `SAF-1.M4.L2` and cited, not added to §9.
3. **The label key** of `SAF-1.M4.X1` lists only the hidden sources the course file names, and says that the label
   decides: no unsourced claims about what a product "usually" contains.
4. **Recipes where the support level is *recipe*.** `SAF-1.M3.X2` now has a full recipe (a minced-beef and vegetable
   sauce with a seasonal leek or courgette). The `ORI-1` exercises use the learner's own dish, as the course file asks,
   with the procedure as the recipe.
5. **Figures under question are flagged in the lesson.** `SAF-1.M3.L2` gives the §9 core temperatures and names review
   question 1 in its Safety field.

### 6.3 Questions for the review (review answers, 2026-11-05)

1. **The format** (§1.2 and §1.3). Is the lesson layout right: why it matters, you need, explanation, practice, look
   for, faults table, safety, record? I recommend keeping it. The validator holds every later file to it.
   **Answer:** as recommended.
2. **The length.** About 430–490 words per lesson or exercise. At that rate the 519 lessons and 272 exercises come to
   about 360,000 words. I recommend keeping it for S1 and S2, where most exercises carry a full recipe, and expecting
   shorter exercise texts from S3, where frameworks and briefs replace recipes.
   **Answer:** as recommended.
3. **The four source questions** of §2: 75 °C for poultry and minced meat; 24 hours for dishes with raw egg; cold-smoked
   fish, raw sprouts and liver in the risk-group row; chargers for the siphon. My recommendation for each is in §2.
   Question 1 changes `SAF-1.M3.L2` and many later items, so I recommend answering it before the rest of S1 is written.
   **Answer:** all four as recommended, applied before the rest of S1 (§2, §5).
4. **The second reader.** The worksheet is ready, with 0 of 278 items read. Who reads it, and when? Every lesson file
   stays a draft until it is done.
   **Answer:** open. The lessons go on as drafts, and the question comes back at the end of S1.

### 6.4 Next

After this review I apply the answers, then write the rest of S1 in outline order (`WRK-1`, `EQP-1`, `KNF-1`, `TST-1`,
`HEA-1`, `EGG-1`, `VEG-1`, `STA-1`), one commit per course, and stop for review at the end of S1.

## 7. End of S1

### 7.1 What was delivered

All ten S1 courses have a lesson file, every one a draft until the second reader's log is in (§3). Words are counted
without the generated blocks.

| Course | Lessons | Exercises | Words | Words per lesson (range, average) | Words per exercise (range, average) |
|---|---|---|---|---|---|
| [`ORI-1`](06-lessons/S1/ORI-1.md) | 7 | 5 | about 5,000 | 410–690, about 520 | 140–300, about 220 |
| [`SAF-1`](06-lessons/S1/SAF-1.md) | 10 | 5 | about 7,100 | 420–680, about 510 | 210–540, about 320 |
| [`WRK-1`](06-lessons/S1/WRK-1.md) | 11 | 6 | about 8,100 | 470–580, about 510 | 280–530, about 350 |
| [`EQP-1`](06-lessons/S1/EQP-1.md) | 9 | 3 | about 6,000 | 460–570, about 520 | 290–350, about 320 |
| [`KNF-1`](06-lessons/S1/KNF-1.md) | 15 | 7 | about 9,200 | 370–570, about 460 | 200–290, about 250 |
| [`TST-1`](06-lessons/S1/TST-1.md) | 13 | 6 | about 9,100 | 460–600, about 500 | 280–440, about 340 |
| [`HEA-1`](06-lessons/S1/HEA-1.md) | 17 | 8 | about 11,700 | 400–600, about 520 | 240–350, about 300 |
| [`EGG-1`](06-lessons/S1/EGG-1.md) | 11 | 6 | about 8,200 | 470–680, about 520 | 270–410, about 320 |
| [`VEG-1`](06-lessons/S1/VEG-1.md) | 15 | 7 | about 9,800 | 420–580, about 480 | 240–340, about 290 |
| [`STA-1`](06-lessons/S1/STA-1.md) | 11 | 5 | about 7,400 | 430–530, about 480 | 270–430, about 340 |
| **S1** | **119** | **58** | **about 81,600** | about 500 | about 310 |

At this rate the remaining stages come to roughly 250,000 words more, less where frameworks and briefs replace recipes
(§6.3, answer 2).

### 7.2 Decisions

1. **The format and the length are kept** (§6.3, answers 1 and 2). Every file passes the validator's checks of headings,
   fields, practice counts, support levels and temperatures.
2. **Figures in lessons come only from §9 or the course file.** Where a lesson needed a figure the course file lacked,
   the course file was edited and the edit logged (§5: `EQP-1.M3.L3`, `EGG-1.M4.L1`). Where the setting is the learner's
   own, the lesson points to it instead of a number: roasting and baking use "your oven table's setting"
   (`EQP-1.M2.X1`), throughout `VEG-1`.
3. **The temperature check reads every number of a list or range** (§5), with a test.
4. **Quantities beyond the course file's Inputs** are chosen in the lesson when a recipe needs them (ratios, butter,
   wine, a sixth litre of stock), and listed in §7.3 so the review can move any of them into the course file.
5. **Routines are put before the first session that needs them.** `STA-1` cooks rice from module 1, but its cooling
   routine is taught in module 4, so the routine is also set out in its Before you start. `TST-1` (tasting rules),
   `HEA-1` (the cue log) and `EGG-1` (the risk-group paragraph) do the same.

### 7.3 Questions for the review (review answers, 2026-11-05)

1. **The S1 lessons.** Are they right for a home learner: the level of detail, the recipes, the Dutch seasonal choices?
   I recommend reviewing two or three files closely (`HEA-1`, `EGG-1` and `STA-1` carry the most safety content) and
   the rest by sampling.
   **Answer:** accepted as written. The files stay drafts until the reader's log is in.
2. **Gaps found in the course files.** None changes a structure; each is a choice made in the lesson. I recommend
   moving the first four into their course files, with logged edits, and leaving the rest as written:
   - **`STA-1.M4.X1` lists a vinaigrette, but no S1 course teaches one** (it comes in S2). The lesson gives a 3 : 1
     starting point in the framework. Add it to the course file, or link `TST-1.M3`.
   - **`TST-1.M2.X2`** says the salt absorbed is "estimated by weighing the salt and the water". The lesson uses half of
     drained weight × water strength (about 1 g for 250 g); the course file could state the method.
   - **`TST-1.M2.L2`**: the "just right" walk needs a sixth litre of stock beyond the Inputs of `TST-1.M2.X1`.
   - **`EGG-1.M4.L1`**: the baked custard is probed to 71 °C by the egg-thickened desserts row of §9; the course file
     names that row only for the crème anglaise.
   - **Ratios and quantities** the course files leave open: the glazing ratio (`VEG-1.M4.L1`), the rice ratios for
     jasmine and brown rice and the quinoa ratio (`STA-1.M2.L1`, `STA-1.M4.L2`), the risotto quantities
     (`STA-1.M2.X1`).
   - **Readings of ambiguous course text:** "250 g spread against 250 g piled" (`HEA-1.M2.X1`, read as two batches of
     125 g); "a roast or a thick piece of meat" (`HEA-1.M1.L3`, a pork tenderloin); the four bean batches of
     `VEG-1.M3.X1`, run two at a time.
   - **Overlaps kept as written:** `TST-1.M4.L2` and `TST-1.M4.X1` are the same session; `EGG-1.M2.L2` asks for three
     jammy runs and `EGG-1.M2.X1` for two.
   - **Held poached eggs** (`EGG-1.M3`): §9 has no row for soft eggs held in ice water. The lesson says "used the same
     morning; in the fridge if well ahead", without a figure.

   **Answer:** as recommended. The first four are in their course files, logged in §5; the rest stay as written.
3. **The second reader** (§6.3, answer 4). The worksheet is still at 0 of 278 items, so all ten files stay drafts. Who
   reads it, and when? I recommend starting the reading now with the 48 S1 items, so S1 can be finalised while S2 is
   written.
   **Answer:** as recommended. The worksheet names the reading order; who reads it is still open.
4. **S2 next.** I recommend writing S2 in outline order (`SCI-2`, `HEA-2`, `KNF-2`, `STK-2`, `SAU-2`, `POU-2`, `MEA-2`,
   `FSH-2`, `LEG-2`, `BAK-2`, `BRD-2`), one commit per course, and stopping for review at the end of S2, as for S1.
   **Answer:** as recommended.

### 7.4 Next

The answers are applied. S2 is written in outline order, one commit per course; its review is in §8.

## 8. End of S2

### 8.1 What was delivered

All eleven S2 courses have a lesson file, every one a draft until the second reader's log is in (§3). Words are counted
without the generated blocks.

| Course | Lessons | Exercises | Words | Words per lesson (range, average) | Words per exercise (range, average) |
|---|---|---|---|---|---|
| [`SCI-2`](06-lessons/S2/SCI-2.md) | 11 | 6 | about 7,400 | 370–500, about 470 | 220–330, about 280 |
| [`HEA-2`](06-lessons/S2/HEA-2.md) | 12 | 7 | about 8,400 | 410–590, about 480 | 250–390, about 300 |
| [`KNF-2`](06-lessons/S2/KNF-2.md) | 5 | 4 | about 4,300 | 420–600, about 520 | 290–350, about 320 |
| [`STK-2`](06-lessons/S2/STK-2.md) | 14 | 7 | about 8,900 | 390–510, about 440 | 240–420, about 320 |
| [`SAU-2`](06-lessons/S2/SAU-2.md) | 12 | 9 | about 10,500 | 470–620, about 550 | 260–410, about 370 |
| [`POU-2`](06-lessons/S2/POU-2.md) | 13 | 6 | about 9,200 | 390–580, about 500 | 270–420, about 370 |
| [`MEA-2`](06-lessons/S2/MEA-2.md) | 14 | 7 | about 9,800 | 400–550, about 470 | 240–430, about 360 |
| [`FSH-2`](06-lessons/S2/FSH-2.md) | 12 | 6 | about 8,200 | 400–590, about 470 | 240–420, about 340 |
| [`LEG-2`](06-lessons/S2/LEG-2.md) | 10 | 5 | about 7,200 | 380–560, about 490 | 270–400, about 350 |
| [`BAK-2`](06-lessons/S2/BAK-2.md) | 17 | 9 | about 12,800 | 390–580, about 500 | 350–430, about 400 |
| [`BRD-2`](06-lessons/S2/BRD-2.md) | 11 | 5 | about 8,000 | 430–610, about 490 | 380–420, about 410 |
| **S2** | **131** | **71** | **about 94,600** | about 490 | about 350 |

Three `SCI-2` units run short of the ranges (`SCI-2.M5.L2`, `SCI-2.M4.X1` and `SCI-2.M5.X1`): they are fixed
comparisons with short recipes, and nothing was padded to reach a count. S1 and S2 together come to about 176,000
words.

### 8.2 Decisions

1. **The format and the length are kept,** as in S1 (§7.2). Every file passes the validator's checks.
2. **Figures in lessons come only from §9 or the course file.** Four course-file edits were needed and are logged in
   §5: three in `HEA-2` (the boiling-water check, the first fry and the braising oven) and one in `SAU-2` (a raw-yolk
   sauce kept at most 24 hours, not 2 days).
3. **Oven settings the course file lacks:**
   - in `STK-2` (brown stock) and `MEA-2` (the steak finish, the reverse sear), the lesson points to "your oven table's
     setting";
   - in `BAK-2` and `BRD-2`, each recipe uses one of the course file's own figures (for example 170–180 °C for butter
     cakes, 230–250 °C for bread), and the bake is judged by the skewer, the wobble or the probe, not the clock.
4. **Recipes, frameworks and checklists that a course file leaves "for Phase 6 to write" are written in the lesson.**
   For example: the mayonnaise and the hollandaise, the six fault cards (`SAU-2.M7.X1`), the freshness checklist
   (`FSH-2.M1.X1`), the butcher questions (`MEA-2.M2.X1`), and every `BAK-2` and `BRD-2` formula. They are listed in
   §8.3 so the review can move any of them into its course file.
5. **Comparisons stay within §9.** Raw and warm egg sauces use pasteurised egg and are kept at most 24 hours. Rare meat
   goes only to tasters outside the risk groups. The bean comparison uses chickpeas, as its course file says.
6. **No unsourced claims.** Statements such as "the most eaten meat" or shelf lives without a source were taken out
   while writing.

### 8.3 Questions for the review (review answers, 2026-11-05)

1. **The S2 lessons.** Are they right for a home learner? I recommend reviewing three files closely and sampling the
   rest. The three are `SAU-2` (raw and warm egg), `POU-2` (poultry) and `BAK-2` (custards and creams).
   **Answer:** as recommended. The files stay drafts until the reader's log is in.
2. **Safety readings the course files don't state.** I recommend moving each into its course file as a logged edit:
   - **`STK-2.M4.L2`:** a soup thickened with a liaison is heated to at least 71 °C without boiling. The lesson takes
     this from the egg-thickened row of §9, by analogy.
   - **The quiche and the baked custards of `BAK-2`:** probed to at least 71 °C, as `EGG-1.M4.L1` does. §9's row names
     desserts; should its wording also name savoury custards?
   - **Two dishes probed to 75 °C:** the *soepballetjes* in `STK-2` (minced meat) and the chicken liver in `POU-2.M5.L2`
     (the poultry-offal row).
   - **`BRD-2.M3.L1` adds loading rules:** the lid is lifted away from you, the water is poured with your arm clear of
     the steam, and the pot has an ovenproof knob. Check them against the module's two safety items.

   **Answer:** as recommended. All four are in their course files (§5). §9 is unchanged: its egg-thickened row cites
   a source that covers dishes containing eggs [S38], and the `BAK-2` course file now says so, so the worksheet's row
   mapping stays as it is.
3. **Corrections to course files:**
   - **`POU-2.M1.L1`** groups *scharrel* with free range, but *scharrel* is indoor housing; the lesson separates them.
   - **`FSH-2`** names a *garnalenkroket*. The lesson replaces it with shrimp on toast or a stuffed tomato, because a
     croquette needs the thick roux and deep-frying. Should the croquette stay as an option?
   - **`SCI-2.M4.L2` and `SAU-2`:** pasteurised egg is missing from their Materials. The `SAU-2` blender mayonnaise
     uses pasteurised whole egg, where the course file says "a whole egg".
   - **`KNF-2`:** the chiffonade has no tolerance, and the edge-log bands are defined only in the lesson. I recommend
     adding a range (for example 1–2 mm ribbons) and the four bands.
   - **`POU-2.M6`:** the brining comparison keeps the course file's unsalted control. A control salted just before
     roasting would test the timing of the salt; is that the better design?

   **Answer:** the first four as recommended, logged in §5; the chiffonade range is also in the `TS` tolerances. The
   croquette is dropped from `FSH-2`, as in the lesson. The brining control stays unsalted.
4. **Oven figures.** I recommend keeping the oven-table references in `STK-2` and `MEA-2`. A figure would have to be
   checked against a source first, and the learner's own oven table is already the course's method (`EQP-1.M2.X1`).
   **Answer:** as recommended.
5. **Quantities chosen in the lessons.** I recommend leaving these in the lessons, since each course file hands them to
   Phase 6:
   - the `SCI-2` starch, brine and dough ratios;
   - the `STK-2` fix-test quantities;
   - the `SAU-2` sauce ratios (1 yolk to 175 ml of oil; 3 yolks to 200 g of butter);
   - the `POU-2` traybake (about 200 °C, 40–50 minutes);
   - the `MEA-2` price-per-portion example;
   - the `FSH-2` cooking times;
   - the `LEG-2` comparison pots and the *paneer*;
   - the `BAK-2` formulas, oven settings and the milk-free swap;
   - the `BRD-2` formulas (the lean loaf at 68 %, the soft rolls).

   **Answer:** as recommended.
6. **Materials still to make:** the reference images for the AI review of every course, and the five photos of faulty
   cakes for `BAK-2.M4.L3`. I recommend making them while the bakes are tested, before Phase 7.
   **Answer:** as recommended.
7. **The second reader.** The worksheet is still at 0 of 278 items, so all 21 files stay drafts. I recommend reading
   the 48 S1 items and then the 62 S2 items, so both stages can be finalised together.
   **Answer:** as recommended. The worksheet names the reading order; who reads it is still open.
8. **S3 next.** I recommend writing S3 in outline order (`SAF-3`, `FLV-3`, `BUT-3`, `SLW-3`, `FIR-3`, `GRD-3`, `PRS-3`,
   `VEG-3`, `PST-3`, `BRD-3`, `CUI-3`, `KMG-3`, `NUT-3`), one commit per course, and stopping for review at the end of
   S3. It has 107 safety items, the most of any stage.
   **Answer:** as recommended.

### 8.4 Next

The answers are applied. S3 is written in outline order, one commit per course, with a stop for review at the end of
S3 (§9).
