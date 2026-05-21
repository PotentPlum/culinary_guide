/**
 * One-time migration: tags all untagged recipes in Supabase using Claude.
 *
 * Usage:
 *   cd web && npx tsx scripts/tag-existing-recipes.ts
 *
 * Requires .env.local with:
 *   NEXT_PUBLIC_SUPABASE_URL
 *   NEXT_PUBLIC_SUPABASE_ANON_KEY
 *   CLAUDE_API_KEY
 */

import { createClient } from "@supabase/supabase-js";
import { readFileSync } from "fs";
import { resolve } from "path";
import { FACETS, getFacet } from "../lib/facets";
import type { RecipeTags } from "../lib/facets";

// Load .env.local
function loadEnv() {
  try {
    const envPath = resolve(process.cwd(), ".env.local");
    const lines = readFileSync(envPath, "utf-8").split("\n");
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("#")) continue;
      const eq = trimmed.indexOf("=");
      if (eq === -1) continue;
      const key = trimmed.slice(0, eq).trim();
      const value = trimmed.slice(eq + 1).trim().replace(/^["']|["']$/g, "");
      if (!process.env[key]) process.env[key] = value;
    }
  } catch {
    // Fall through — variables may already be set in environment
  }
}

loadEnv();

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL;
const SUPABASE_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
const CLAUDE_API_KEY = process.env.CLAUDE_API_KEY;

if (!SUPABASE_URL || !SUPABASE_KEY || !CLAUDE_API_KEY) {
  console.error("Missing environment variables. Check .env.local for NEXT_PUBLIC_SUPABASE_URL, NEXT_PUBLIC_SUPABASE_ANON_KEY, and CLAUDE_API_KEY.");
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

const DISH_TYPES  = getFacet("dishType").options.map((o) => o.value).join(", ");
const CUISINES    = getFacet("cuisine").options.map((o) => o.value).join(", ");
const MAIN_INGR   = getFacet("mainIngredient").options.map((o) => o.value).join(", ");
const PREP_TIMES  = getFacet("prepTime").options.map((o) => o.value).join(", ");

// Keep FACETS used for potential future reference
void FACETS;

const SYSTEM_PROMPT = `Je bent een receptclassificator. Analyseer het gegeven recept en ken tags toe op basis van de beschikbare categorieën.

Geef ALLEEN een JSON-object terug:
{
  "cuisine": ["optioneel — een of meer van: ${CUISINES}"],
  "dishType": ["VERPLICHT — een of meer van: ${DISH_TYPES}"],
  "mainIngredient": ["VERPLICHT — een of meer van: ${MAIN_INGR}"],
  "prepTime": ["VERPLICHT — exact een van: ${PREP_TIMES}"]
}

Regels:
- Kies alleen uit de opgegeven waarden
- cuisine: laat leeg als onduidelijk
- dishType: maximaal 2 waarden
- mainIngredient: maximaal 3 waarden
- prepTime: schat op basis van de receptnaam en ingrediënten; kies <30 als onzeker
- Schrijf geen tekst buiten het JSON-object`;

async function tagRecipe(
  name: string,
  ingredients: string[],
  instructionsSample: string,
): Promise<RecipeTags> {
  const userMessage = [
    `Recept: ${name}`,
    ingredients.length > 0 ? `Ingrediënten: ${ingredients.slice(0, 10).join(", ")}` : "",
    instructionsSample ? `Bereiding (kort): ${instructionsSample.slice(0, 200)}` : "",
  ].filter(Boolean).join("\n");

  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "x-api-key": CLAUDE_API_KEY!,
      "anthropic-version": "2023-06-01",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      model: "claude-haiku-4-5-20251001",
      max_tokens: 256,
      temperature: 0,
      system: SYSTEM_PROMPT,
      messages: [{ role: "user", content: userMessage }],
    }),
  });

  if (!res.ok) throw new Error(`Claude API fout: ${res.status}`);

  const data = (await res.json()) as { content: Array<{ type: string; text?: string }> };
  const text = data.content.find((b) => b.type === "text")?.text ?? "";
  const jsonMatch = text.match(/\{[\s\S]*\}/);
  if (!jsonMatch) throw new Error("Geen JSON in antwoord");

  return JSON.parse(jsonMatch[0]) as RecipeTags;
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function main() {
  console.log("Recepten ophalen uit Supabase…");

  const { data: rows, error } = await supabase
    .from("recipes")
    .select("id, name, ingredients, instructions, tags")
    .order("created_at", { ascending: true });

  if (error) {
    console.error("Supabase fout:", error.message);
    process.exit(1);
  }

  const untagged = (rows ?? []).filter((r) => {
    const tags = r.tags as RecipeTags | null;
    if (!tags) return true;
    const vals = Object.values(tags).flat();
    return vals.length === 0;
  });

  console.log(`${rows?.length ?? 0} recepten gevonden, ${untagged.length} zonder tags.\n`);

  if (untagged.length === 0) {
    console.log("Niets te doen — alle recepten zijn al getagd.");
    return;
  }

  let success = 0;
  let failures = 0;

  for (let i = 0; i < untagged.length; i++) {
    const row = untagged[i];
    const num = `[${i + 1}/${untagged.length}]`;

    // Extract a brief instructions sample from the stored JSON if possible
    let instructionsSample = "";
    try {
      const parsed = JSON.parse(row.instructions as string) as { instructionSections?: Array<{ steps?: Array<{ text?: string }> }> };
      instructionsSample = parsed.instructionSections?.[0]?.steps?.[0]?.text ?? "";
    } catch {
      instructionsSample = String(row.instructions ?? "").slice(0, 200);
    }

    try {
      console.log(`${num} ${row.name} — tagging…`);
      const tags = await tagRecipe(
        row.name as string,
        (row.ingredients as string[]) ?? [],
        instructionsSample,
      );

      const { error: updateError } = await supabase
        .from("recipes")
        .update({ tags })
        .eq("id", row.id);

      if (updateError) throw new Error(updateError.message);

      console.log(`    ✓ ${JSON.stringify(tags)}`);
      success++;
    } catch (err) {
      console.error(`    ✗ Mislukt: ${err instanceof Error ? err.message : String(err)}`);
      failures++;
    }

    // Avoid rate limiting — 200 ms between calls
    if (i < untagged.length - 1) await sleep(200);
  }

  console.log(`\nKlaar. ${success} gelukt, ${failures} mislukt.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
