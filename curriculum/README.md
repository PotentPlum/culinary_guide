# Curriculum — a competency-driven culinary program for the home learner

This folder holds the design of a structured culinary education program. The aim is for a dedicated adult, cooking at
home 5–8 hours a week, to reach the knowledge, skill, judgment and confidence of a capable, professionally trained cook
within the program's scope. The benchmark is professional culinary education, in particular the Culinary Institute of
America [S1].

> **No qualification.** The program gives no diploma, accreditation or professional certification. It is benchmarked
> against culinary schools, but it is not equivalent to them and does not stand in for their credentials.

The design is **independent of any app**. A later Android app, possibly Mise in this repository, is only one way to
deliver it. Phases 1–6 design the education; Phase 7 maps it onto an app content model. Until then, nothing here
changes `spec.md` or `data/`.

## Phases

The program is built in gated phases. Each phase is reviewed before the next one starts, and later phases never
silently change an agreed structure: changes are explained and logged in the phase document that makes them.

| Phase | Deliverable | File | Status |
|---|---|---|---|
| 1 | Educational vision and curriculum architecture | [`01-vision-architecture.md`](01-vision-architecture.md) | ✅ Reviewed; answers applied |
| 2 | Complete curriculum outline (courses → modules → lessons, objectives, prerequisites, hours) | [`02-curriculum-outline.md`](02-curriculum-outline.md) and [`02-outline/`](02-outline/) | ✅ Reviewed; revised in Phase 3 |
| 3 | Curriculum validation and optimization (benchmarks, realistic hours, barbecue added) | [`03-validation.md`](03-validation.md) | ✅ Reviewed; answers applied |
| 4 | Full competency framework and assessment design (milestones, capstone rubrics) | [`04-competency-framework.md`](04-competency-framework.md) and [`04-assessment.md`](04-assessment.md) | ✅ Reviewed; answers applied |
| 5 | Detailed course development (stage by stage) | [`05-course-development.md`](05-course-development.md) and [`05-courses/`](05-courses/) | ✅ Reviewed; answers applied (S1–S5) |
| 6 | Detailed lesson development (safety review, then course by course) | [`06-lesson-development.md`](06-lesson-development.md), [`06-safety-review.md`](06-safety-review.md) and `06-lessons/` | 🔄 S2 lessons ready for review; safety review open |
| 7 | App content architecture (entities, IDs, relationships, progress tracking) | — | Pending |

Sources are numbered `[S#]` across all phases and listed in [`sources.md`](sources.md).

## Conventions

**Language.** The documents are in English. Learner-facing terms carry their Dutch (NL) and, where the kitchen
uses it, French (FR) name, because the learner will meet those words in shops, cookbooks and on menus.
Example: *sauté* (FR) / *sauteren* (NL).

**Four dimensions of learning.** Every objective, competency and assessment is tagged with one or more of:

| Code | Dimension | Question it answers | Example |
|---|---|---|---|
| **K** | Knowledge | What does the learner understand? | Explains why a dry, hot surface browns and a wet one steams. |
| **S** | Skill | What can the learner physically do? | Sears a steak evenly without burning the fond. |
| **J** | Judgment | What can the learner decide alone? | Lowers the heat or changes method when the pan or the cut calls for it. |
| **M** | Mastery | What can the learner do consistently and adapt? | Gets a good sear on different cuts, pans and stoves, time after time. |

**Proficiency scale (1–5)**, used for every competency (defined per dimension in Phase 1 §5.2):

| Level | Name | Expected ability |
|---|---|---|
| 1 | Awareness | Recognises and understands the concept. |
| 2 | Guided execution | Performs the task with instructions. |
| 3 | Independent execution | Performs the task without instructions, with acceptable results. |
| 4 | Adaptation | Adapts the technique to new ingredients, equipment or constraints, and explains why. |
| 5 | Mastery | Performs consistently, diagnoses and optimises the result, and can teach it. |

**Identifiers.** IDs are stable, have no meaning tied to position, and are never reused:

| Entity | Pattern | Example | Notes |
|---|---|---|---|
| Learning stage | `S1`–`S5` | `S2` | The only ordinal IDs. Stages are the fixed backbone. |
| Course | `AAA-n` (subject code + generation) | `SAU-2`, `BRD-3` | The number tells consecutive courses in one subject apart. It is **not** the stage. Reordering keeps the ID. |
| Competency | `DD-XXXX` (domain + 3–4 letter mnemonic) | `TS-KNF`, `FL-SEAS` | Ten domains (Phase 1 §5.1). |
| Milestone assessment | `MS-Sn` | `MS-S3` | One per stage. |
| Capstone | `CAP-5` | | |
| Module, lesson, activity | defined in Phase 2 | `SAU-2.M3`, `SAU-2.M3.L2` | Order is a separate field, so inserting a module doesn't renumber the others. |
| Course section | defined in Phase 3 | `CUI-3.SEC1` | Optional; groups consecutive modules of a large course into units of progress. |

A retired entity keeps its ID, marked *retired*, so learner progress that refers to it stays valid. A materially changed
entity keeps its ID and gets a new content version (detailed in Phase 7).
