# Culinary Guide — Project Instructions

## Stack
- Next.js 16 (App Router) + TypeScript, deployed to Vercel
- Backend: Next.js API routes in `web/app/api/` (recipe ingestion, Supabase persistence, Claude challenge generation)
- All work lives in `web/`

## Data scope
Locally grown vegetables in the Netherlands only. Seasonality = Dutch field/greenhouse harvest windows (not supermarket imports). Source: velt.nu.
Do NOT add: imported year-round produce (avocado, citrus, mango), fruits, berries, or nuts.

## Language
All user-visible text must be in **Dutch**. Code, variable names, comments, and filenames stay in English.
- Source of truth for all Dutch UI strings: `web/lib/strings.ts` — import from there, do not hardcode
- `VegItem.name` = internal English identifier, **never rendered**; display `VegItem.nl` instead
- See `web/AGENTS.md` for the full language ruleset

## Design system
"Mise — een kooksalmanak". Editorial cookbook aesthetic:
- Fonts: Instrument Serif italic + Instrument Sans body
- Colors: cream `#F4EFE6`, leaf-green `#3F5E2A`, terracotta `#B95128` accent
- Layout via plain CSS classes in `web/app/globals.css` — no Tailwind utilities for layout
- Tailwind is installed but intentionally unused for layout; do not introduce Tailwind utility classes

## Key files
| File | Purpose |
|------|---------|
| `web/lib/vegetables.ts` | `VegItem` type, `VEG_DATA[]` (67 entries), `statusFor(veg, month)`, `MONTHS`/`MONTHS_LONG` |
| `web/lib/strings.ts` | All Dutch UI strings; add new labels here, never hardcode in components |
| `web/lib/types.ts` | `SavedRecipe` and `Challenge` types for Supabase rows |
| `web/lib/supabase.ts` | Supabase client singleton; throws at startup if env vars missing |
| `web/lib/recipeIngestion.ts` | Schema.org JSON-LD extractor; unit-tested in `recipeIngestion.test.ts` |
| `web/data/vegetables.json` | Original Dutch source data — reference only, not imported |
| `ToDo` | Quick notes on upcoming features (repo root) |

## Environment variables
Copy `web/.env.local.example` to `web/.env.local`:
```
NEXT_PUBLIC_SUPABASE_URL=...      # browser-safe; used by supabase.ts
NEXT_PUBLIC_SUPABASE_ANON_KEY=... # browser-safe; RLS-enforced
CLAUDE_API_KEY=...                # server-only; used by /api/challenge
```

## API routes
All in `web/app/api/`:
| Route | Status | Notes |
|-------|--------|-------|
| `POST /api/recipe` | Done | Fetches URL, extracts Schema.org recipe data |
| `POST /api/recipe-from-images` | Done | Claude vision — reads recipe text from up to 5 photos, normalises to metric |
| `POST /api/recipes/save` | Done | Supabase `recipes` insert; accepts `name, ingredients[], instructions, source_url, source` |
| `GET /api/recipes/list` | Done | Supabase `recipes` SELECT; returns `id, name, instructions, source_url, source, created_at` |
| `PATCH /api/recipes/[id]` | Done | Supabase `recipes` update by id; accepts `name, ingredients[], instructions, source` |
| `POST /api/challenge` | Done | Calls Claude API and inserts into Supabase `challenges` |

## Validation
- `npm run validate` — checks all 67 VegItem entries for hex colors, valid months (1–12), required fields, allowed family values
- `npm run build` runs validate first; a failing validate blocks the build
- Allowed `family` values: `"Wortel"`, `"Blad"`, `"Brassica"`, `"Allium"`, `"Peulvrucht"`, `"Stengel"`, `"Pompoen"`, `"Knolgewas"`, `"Vruchtgroente"`, `"Paddenstoel"`, `"Kruid"`

## Testing
- `npm test` / `npm run test:watch` — vitest; tests live in `web/lib/*.test.ts`
- `npm run test:coverage` — v8 coverage report

## Component contracts

| Component | File | Props summary | Responsibility |
|-----------|------|---------------|----------------|
| `App` | `app/page.tsx` | — (root) | App shell; all state (page, filter, query, view, density, openVeg); month from `new Date()` |
| `VegCard` | `components/VegCard.tsx` | `veg, month, density?, onOpen, listMode?` | Grid card or list row; calls `onOpen(veg)` on click |
| `DetailPanel` | `components/DetailPanel.tsx` | `veg\|null, month, onClose` | Slide-in drawer; ESC closes; blocks body scroll while open |
| `FilterPills` | `components/FilterPills.tsx` | `value, onChange, counts` | Tab-style season filter; exports `Filter` and `Counts` types |
| `CalendarPage` | `components/CalendarPage.tsx` | `month, onOpen` | Full-year 12-month calendar heatmap |
| `PeakBar` | `components/PeakBar.tsx` | `veg, month, compact?` | 12-month peak/shoulder availability strip with tooltips |
| `StatusDot` | `components/StatusDot.tsx` | `status, withLabel?` | Colored dot + optional Dutch status label |
| `VegToken` | `components/VegToken.tsx` | `veg, size` | Geometric SVG circle illustration placeholder |
| `NavIcon` | `components/NavIcon.tsx` | `name` | Monoline SVG icon switcher (6 icons: leaf, cal, book, swap, knife, compass) |
| `FuturePeek` | `components/FuturePeek.tsx` | — | Teaser cards for upcoming modules (static, strings from `lib/strings.ts`) |
| `RecipesPage` | `components/RecipesPage.tsx` | — | Full recipe library: card grid, search/sort, add via URL or photos, edit, ingredient checklist, serving scaler; localStorage + Supabase dual persistence |
| `TechniquesPage` | `components/TechniquesPage.tsx` | — | Cooking techniques browser with search, category filter, and detail panel |
| `TechniqueCard` | `components/TechniqueCard.tsx` | technique props | Card for a single technique in the grid |
| `TechniqueDetail` | `components/TechniqueDetail.tsx` | technique props | Detail panel for a technique |
| `ChallengePage` | `components/ChallengePage.tsx` | — | Claude-powered cooking challenge generator; takes an ingredient, returns a targeted exercise |
| `HomePage` | `components/HomePage.tsx` | — | Landing page with seasonal spotlight, module cards, and tip section |
| `RecipeSaver` | `components/RecipeSaver.tsx` | `recipe, onSaved?` | **Stub** — legacy; superseded by save logic in `RecipesPage` |
| `ChallengeModePanel` | `components/ChallengeModePanel.tsx` | `ingredients` | **Stub** — legacy; superseded by `ChallengePage` |

## RecipesPage data model
Local `SavedRecipe` type (in `RecipesPage.tsx`, not the same as the exported type in `lib/types.ts`):
```ts
type SavedRecipe = {
  url: string;        // technical key — real URL or "mise-photo:timestamp"
  source: string;     // human-readable: domain name, cookbook title, etc.
  recipe: RecipeOutput;
  savedAt: number;
  remoteId?: string;  // Supabase row id, populated after list fetch
};
```
Supabase `recipes` table columns: `id, name, ingredients[] (text[]), instructions (text/JSON string), source_url (text), source (text), created_at`.
The `source` column was added via migration: `ALTER TABLE recipes ADD COLUMN IF NOT EXISTS source TEXT DEFAULT ''`.

## RecipesPage features (as of branch claude/recepten-page-library-redesign)
- Library-first layout: recipe cards are the primary focus; add panel is a secondary FAB/drawer
- Add panel: URL tab (auto-derives source from domain) + Photo tab (up to 5 images, Claude vision)
- Editable source field (cookbook name or URL) on both tabs
- Sorting: newest first / oldest first / A–Z
- Search with inline × clear and result count
- Card accent colour derived from cuisine via `CUISINE_COLORS` map
- Recipe editing modal (`EditRecipePanel`): title, description, ingredients (1/line), instructions (1/step/line), source; saves to localStorage + PATCH `/api/recipes/[id]` when `remoteId` is set
- Ingredient checklist: checkboxes with strikethrough, state resets on back navigation
- Serving size scaler: `−`/`+` controls, scales numeric quantities in ingredient text (handles unicode fractions)

## Known intentional gaps
- `density` state exists in `App` but has no toggle UI — fixed at `"comfortable"`, passed through. Adding the toggle is a future task, not a bug.
- `VegItem.shoulder` is optional; not all vegetables have shoulder months.
- Month detection uses local client timezone (`new Date().getMonth()`). Intentional — NL-only app.
- Some Dutch strings in `page.tsx` hero section are still hardcoded; they should be migrated to `lib/strings.ts`.
- `RecipeSaver` and `ChallengeModePanel` are legacy stubs; their functionality lives in `RecipesPage` and `ChallengePage` respectively.

## Future modules (not yet built)
Vervangers, Plukken — ghost nav items in `NAV_ITEMS` in `page.tsx`.
Technieken is built (`TechniquesPage`) but may not be fully wired into the nav yet.
