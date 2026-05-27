import type { NutritionInfo, AggregateRating } from "@/lib/recipeIngestion";
import type { RecipeTags } from "@/lib/facets";
import type { NextRequest } from "next/server";
import { requireSession } from "@/lib/auth";

export type RecipeRow = {
  id: string;
  name: string;
  instructions: string;
  source_url: string;
  source: string;
  tags: RecipeTags;
  created_at: string;
  description: string;
  prep_time: number | null;
  cook_time: number | null;
  total_time: number | null;
  servings: number | null;
  cuisine: string[];
  category: string[];
  keywords: string[];
  image_url: string | null;
  author: string | null;
  suitable_for_diet: string[];
  date_published: string | null;
  aggregate_rating: AggregateRating | null;
  video_url: string | null;
  nutrition: NutritionInfo;
};

export async function GET(req: NextRequest) {
  const { error: authError, client } = await requireSession();
  if (authError) return authError;

  const { data, error } = await client
    .from("recipes")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json({ recipes: (data ?? []) as RecipeRow[] });
}
