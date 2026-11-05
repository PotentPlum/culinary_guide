# Phase 6 — Lesson development

Status: **S1 in progress** (2026-11-05). Phase 5 is complete ([its overview](05-course-development.md)). The second
reader's worksheet is open ([`06-safety-review.md`](06-safety-review.md)). This phase runs in parallel with it (S5
review answer 4): lesson files are drafts until the reader's log is in (§3). The pilot (`ORI-1` and `SAF-1`, §6) was
reviewed on 2026-11-05, and its answers are applied (§2, §5, §6.3).

**Courses developed:** ORI-1, SAF-1, WRK-1, EQP-1

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

