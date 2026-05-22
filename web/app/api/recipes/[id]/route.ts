import type { NextRequest } from "next/server";
import { supabase } from "@/lib/supabase";

export async function PATCH(req: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  let body: unknown;
  try { body = await req.json(); } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }
  const {
    name, ingredients, instructions, source, tags,
    description, prep_time, cook_time, total_time, servings,
    cuisine, category, keywords, image_url, author, suitable_for_diet, nutrition,
  } = body as Record<string, unknown>;
  if (!name || !instructions) {
    return Response.json({ error: "Naam en instructies zijn verplicht." }, { status: 400 });
  }
  const { data, error } = await supabase
    .from("recipes")
    .update({
      name: name as string,
      ingredients: Array.isArray(ingredients) ? (ingredients as string[]) : [],
      instructions: instructions as string,
      source: typeof source === "string" ? source : "",
      tags: tags && typeof tags === "object" ? tags : {},
      description: typeof description === "string" ? description : "",
      prep_time: typeof prep_time === "number" ? prep_time : null,
      cook_time: typeof cook_time === "number" ? cook_time : null,
      total_time: typeof total_time === "number" ? total_time : null,
      servings: typeof servings === "number" ? servings : null,
      cuisine: Array.isArray(cuisine) ? cuisine : [],
      category: Array.isArray(category) ? category : [],
      keywords: Array.isArray(keywords) ? keywords : [],
      image_url: typeof image_url === "string" ? image_url : null,
      author: typeof author === "string" ? author : null,
      suitable_for_diet: Array.isArray(suitable_for_diet) ? suitable_for_diet : [],
      nutrition: nutrition && typeof nutrition === "object" ? nutrition : {},
    })
    .eq("id", id)
    .select()
    .single();
  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json(data);
}
