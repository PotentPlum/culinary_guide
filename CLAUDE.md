# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack
- Next.js 16 (App Router) + TypeScript, deployed to Vercel
- All work lives in `web/` — run all commands from there
- Backend: Next.js API routes in `web/app/api/`
- Persistence: Supabase (postgres + RLS); client singleton in `web/lib/supabase.ts`

## Commands (run from `web/`)

```bash
npm run dev          # dev server
npm run build        # validate + next build
npm run validate     # check VegItem data integrity only
npm test             # vitest (all lib/*.test.ts)
npm run test:watch   # vitest watch
npm run test:coverage
npx vitest run lib/recipeIngestion.test.ts  # single test file
npm run lint
```

## Environment variables
Copy `web/.env.local.example` to `web/.env.local`:
```
NEXT_PUBLIC_SUPABASE_URL=...      # browser-safe
NEXT_PUBLIC_SUPABASE_ANON_KEY=... # browser-safe, RLS-enforced
CLAUDE_API_KEY=...                # server-only; never import in client components
NEXT_PUBLIC_API_SECRET=...        # optional; forwarded by apiClient.ts as x-api-secret header
API_SECRET=...                    # optional; server-side guard in requireSecret()
```

## Language: Dutch only
All user-visible text must be in Dutch. Code, variable names, comments, and filenames stay in English.
- **Never hardcode Dutch strings in components.** Add to `web/lib/strings.ts` under the appropriate key, import `UI` from there.
- `VegItem.name` = internal English slug, **never rendered**. Display `VegItem.nl` instead.
- Every label, heading, placeholder, aria-label, tooltip, and button text must be Dutch.

## Design system
"Mise — een kooksalmanak". Editorial cookbook aesthetic.
- **No Tailwind utility classes for layout.** Tailwind is installed but intentionally unused; use CSS class names from `web/app/globals.css` only.
- CSS variables: `--primary`, `--surface`, `--surface-card`, `--ink`, `--ink-soft`, `--accent`
- Font variables: `--serif` (headings/emphasis), `--sans` (body/UI), `--mono` (meta labels)
- Brand colors: cream `#F4EFE6`, leaf-green `#3F5E2A`, terracotta `#B95128`
- New components → `components/`; new pages → `app/`; new lib utilities → `lib/`

## Data scope
Dutch locally grown vegetables only. Seasonality = field/greenhouse harvest windows per velt.nu, not supermarket imports. Do NOT add imported year-round produce (avocado, citrus, mango), fruits, berries, or nuts.

## Key library files

| File | Purpose |
|------|---------|
| `web/lib/vegetables.ts` | `VegItem` type + `VEG_DATA[]` (67 entries), `statusFor(veg, month)`, `MONTHS`/`MONTHS_LONG` |
| `web/lib/strings.ts` | All Dutch UI strings — single source of truth |
| `web/lib/types.ts` | `SavedRecipe`, `Challenge`, `ChallengeResponse`, `ChallengeParams` types for Supabase rows |
| `web/lib/facets.ts` | Recipe tagging schema: `FacetKey`, `FACETS[]`, `RecipeTags`, `getCuisineColor()` |
| `web/lib/techniques.ts` | Cooking techniques data + types; tested in `techniques.test.ts` |
| `web/lib/supabase.ts` | Browser-side Supabase client singleton; throws at startup if env vars missing |
| `web/lib/supabase-server.ts` | Server-side Supabase client (SSR cookie-based); use in API routes + Server Components |
| `web/lib/auth.ts` | `requireSession()` (session guard for API routes) and `requireSecret()` (optional API secret guard) |
| `web/lib/apiClient.ts` | `apiHeaders()` — injects `x-api-secret` header for all fetch calls to internal API routes |
| `web/lib/recipeIngestion.ts` | Schema.org JSON-LD extractor; unit-tested |
| `web/data/vegetables.json` | Original Dutch source — reference only, not imported |
| `web/scripts/validate-data.ts` | Validates all VegItem entries; runs before every build |

## VegItem validation
`npm run validate` checks all 67 entries. A failing validate blocks build. Required fields: `id` (kebab-case), `nl`, `color` (hex), `accent` (hex), `family`, `peak` (months 1–12), `blurb`, `storage`, `prep`, `pairings` (≥2), `substitutions` (≥1). `shoulder` is optional.

Allowed `family` values: `"Wortel"`, `"Blad"`, `"Brassica"`, `"Allium"`, `"Peulvrucht"`, `"Stengel"`, `"Pompoen"`, `"Knolgewas"`, `"Vruchtgroente"`, `"Paddenstoel"`, `"Kruid"`

## App architecture

`app/page.tsx` is the single-page shell (`App` component). It owns all top-level state — current page, filter, search query, view, density, open vegetable — and renders the active page component. Navigation is driven by `NAV_ITEMS`; pages with `ready: false` are ghost nav items (`substitutions`, `foraging`).

Month is always derived from `new Date().getMonth() + 1` on the client (intentional — NL-only app).

### Persistence: dual-write pattern
`RecipesPage` maintains recipes in both localStorage (immediate, offline) and Supabase (sync). On mount it fetches from Supabase and merges with localStorage, populating `remoteId` on matched entries. Saves write to localStorage first, then POST to Supabase; edits PATCH via `/api/recipes/[id]` when `remoteId` is set.

### Recipe tagging
Recipes carry `tags: RecipeTags` (a `Partial<Record<FacetKey, string[]>>`). Facet definitions live in `web/lib/facets.ts`. The Claude API is used both for challenge generation (`/api/challenge`) and for auto-tagging recipes from photos (`/api/recipe-from-images`).

### Recipe data model note
There are **two** `SavedRecipe` types:
1. `lib/types.ts` — Supabase row shape (exported, used by API routes)
2. Local type inside `RecipesPage.tsx` — adds `url` (technical key), `recipe: RecipeOutput`, `savedAt: number`, `remoteId?: string`

### Authentication
The app is fully gated behind Supabase Auth. `web/middleware.ts` redirects unauthenticated requests to `/login`; unauthenticated API calls get a 401. The login page is at `app/login/page.tsx`.

- **Browser:** use `supabase` from `lib/supabase.ts` (`supabase.auth.signInWithPassword`, etc.)
- **API routes:** call `requireSession()` from `lib/auth.ts` — returns `{ client, error }`. If `error` is non-null, return it immediately.
- **Server Components:** use `createSupabaseServerClient()` from `lib/supabase-server.ts`.
- All `fetch()` calls from client → internal API routes should use `apiHeaders()` from `lib/apiClient.ts`.

## API routes (`web/app/api/`)

| Route | Notes |
|-------|-------|
| `POST /api/recipe` | Fetches URL, extracts Schema.org recipe data |
| `POST /api/recipe-from-images` | Claude vision — up to 5 photos, normalises to metric |
| `POST /api/recipes/save` | Supabase insert: `name, ingredients[], instructions, source_url, source, tags` |
| `GET /api/recipes/list` | Supabase SELECT |
| `PATCH /api/recipes/[id]` | Supabase update by id |
| `POST /api/challenge` | Calls Claude API, inserts into `challenges` table |

## Supabase schema
Migrations in `supabase/migrations/`. Key columns added after initial schema:
- `recipes.source TEXT` — human-readable source (domain or cookbook title)
- `recipes.tags JSONB` — facet tags (`{}` default)

## Known intentional gaps
- `density` state in `App` is wired through but fixed at `"comfortable"` — no toggle UI yet.
- Some Dutch strings in the `page.tsx` hero section are still hardcoded; should migrate to `lib/strings.ts`.
- `RecipeSaver` and `ChallengeModePanel` are legacy stubs — superseded by `RecipesPage` and `ChallengePage`.

## Next.js version note
This uses Next.js 16 — APIs and conventions may differ from training data. Check `node_modules/next/dist/docs/` before writing framework-specific code.
