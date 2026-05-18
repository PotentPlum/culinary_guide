# Batch Plan: Supabase + Claude API Integration Foundation

## Research Summary

- **Framework**: Next.js 16 App Router, TypeScript strict mode, Tailwind v4
- **Existing API route**: `web/app/api/recipe/route.ts` (POST, ingests recipe from URL) — uses `NextRequest`, `Response.json()`
- **Existing types**: `Recipe` and `VegItem` exported from `web/lib/vegetables.ts` — the new Supabase `Recipe` type must be named `SavedRecipe` to avoid conflicts
- **No existing** `.env.local.example`; no env vars in the project today
- **Test command**: `npm run validate && npm run build` (validate runs first in build)
- **Branch**: `claude/supabase-claude-integration-zr7kN` (current dev branch per session config)
- **Target branch for task**: `claude/recipe-database-setup-ll80K`
- **Package manager**: npm; `@supabase/supabase-js` not yet installed

## Naming Note
`Recipe` already exists in `web/lib/vegetables.ts`. The Supabase-persisted recipe type will be named **`SavedRecipe`** in `web/lib/types.ts` to avoid import collisions.

## Work Units

1. **Infrastructure** — `web/lib/supabase.ts`, `web/lib/types.ts`, `.env.local.example`, `package.json` (install dep)
   - Install `@supabase/supabase-js`; create Supabase singleton client; define `SavedRecipe` and `Challenge` types; create env example file

2. **API Routes** — `web/app/api/recipes/save/route.ts`, `web/app/api/recipes/list/route.ts`, `web/app/api/challenge/route.ts`
   - Stub POST `/api/recipes/save` (save to Supabase), GET `/api/recipes/list` (fetch all), POST `/api/challenge` (call Claude API with ingredient)

3. **Stub Components** — `web/components/RecipeSaver.tsx`, `web/components/ChallengeModePanel.tsx`
   - Minimal React client components; no wiring yet; TypeScript-valid stubs with clear TODO comments

4. **Documentation** — `web/SUPABASE_IMPLEMENTATION.md`
   - SQL DDL for `recipes` and `challenges` tables; step-by-step guide covering env setup, wiring persistence, displaying saved recipes, challenge mode, and testing checklist

## E2E Test Recipe

Each worker should:
1. `cd web && npm install` (to pick up any newly added deps in package.json)
2. `npm run validate` — verify vegetable data still passes
3. `npm run build` — verify TypeScript compiles and Next.js builds

If build fails, fix TypeScript errors before committing.

> **Note**: `@supabase/supabase-js` must be in `package.json` for any file importing from it to compile. Units 2 and 3 do NOT import from supabase directly (they import from `@/lib/supabase` which they create or leave as a stub import), so they can still build if unit 1 has not landed yet — **only if** they add `@supabase/supabase-js` to package.json themselves or avoid the import.
> **Simplest approach**: Each worker that imports supabase should also install the package in their worktree.

## Worker Template (copied verbatim into each agent prompt)

```
After you finish implementing the change:
1. Simplify — Invoke the Skill tool with skill: "simplify" to review and clean up your changes.
2. Run unit tests — Run the project's test suite: cd web && npm test
3. Test end-to-end — cd web && npm install && npm run build. Fix any TypeScript errors before committing.
4. Commit and push — Commit all changes with a clear message, push the branch, and create a PR.
5. Report — End with a single line: PR: <url>
```
