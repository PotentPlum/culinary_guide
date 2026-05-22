import type { NextRequest } from "next/server";
import { supabase } from "@/lib/supabase";

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  let body: unknown;
  try { body = await req.json(); } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }
  const { name, ingredients, instructions, source, tags } = body as Record<string, unknown>;
  if (!name || !instructions) {
    return Response.json({ error: "Naam en instructies zijn verplicht." }, { status: 400 });
  }
  const basePayload = {
    name: name as string,
    ingredients: Array.isArray(ingredients) ? (ingredients as string[]) : [],
    instructions: instructions as string,
  };
  const fullPayload = {
    ...basePayload,
    source: typeof source === "string" ? source : "",
    tags: tags && typeof tags === "object" ? tags : {},
  };

  let { data, error } = await supabase.from("recipes").update(fullPayload).eq("id", id).select().single();
  if (error?.message?.toLowerCase().includes("schema cache")) {
    ({ data, error } = await supabase.from("recipes").update(basePayload).eq("id", id).select().single());
  }
  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json(data);
}
