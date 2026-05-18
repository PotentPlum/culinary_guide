# Supabase + Claude API Integration — Implementation Guide

This guide walks through wiring the stub files into a fully working Supabase-backed recipe persistence layer and Claude-powered challenge mode.

## Prerequisites

- A [Supabase](https://supabase.com) project (free tier is fine)
- An [Anthropic](https://console.anthropic.com) API key
- The stub files already in place (see repo)

---

## Supabase Setup (SQL DDL)

Open the **SQL Editor** in your Supabase dashboard and run the following statements in order.

### Create the tables

```sql
-- recipes table
create table recipes (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  ingredients jsonb not null default '[]',
  instructions text not null,
  source_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- auto-update updated_at
create or replace function update_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger recipes_updated_at
  before update on recipes
  for each row execute function update_updated_at();

-- challenges table
create table challenges (
  id uuid primary key default gen_random_uuid(),
  ingredient text not null,
  challenge_text text not null,
  difficulty text check (difficulty in ('easy', 'medium', 'hard')),
  created_at timestamptz not null default now()
);
```

### Enable Row Level Security and allow anon reads

The anon key (used by `supabase.ts`) can only read data once RLS is on and a policy grants access.

```sql
-- Enable RLS on both tables
alter table recipes enable row level security;
alter table challenges enable row level security;

-- Allow anyone with the anon key to read and write recipes
create policy "anon can read recipes"
  on recipes for select
  using (true);

create policy "anon can insert recipes"
  on recipes for insert
  with check (true);

-- Allow anyone with the anon key to read and write challenges
create policy "anon can read challenges"
  on challenges for select
  using (true);

create policy "anon can insert challenges"
  on challenges for insert
  with check (true);
```

> **Note:** These open policies are fine for a local/demo project. For production, scope them to authenticated users.

---

## Step 1: Configure Environment

Copy the example file and fill in your credentials:

```bash
cp web/.env.local.example web/.env.local
```

Open `web/.env.local` and set the three values:

```
NEXT_PUBLIC_SUPABASE_URL=https://<your-project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<your-anon-key>
CLAUDE_API_KEY=<your-anthropic-api-key>
```

**Where to find each value:**

| Variable | Location |
|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | Supabase Dashboard → Settings → API → Project URL |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Supabase Dashboard → Settings → API → Project API keys → `anon` `public` |
| `CLAUDE_API_KEY` | [console.anthropic.com](https://console.anthropic.com) → API Keys |

> `NEXT_PUBLIC_` prefix makes the Supabase values available in the browser bundle. `CLAUDE_API_KEY` has no prefix — it stays server-only and is only accessed inside API routes.

---

## Step 2: Wire Recipe Persistence

### Update the save route

Replace the stub body in `web/app/api/recipes/save/route.ts` with the real Supabase insert:

```typescript
import type { NextRequest } from "next/server";
import { supabase } from "@/lib/supabase";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }

  const { name, ingredients, instructions, source_url } = body as Record<string, unknown>;

  if (!name || !instructions) {
    return Response.json({ error: "Naam en instructies zijn verplicht." }, { status: 400 });
  }

  const { data, error } = await supabase
    .from("recipes")
    .insert([
      {
        name,
        ingredients: Array.isArray(ingredients) ? ingredients : [],
        instructions,
        source_url: typeof source_url === "string" ? source_url : null,
      },
    ])
    .select()
    .single();

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json(data);
}
```

### Update the list route

Replace the stub body in `web/app/api/recipes/list/route.ts`:

```typescript
import { supabase } from "@/lib/supabase";

export async function GET() {
  const { data, error } = await supabase
    .from("recipes")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json({ recipes: data });
}
```

### Wire up the RecipeSaver component

The `RecipeSaver` stub already has the fetch call commented out. Uncomment it and remove the fake `setSaved(true)` shortcut:

```typescript
async function handleSave() {
  setSaving(true);
  try {
    const res = await fetch("/api/recipes/save", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(recipe),
    });
    if (res.ok) {
      setSaved(true);
      onSaved?.();
    }
  } finally {
    setSaving(false);
  }
}
```

### Add RecipeSaver to RecipesPage

`RecipesPage` already has its own local save logic (`handleSave` / `isSaved`). To integrate Supabase persistence, import `RecipeSaver` and place it alongside the existing save button inside `RecipeResult`, passing the current recipe's fields:

```typescript
// At the top of web/components/RecipesPage.tsx
import RecipeSaver from "@/components/RecipeSaver";
```

Inside the `RecipeResult` component's `recipe-result-actions` div, add `RecipeSaver` next to the existing local save button:

```tsx
<div className="recipe-result-actions">
  {/* existing local save button */}
  <button
    className={`recipe-save-btn${isSaved ? " recipe-save-btn--saved" : ""}`}
    onClick={onSave}
    disabled={isSaved}
  >
    {isSaved ? UI.recipes.savedBtn : UI.recipes.saveBtn}
  </button>

  {/* Supabase persistence */}
  <RecipeSaver
    recipe={{
      name: recipe.title,
      ingredients: recipe.ingredientSections.flatMap((s) => s.ingredients),
      instructions: recipe.instructionSections
        .flatMap((s) => s.steps)
        .map((step) => `${step.stepNumber}. ${step.text}`)
        .join("\n"),
      source_url: activeUrl,
    }}
  />

  <button className="recipe-clear-btn" onClick={onReset} aria-label={UI.recipes.clearBtn}>
    {UI.recipes.clearBtn}
  </button>
</div>
```

> `RecipeResult` does not have access to `activeUrl` directly — either thread it as a prop (`activeUrl: string`) or hoist it from the parent. The simplest approach is to add `activeUrl` to `RecipeResult`'s props type and pass it from `RecipesPage`.

---

## Step 3: Display Saved Recipes

To show persisted recipes from Supabase (not just localStorage), fetch the list endpoint on mount. Add this to `web/components/RecipesPage.tsx` or to a dedicated section in `web/app/page.tsx`:

```typescript
import { useState, useEffect } from "react";
import type { SavedRecipe } from "@/lib/types";

const [recipes, setRecipes] = useState<SavedRecipe[]>([]);

useEffect(() => {
  fetch("/api/recipes/list")
    .then((r) => r.json())
    .then((d) => setRecipes(d.recipes ?? []));
}, []);
```

Then render the list wherever makes sense in the UI — for example, in the existing `SavedRecipesList` section of `RecipesPage`, alongside or replacing the localStorage-backed `savedRecipes` state. The `SavedRecipe` type from `web/lib/types.ts` matches the Supabase row shape exactly.

---

## Step 4: Set Up Challenge Mode

### Update the challenge route

Replace the stub in `web/app/api/challenge/route.ts` with a real Claude API call that also persists the result to Supabase:

```typescript
import type { NextRequest } from "next/server";
import { supabase } from "@/lib/supabase";

export async function POST(req: NextRequest) {
  const apiKey = process.env.CLAUDE_API_KEY;
  if (!apiKey) {
    return Response.json({ error: "Claude API-sleutel niet geconfigureerd." }, { status: 500 });
  }

  let ingredient: string;
  try {
    const body = (await req.json()) as { ingredient?: unknown };
    ingredient = typeof body.ingredient === "string" ? body.ingredient.trim() : "";
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }

  if (!ingredient) {
    return Response.json({ error: "Geen ingrediënt opgegeven." }, { status: 400 });
  }

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      model: "claude-sonnet-4-6",
      max_tokens: 1024,
      messages: [
        {
          role: "user",
          content: `Genereer een creatieve kookuitdaging voor het ingrediënt: ${ingredient}. Geef een uitdagende taak en leg uit waarom dit leerzaam is. Antwoord in het Nederlands.`,
        },
      ],
    }),
  });

  if (!response.ok) {
    const err = await response.text();
    return Response.json({ error: `Claude API fout: ${err}` }, { status: 502 });
  }

  const claude = (await response.json()) as { content: Array<{ text: string }> };
  const challenge_text = claude.content[0]?.text ?? "Geen uitdaging gegenereerd.";

  const { data, error } = await supabase
    .from("challenges")
    .insert([{ ingredient, challenge_text, difficulty: "medium" }])
    .select()
    .single();

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json(data);
}
```

### Wire ChallengeModePanel into RecipesPage

The `ChallengeModePanel` component expects an `ingredients` array. In `RecipesPage`, once a recipe is loaded you have `recipe.ingredientSections`. Flatten the ingredients and pass them to the panel.

In `web/components/RecipesPage.tsx`, uncomment the fetch call in `ChallengeModePanel`'s stub `handleGenerate`:

```typescript
async function handleGenerate() {
  if (!selected) return;
  setLoading(true);
  setError(null);
  try {
    const res = await fetch("/api/challenge", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ ingredient: selected }),
    });
    const data = await res.json();
    if (res.ok) setResult(data);
    else setError(data.error ?? "Onbekende fout.");
  } catch {
    setError("Netwerkfout. Probeer het opnieuw.");
  } finally {
    setLoading(false);
  }
}
```

Then render `ChallengeModePanel` in the `RecipeResult` section, after the instructions column:

```tsx
// At the top of RecipesPage.tsx
import ChallengeModePanel from "@/components/ChallengeModePanel";
```

```tsx
{/* Below the recipe-result-body div, inside RecipeResult */}
<ChallengeModePanel
  ingredients={recipe.ingredientSections.flatMap((s) => s.ingredients)}
/>
```

Pass `recipe` as a prop to `RecipeResult` (it already receives it), so no additional prop threading is needed.

---

## Step 5: Testing Checklist

Work through this list top-to-bottom to verify everything is connected:

- [ ] `.env.local` created with all three keys (`NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `CLAUDE_API_KEY`)
- [ ] Supabase tables (`recipes`, `challenges`) created via SQL Editor
- [ ] RLS enabled and anon policies in place on both tables
- [ ] `npm run dev` starts without errors (check terminal for missing-env-var throws from `web/lib/supabase.ts`)
- [ ] POST `/api/recipes/save` with `{ "name": "Test", "ingredients": ["wortel"], "instructions": "Kook." }` returns a recipe object with a UUID `id`
- [ ] GET `/api/recipes/list` returns `{ "recipes": [{ ... }] }` including the saved recipe
- [ ] Recipe row is visible in the Supabase dashboard Table Editor after save
- [ ] Recipe persists after a hard page reload (data comes from Supabase, not localStorage)
- [ ] POST `/api/challenge` with `{ "ingredient": "wortel" }` returns a `challenge_text` in Dutch from Claude
- [ ] Challenge row is visible in the Supabase dashboard after generation
- [ ] `RecipeSaver` button appears in the recipe reader UI after a recipe is loaded
- [ ] Clicking `RecipeSaver` sets it to "Opgeslagen" and the recipe appears in Supabase
- [ ] `ChallengeModePanel` renders below the recipe body with the ingredient dropdown populated
- [ ] Selecting an ingredient and clicking "Genereer uitdaging" returns a real Dutch challenge from Claude
