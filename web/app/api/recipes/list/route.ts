import { supabase } from "@/lib/supabase";

export async function GET() {
  const { data, error } = await supabase
    .from("recipes")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) return Response.json({ error: error.message }, { status: 500 });
  const rows = (data ?? []) as Array<{ id: string; name: string; instructions: string; source_url: string; source: string; created_at: string }>;
  return Response.json({ recipes: rows });
}
