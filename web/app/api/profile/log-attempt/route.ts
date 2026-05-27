import { requireSession } from "@/lib/auth";

export async function POST(req: Request) {
  const { error: authError, client, user } = await requireSession();
  if (authError) return authError;

  const body = await req.json() as Record<string, unknown>;
  const { source_type, source_id, score, techniques, cuisines, ingredients, taste_score, notes } = body;

  if (!["challenge", "recipe"].includes(source_type as string)) {
    return Response.json({ error: "Ongeldig brontype." }, { status: 400 });
  }

  const { data, error } = await client
    .from("user_attempts")
    .insert({
      user_id: user.id,
      source_type,
      source_id: (source_id as string) ?? null,
      score: (score as number) ?? null,
      techniques: (techniques as string[]) ?? [],
      cuisines: (cuisines as string[]) ?? [],
      ingredients: (ingredients as string[]) ?? [],
      taste_score: (taste_score as number) ?? null,
      notes: (notes as string) ?? null,
    })
    .select("id")
    .single();

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json({ id: data?.id });
}
