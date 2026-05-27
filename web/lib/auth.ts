import type { NextRequest } from "next/server";
import { createSupabaseServerClient } from "@/lib/supabase-server";

export function requireSecret(req: NextRequest): Response | null {
  const secret = process.env.API_SECRET;
  if (!secret) return null; // not configured — open in dev
  if (req.headers.get("x-api-secret") !== secret) {
    return Response.json({ error: "Niet geautoriseerd." }, { status: 401 });
  }
  return null;
}

import type { SupabaseClient } from "@supabase/supabase-js";

type SessionResult =
  | { error: Response; client: null }
  | { error: null; client: SupabaseClient };

export async function requireSession(): Promise<SessionResult> {
  const client = await createSupabaseServerClient();
  const { data: { user } } = await client.auth.getUser();
  if (!user) {
    return { error: Response.json({ error: "Niet geautoriseerd." }, { status: 401 }), client: null };
  }
  return { error: null, client };
}
