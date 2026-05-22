import { supabase } from "@/lib/supabase";
import type { NutritionInfo, AggregateRating } from "@/lib/recipeIngestion";
import type { RecipeTags } from "@/lib/facets";

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

export async function GET() {
  const { data, error } = await supabase
    .from("recipes")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json({ recipes: (data ?? []) as RecipeRow[] });
}
