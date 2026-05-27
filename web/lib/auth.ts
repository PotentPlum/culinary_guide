import type { NextRequest } from "next/server";

export function requireSecret(req: NextRequest): Response | null {
  const secret = process.env.API_SECRET;
  if (!secret) return null; // not configured — open in dev
  if (req.headers.get("x-api-secret") !== secret) {
    return Response.json({ error: "Niet geautoriseerd." }, { status: 401 });
  }
  return null;
}
