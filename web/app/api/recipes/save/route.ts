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
    .insert({
      name: name as string,
      ingredients: Array.isArray(ingredients) ? (ingredients as string[]) : [],
      instructions: instructions as string,
      source_url: typeof source_url === "string" ? source_url : "",
    })
    .select()
    .single();
  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json(data);
}
