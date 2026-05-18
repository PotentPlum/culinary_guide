export async function GET() {
  // TODO: wire up Supabase client from @/lib/supabase
  // const { data, error } = await supabase.from("recipes").select("*").order("created_at", { ascending: false });
  return Response.json({ recipes: [] });
}
