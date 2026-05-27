import type { NextRequest } from "next/server";
import { requireSession } from "@/lib/auth";

export type ChallengeRecord = {
  id: string;
  ingredient: string;
  challenge_text: string;
  skill: string | null;
  difficulty: string | null;
  time_estimate: number | null;
  why: string | null;
  output_type: string | null;
  created_at: string;
};

export async function GET(_req: NextRequest) {
  const { error: authError, client, user } = await requireSession();
  if (authError) return authError;

  const { data, error } = await client
    .from("challenges")
    .select("id, ingredient, challenge_text, skill, difficulty, time_estimate, why, output_type, created_at")
    .eq("user_id", user.id)
    .order("created_at", { ascending: false });

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json({ challenges: (data ?? []) as ChallengeRecord[] });
}
