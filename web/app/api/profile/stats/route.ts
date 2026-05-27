import type { NextRequest } from "next/server";
import { requireSession } from "@/lib/auth";
import type { UserAttempt } from "@/lib/profileTypes";

export async function GET(_req: NextRequest) {
  const { error: authError, client } = await requireSession();
  if (authError) return authError;

  const { data, error } = await client
    .from("user_attempts")
    .select("*")
    .order("attempted_at", { ascending: false });

  if (error) return Response.json({ error: error.message }, { status: 500 });
  return Response.json({ attempts: (data ?? []) as UserAttempt[] });
}
