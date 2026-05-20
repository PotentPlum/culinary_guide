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
| `POST /api/recipes/save` | **Stub** | Needs Supabase `recipes` insert wired |
| `GET /api/recipes/list` | **Stub** | Needs Supabase `recipes` SELECT wired |
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
| `RecipesPage` | `components/RecipesPage.tsx` | — | Recipe URL ingestion UI; localStorage persistence; Supabase save pending |
| `RecipeSaver` | `components/RecipeSaver.tsx` | `recipe, onSaved?` | **Stub** — Supabase save button; not yet wired |
| `ChallengeModePanel` | `components/ChallengeModePanel.tsx` | `ingredients` | **Stub** — Claude challenge generator UI; not yet wired |

## Known intentional gaps
- `density` state exists in `App` but has no toggle UI — fixed at `"comfortable"`, passed through. Adding the toggle is a future task, not a bug.
- `VegItem.shoulder` is optional; not all vegetables have shoulder months.
- Month detection uses local client timezone (`new Date().getMonth()`). Intentional — NL-only app.
- `RecipeSaver` and `ChallengeModePanel` are UI stubs pending API wiring.
- Some Dutch strings in `page.tsx` hero section are still hardcoded; they should be migrated to `lib/strings.ts`.

## Future modules (not yet built)
Vervangers, Technieken, Plukken — ghost nav items in `NAV_ITEMS` in `page.tsx`.
