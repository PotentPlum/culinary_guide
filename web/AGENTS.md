<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

<!-- BEGIN:language-rules -->
# Language: Dutch only

All user-visible content in this web app must be in Dutch. This is a hard requirement.

- Every label, heading, placeholder, aria-label, tooltip, button text, status message, and data field rendered to the user must be in Dutch.
- Vegetable data in `web/lib/vegetables.ts` — including `blurb`, `storage`, `prep`, `pairings`, `substitutions`, `technique`, `family`, and recipe `why` fields — must be written in Dutch.
- Month names (`MONTHS`, `MONTHS_LONG`) must use Dutch abbreviations and full names.
- The `name` field on `VegItem` is the internal English identifier used for search matching and sorting only — it is never rendered to users. Display only the `nl` field.
- Code, variable names, comments, filenames, and anything not visible to website visitors may remain in English.
<!-- END:language-rules -->

<!-- BEGIN:strings-rules -->
# Strings: always import from lib/strings.ts

Never hardcode a Dutch string in a component. Add new strings to `lib/strings.ts` under the appropriate key, then import `UI` from there.

```ts
import { UI } from "@/lib/strings";
// ✓ correct
<button>{UI.controls.searchAriaLabel}</button>
// ✗ wrong
<button>Groenten zoeken</button>
```
<!-- END:strings-rules -->

<!-- BEGIN:design-rules -->
# Design system

- Use CSS class names from `app/globals.css` only — do NOT introduce Tailwind utility classes.
- CSS variables for colors: `--primary`, `--surface`, `--surface-card`, `--ink`, `--ink-soft`, `--accent`.
- Fonts via CSS variables: `--serif` (headings/emphasis), `--sans` (body/UI), `--mono` (meta labels).
- New components go in `components/`; new pages go in `app/`; new lib utilities go in `lib/`.
<!-- END:design-rules -->

<!-- BEGIN:data-rules -->
# Data: vegetables

- All 67 VegItem entries live in `lib/vegetables.ts`. Run `npm run validate` after any change.
- Never add a vegetable without: `id` (kebab-case), `nl` (Dutch name), `color`, `accent` (hex), `family`, `peak` (months 1–12), `blurb`, `storage`, `prep`, `pairings` (≥2), `substitutions` (≥1).
- `VegItem.name` is the English internal slug — never displayed, used for search/sort only.
- Seasonal data source: velt.nu. Do not use supermarket availability windows.
<!-- END:data-rules -->

<!-- BEGIN:api-rules -->
# API routes

- Server-only secrets (`CLAUDE_API_KEY`) must never be referenced in client components or files that import into them.
- `NEXT_PUBLIC_*` env vars are browser-safe; all others are server-only.
- All recipe routes are complete: `/api/recipe` (URL ingestion), `/api/recipe-from-images` (photo/Claude vision), `/api/recipes/save` (Supabase insert), `/api/recipes/list` (Supabase SELECT), `/api/recipes/[id]` (PATCH update).
- Challenge generation (`/api/challenge`) is complete.
<!-- END:api-rules -->
