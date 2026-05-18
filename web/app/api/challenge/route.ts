import type { NextRequest } from "next/server";

export async function POST(req: NextRequest) {
  const apiKey = process.env.CLAUDE_API_KEY;
  if (!apiKey) {
    return Response.json({ error: "Claude API-sleutel niet geconfigureerd." }, { status: 500 });
  }
  let ingredient: string;
  try {
    const body = (await req.json()) as { ingredient?: unknown };
    ingredient = typeof body.ingredient === "string" ? body.ingredient.trim() : "";
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }
  if (!ingredient) {
    return Response.json({ error: "Geen ingrediënt opgegeven." }, { status: 400 });
  }

  // TODO: replace stub with real Claude API call
  // const response = await fetch("https://api.anthropic.com/v1/messages", {
  //   method: "POST",
  //   headers: { "x-api-key": apiKey, "anthropic-version": "2023-06-01", "content-type": "application/json" },
  //   body: JSON.stringify({
  //     model: "claude-sonnet-4-6",
  //     max_tokens: 1024,
  //     messages: [{ role: "user", content: `Maak een kookuitdaging voor: ${ingredient}` }],
  //   }),
  // });

  return Response.json({
    ingredient,
    challenge_text: `Uitdaging voor ${ingredient}: bereid dit ingrediënt op drie verschillende manieren.`,
    difficulty: "medium",
  });
}
