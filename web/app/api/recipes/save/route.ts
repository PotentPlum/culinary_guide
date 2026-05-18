import type { NextRequest } from "next/server";
import type { SavedRecipe } from "@/lib/types";

export async function POST(req: NextRequest) {
  // TODO: wire up Supabase client from @/lib/supabase
  // const { data, error } = await supabase.from("recipes").insert([recipe]).select().single();
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
  const now = new Date().toISOString();
  const recipe: SavedRecipe = {
    id: crypto.randomUUID(),
    name: name as string,
    ingredients: Array.isArray(ingredients) ? (ingredients as string[]) : [],
    instructions: instructions as string,
    source_url: typeof source_url === "string" ? source_url : "",
    created_at: now,
    updated_at: now,
  };
  return Response.json(recipe);
}
