# Audit Fixes — Agent Task File

This file is structured for an AI agent. Work through each task in order. Each task is
self-contained: it names the exact file(s), the exact change required, and the acceptance
criterion. Mark a task done by checking its box. Do not skip tasks; do not make changes
beyond what is specified.

---

## TASK 1 — CRITICAL: Block private/loopback IPs in recipe fetch (SSRF)

**File:** `web/app/api/recipe/route.ts`

**What to change:**

After the block that parses `url` into `parsed` (the `new URL(url)` try/catch, lines 36–41),
insert a private-IP hostname guard and change the redirect mode to `"error"`.

1. Add this constant near the top of the file (after the `BROWSER_HEADERS` block):

```ts
const BLOCKED_HOST_PATTERNS = [
  /^localhost$/i,
  /^127\./,
  /^10\./,
  /^192\.168\./,
  /^172\.(1[6-9]|2\d|3[01])\./,
  /^169\.254\./,
  /^::1$/,
  /^fc/i,
  /^fd/i,
];
```

2. Immediately after `parsed` is successfully constructed, add:

```ts
if (BLOCKED_HOST_PATTERNS.some((p) => p.test(parsed.hostname))) {
  return Response.json(
    { error: "URL niet toegestaan." },
    { status: 400 },
  );
}
```

3. In the `fetch(url, { ... })` call, change `redirect: "follow"` to `redirect: "error"`.

**Acceptance:** The file compiles (`npm run build` from `web/`). A request with
`url = "http://127.0.0.1/"` hits the new guard and returns 400 before any network call.

---

## TASK 2 — CRITICAL: Add shared-secret auth to all API routes

**Context:** All Supabase tables have `anon full access` RLS policies, so the only
meaningful guard is at the API route layer.

**New env var:** `API_SECRET` — server-only (no `NEXT_PUBLIC_` prefix). Add it to
`web/.env.local.example` as `API_SECRET=change-me`.

**Files to change:**

- `web/app/api/recipe/route.ts`
- `web/app/api/recipe-from-images/route.ts`
- `web/app/api/recipes/save/route.ts`
- `web/app/api/recipes/list/route.ts`
- `web/app/api/recipes/[id]/route.ts`
- `web/app/api/challenge/route.ts`

**What to change in each file:**

Add a helper function (copy into each file, or extract to `web/lib/auth.ts` and import):

```ts
function requireSecret(req: NextRequest): Response | null {
  const secret = process.env.API_SECRET;
  if (!secret) return null; // not configured — open in dev
  if (req.headers.get("x-api-secret") !== secret) {
    return Response.json({ error: "Niet geautoriseerd." }, { status: 401 });
  }
  return null;
}
```

At the very top of every exported handler function (before any body parsing), add:

```ts
const authError = requireSecret(req);
if (authError) return authError;
```

**Client-side:** All `fetch("/api/...")` calls in `web/components/RecipesPage.tsx` and
`web/components/ChallengePage.tsx` must send the header. Add a helper in
`web/lib/apiClient.ts`:

```ts
export function apiHeaders(extra?: HeadersInit): HeadersInit {
  const secret = process.env.NEXT_PUBLIC_API_SECRET ?? "";
  return { "x-api-secret": secret, ...extra };
}
```

Then add `NEXT_PUBLIC_API_SECRET` to `web/.env.local.example`. Update every `fetch`
call that hits `/api/` in those components to use `apiHeaders({"Content-Type": "application/json"})`.

**Note:** `NEXT_PUBLIC_API_SECRET` is visible in the client bundle, which is acceptable
for a single-user app (security through obscurity plus the secret is user-controlled).
This is explicitly the minimal fix; the TODO comment for Supabase Auth can be added as
a code comment.

**Acceptance:** `npm run build` passes. All routes return 401 when the header is absent
and `API_SECRET` is set.

---

## TASK 3 — HIGH: `handleSave` must capture `remoteId` after first save

**File:** `web/components/RecipesPage.tsx`

**Problem:** `saveRemote` returns `Promise<void>` and discards the Supabase row id.
After saving, the in-memory item has no `remoteId`, so subsequent edits within the same
session never PATCH Supabase.

**What to change:**

1. Change `saveRemote` signature from `Promise<void>` to `Promise<string>` and return
   the id from the API response:

```ts
async function saveRemote(item: SavedRecipe): Promise<string> {
  // ... existing fetch ...
  if (!res.ok) {
    const json = (await res.json()) as { error?: string };
    throw new Error(json.error ?? "Opslaan mislukt");
  }
  const json = (await res.json()) as { id: string };
  return json.id;
}
```

2. In `handleSave`, after `await saveRemote(item)` resolves, patch the `remoteId` into
   both state and localStorage:

```ts
async function handleSave() {
  if (!recipe || !activeUrl) return;
  setSaveError("");
  const item: SavedRecipe = {
    url: activeUrl,
    source: pendingSource.trim() || deriveDomain(activeUrl),
    recipe,
    savedAt: Date.now(),
    tags: pendingTags,
  };
  const updated = [item, ...savedRecipes.filter((r) => r.url !== activeUrl)];
  setSavedRecipes(updated);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  try {
    const remoteId = await saveRemote(item);
    const withId = updated.map((r) =>
      r.url === item.url ? { ...r, remoteId } : r,
    );
    setSavedRecipes(withId);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(withId));
  } catch (err) {
    setSaveError(err instanceof Error ? err.message : "Opslaan mislukt");
  }
}
```

**Acceptance:** After saving a recipe and immediately clicking edit → save, the browser
DevTools Network tab shows a PATCH request to `/api/recipes/<uuid>`.

---

## TASK 4 — HIGH: Fix remote-wins merge on page load (offline edits discarded)

**File:** `web/components/RecipesPage.tsx`

**Problem:** The mount `useEffect` replaces all local state with the remote list when
`remote.length > 0`, discarding any locally-saved recipe that hasn't synced yet.

**What to change:**

Replace the `fetchRemoteSaved().then(...)` callback inside the mount effect:

```ts
// BEFORE:
fetchRemoteSaved().then((remote) => {
  if (remote.length > 0) setSavedRecipes(remote);
});

// AFTER:
fetchRemoteSaved().then((remote) => {
  if (remote.length === 0) return;
  setSavedRecipes((prev) => {
    const remoteByUrl = new Map(remote.map((r) => [r.url, r]));
    const localOnly = prev.filter((r) => !remoteByUrl.has(r.url));
    return [...remote, ...localOnly];
  });
});
```

**Acceptance:** A recipe saved while offline (simulated by disabling the network in
DevTools before saving) is still present after re-enabling the network and the app
re-running the mount effect.

---

## TASK 5 — MEDIUM: Cap response body size in recipe fetch route

**File:** `web/app/api/recipe/route.ts`

**What to change:**

After the `if (!upstream.ok)` check (line ~57) and before `html = await upstream.text()`,
add a Content-Length guard:

```ts
const contentLength = parseInt(
  upstream.headers.get("content-length") ?? "0",
  10,
);
if (contentLength > 5_000_000) {
  return Response.json(
    { error: "De pagina is te groot om te verwerken." },
    { status: 422 },
  );
}
```

**Acceptance:** `npm run build` passes. The guard is present and reachable.

---

## TASK 6 — MEDIUM: Add input length cap on `ingredient` in challenge route

**File:** `web/app/api/challenge/route.ts`

**What to change:**

After `ingredient = typeof body.ingredient === "string" ? body.ingredient.trim() : ""`
and the `if (!ingredient)` check, add:

```ts
if (ingredient.length > 200) {
  return Response.json(
    { error: "Ingrediëntnaam is te lang (max. 200 tekens)." },
    { status: 400 },
  );
}
```

Also strip unknown keys from `params` so only valid `ChallengeParams` fields pass
through. Replace the `params` assignment with:

```ts
const ALLOWED_PARAMS = new Set([
  "technique", "cuisine", "timeLimit", "simpleAndSmall", "quantity", "unit",
]);
const rawParams =
  typeof body.params === "object" && body.params !== null ? body.params : {};
params = Object.fromEntries(
  Object.entries(rawParams as Record<string, unknown>).filter(([k]) =>
    ALLOWED_PARAMS.has(k),
  ),
) as ChallengeParams;
```

**Acceptance:** `npm run build` passes. A request with a 300-character ingredient string
returns 400.

---

## TASK 7 — MEDIUM: Fix `challenges` table schema mismatch

**Context:** The initial migration defines `difficulty CHECK (difficulty IN ('easy',
'medium', 'hard'))` and is missing the columns `why`, `output_type`, `time_estimate`,
`recipe`, `params` that the challenge route inserts into. Every insert currently fails
silently.

**File to create:** `supabase/migrations/20260527000000_fix_challenges_schema.sql`

**Content:**

```sql
ALTER TABLE challenges
  DROP CONSTRAINT IF EXISTS challenges_difficulty_check;

ALTER TABLE challenges
  ADD CONSTRAINT challenges_difficulty_check
  CHECK (difficulty IN ('Beginner', 'Intermediate', 'Advanced'));

ALTER TABLE challenges ADD COLUMN IF NOT EXISTS skill         TEXT;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS time_estimate INTEGER;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS why           TEXT;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS output_type   TEXT;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS recipe        JSONB;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS params        JSONB;
```

**Also:** In `web/app/api/challenge/route.ts`, the `supabase.from("challenges").insert(...)`
call result is currently ignored. Add error logging:

```ts
const { error: insertError } = await supabase.from("challenges").insert({ ... });
if (insertError) console.error("Challenge insert failed:", insertError.message);
```

**Acceptance:** The migration file exists. The insert no longer silently fails when
`difficulty` is `"Beginner"`.

---

## TASK 8 — LOW: Delete dead legacy stub components

**Files to delete:**

- `web/components/RecipeSaver.tsx`
- `web/components/ChallengeModePanel.tsx`

Neither file is imported anywhere. Verify with `grep -r "RecipeSaver\|ChallengeModePanel" web/`
before deleting. The grep should return only the files themselves.

**Acceptance:** Both files are gone. `npm run build` still passes.

---

## TASK 9 — LOW: Move hardcoded Dutch strings in `RecipesPage` to `lib/strings.ts`

**Files:** `web/components/RecipesPage.tsx`, `web/lib/strings.ts`

**Strings to migrate** (exact text as it appears in the component):

| Hardcoded string | Suggested key in `UI.recipes` |
|---|---|
| `"Nog geen recepten"` | `emptyTitle` |
| `"Voeg je eerste recept toe via een link of een foto van een kookboek."` | `emptySub` |
| `"Eerste recept toevoegen"` | `emptyCta` |
| `"Recept toevoegen"` (add-panel title, line 683) | `addPanelTitle` |
| `"Sluiten"` (aria-label, line 684) | `addPanelClose` |
| `"Recepten zoeken"` (aria-label, line 566) | `searchAriaLabel` (add under `recipes` if not already there) |
| `"Geen recepten gevonden met deze filters"` | `noResultsFilters` |
| `"Wis zoekopdracht en filters"` | `clearAll` |

**Steps:**
1. Add each key/value pair to the `recipes` object in `web/lib/strings.ts`.
2. Replace each hardcoded string in `RecipesPage.tsx` with the corresponding `UI.recipes.*` reference.

**Acceptance:** `grep -n '"Nog geen recepten"\|"Voeg je eerste\|"Eerste recept\|"Recept toevoegen"\|"Wis zoekopdracht"' web/components/RecipesPage.tsx` returns no matches. `npm run build` passes.

---

## Completion checklist

- [ ] Task 1 — SSRF private-IP block
- [ ] Task 2 — Shared-secret auth on all routes
- [ ] Task 3 — `remoteId` captured after first save
- [ ] Task 4 — Remote-wins merge fix
- [ ] Task 5 — Response body size cap
- [ ] Task 6 — Ingredient length cap + params whitelist
- [ ] Task 7 — `challenges` schema migration + insert error logging
- [ ] Task 8 — Delete `RecipeSaver` and `ChallengeModePanel`
- [ ] Task 9 — Migrate hardcoded Dutch strings
