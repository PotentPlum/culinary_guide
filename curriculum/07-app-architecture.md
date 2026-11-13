# Phase 7 — App content architecture

**Status:** built (2026-11-13). The curriculum is in the Mise app as **Opleiding**. The lesson files are still drafts
(Phase 6 §12.2); the app shows that with a "Concept" label (D10 below).

This phase maps the curriculum of Phases 1–6 onto the Android app in this repository: which entities the app knows, how
their IDs carry over, how the content gets into the APK, and how the learner's progress is stored. The screens are
specified in `spec.md` §6.17; this document explains the model behind them.

## 1. The owner's decisions (2026-11-13)

Phase 6 §12.3 named three clashes between the curriculum and the app ([Phase 1 §13.3](01-vision-architecture.md#133-forward-notes-for-phase-7-app-architecture))
and one open question about drafts. The owner decided all four on 2026-11-13, each time choosing the recommended option.
They are `spec.md` §0 D7–D10.

| # | Question | Decision |
|---|---|---|
| D7 | The app is Dutch-only; the curriculum is written in English. | **English lessons, Dutch UI.** Lessons, check items and exercises stay in English. Every label, button, band and message around them is Dutch and lives in `strings.xml`. `spec.md` §15.4 records the exception. |
| D8 | Spec D5 forbids quest checkpoints and success criteria; the app's score scale (2/4/6/8/10) differs from the rubrics. | **A separate section.** Opleiding has its own assessment: knowledge checks with the pass rule of [assessment §4.1](04-assessment.md) and exercise evidence in the four rubric bands. Quests and the journal score are untouched, so D5 still holds. |
| D9 | The app's content scope (Dutch-grown vegetables) is narrower than the curriculum. | **Lessons are exempt.** The scope rule keeps applying to Seizoen, the calendar, quests and suggestions. Lesson text may name fruit, imported ingredients and so on. |
| D10 | Phase 6 is not closed: no second reader has logged the 278 safety items. | **Ship as labelled drafts.** A course with status `draft` shows "Concept" and the reader's count ("0 van 12 gelezen"). Nothing blocks the build. When a lesson file becomes `final`, the next export drops the label. |

## 2. Entities and IDs

The app keeps every curriculum ID exactly as written (README, *Identifiers*). IDs never change and are never reused, so
they are safe keys for stored progress.

| Curriculum entity | ID | In the app | Source |
|---|---|---|---|
| Stage | `S1`–`S5` | `CurriculumStage`: id, English title, Dutch title, question, course ids in order | [`05-course-development.md`](05-course-development.md) §2 |
| Course | `EGG-1` | `CourseSummary` (index) and `CourseFile` (content); kind `COURSE` | `05-courses/`, `06-lessons/` |
| Milestone | `MS-S1`…`MS-S4` | `CourseSummary`, kind `MILESTONE`, rubric only | [`04-assessment.md`](04-assessment.md) |
| Capstone | `CAP-5` | `CourseSummary`, kind `CAPSTONE`, rubric only | [`04-assessment.md`](04-assessment.md) |
| Module | `EGG-1.M2` | `ModuleSummary`, `CourseModule` | course and lesson files |
| Lesson | `EGG-1.M2.L1` | `LessonSummary`, `Lesson` (blocks) | course and lesson files |
| Exercise | `EGG-1.M2.X1` | `ExerciseSummary` (with support level), `Exercise` (blocks) | course and lesson files |
| Knowledge-check item | `EGG-1.M2.Q1` | `CheckItem` (type, safety flag, options, answer, why) | lesson files |
| Competency | `CK-TERM` | `Competency` (id, title); lessons and exercises carry `{id, level}` | `04-competencies/` |
| Rubric row | per milestone | `RubricRow` (criterion, competencies, evidence, weight or *gate*) | [`04-assessment.md`](04-assessment.md) |

Relationships the app uses:

- a stage lists its courses in order; each course belongs to exactly one stage;
- a course has prerequisites (course ids); the app shows them as links and never enforces them;
- a module belongs to its course by ID prefix (`EGG-1.M2` → `EGG-1`), a lesson or exercise to its module the same way;
- a check item names its lesson (`lessonId`), which lies in the item's module (the validator checks this);
- lessons and exercises name competencies with a target level; titles come from the index.

Not in the app yet: course sections (`CUI-3.SEC1`), practice-session logging, and the learner's competency profile
(levels per competency over time). Practice sessions appear only as a count in the lesson summary.

## 3. From Markdown to assets

`tools/export_curriculum.py` reads the Phase 5 course files and the Phase 6 lesson files through the curriculum
validator's own parsers, so the export can't disagree with what the validator checked. It writes:

- `data/assets/curriculum/index.json`: stages, all 50 courses (45 with content, 4 milestones, the capstone), their modules,
  lessons, exercises, check counts, rubrics, status and second-reader counts, and the 67 competency titles;
- `data/assets/curriculum/courses/<ID>.json`: one file per course with content, holding the introduction and each module's
  lessons, exercises and items. Text is a list of blocks (paragraph, list, table) whose strings keep the inline marks
  `**bold**`, `*italic*` and `` `code` ``.

The schemas are `data/schemas/curriculum-index.schema.json` and `curriculum-course.schema.json`. `tools/validate_data.py`
fails when the committed assets differ from a fresh export, so a lesson edit without `--write` is caught in CI. It also
checks unique IDs, one stage per course, resolvable prerequisites and competencies, item placement, and options and
answers that fit each item type.

Multiple-choice options are written inline in the lesson files ("A) … B) …"); the export splits them into `options`.
The other four item types (scenario, calculation, ordering, image identification) keep a model answer. Image items have
no images yet (Phase 6 §12.2, materials); the app shows their question and model answer.

## 4. Progress tracking

Progress lives in three Room tables (database v2, `MIGRATION_1_2`, `spec.md` §7.2). Each row points at a curriculum ID.

| Table | One row per | Fields |
|---|---|---|
| `curriculum_lesson` | lesson marked done | `lessonId`, `completedAt` |
| `curriculum_check` | sitting of a knowledge check | `moduleId`, `takenAt`, totals, safety totals, `passed`, `wrongItemIds` |
| `curriculum_evidence` | piece of exercise evidence | `exerciseId`, `loggedAt`, `band`, `notes?`, `photoPath?` |

Rules (`app/.../domain/CurriculumProgress.kt`):

- **Step.** A module's steps are its lessons, its exercises and, when it has items, its knowledge check. Milestones and the
  capstone have no steps in the app.
- **Lesson done** when the learner marks it so (and undone when they unmark it).
- **Check passed** when one sitting reaches 80 % and every safety item is right ([assessment §4.1](04-assessment.md)).
  The percent rounds down, so 79.9 % never passes. Multiple choice is marked by the app; the other types are marked by the
  learner against the model answer, as a self-check. Every sitting is kept.
- **Exercise done** when evidence at *Competent* (Bekwaam) or above is logged. The band is the learner's own judgement,
  in the rubric's four bands.
- **Next step**: stage order, then course order within the stage, then module order; within a module the lessons, then the
  exercises, then the check.

**Content versions.** The README promised that a materially changed entity keeps its ID and gets a new content version.
The app stores no content version yet: progress refers to IDs only, so an edited lesson keeps its "done". If a change ever
needs the learner to redo something, the plan is a `contentVersion` field in the index and on the progress rows, added
with a numbered migration. A retired entity drops out of the export; rows that point at it stay in the database and in
backups, and no longer count.

## 5. Backup

Backups (`spec.md` §13) carry `lessonProgress[]`, `checkAttempts[]` and `evidence[]`, with evidence photos as
`evidence/<id>.jpg`. The lists are optional: older backups restore without them, and an older app ignores them, so the
backup format stays at schemaVersion 1. Merge adds rows by id and never overwrites; Replace wipes the curriculum tables too.

## 6. What remains

- Phase 6 §12.2 in full: the second reader, the fixes, the owner's review of the S3–S5 answers, the materials, and setting
  each file to `final`. Each step only needs a new export (`python3 tools/export_curriculum.py --write`); the app picks up
  the status and the reader's counts from the index.
- Later app work, not needed for this phase: a competency profile built from the evidence, practice-session logging,
  course sections, milestone and capstone evidence, and reference images for image items.
