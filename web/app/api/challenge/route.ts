import type { NextRequest } from "next/server";
import type { ChallengeResponse } from "@/lib/types";

const SYSTEM_PROMPT = `Je bent een kookcoach voor thuiskoks. Wanneer je een ingrediënt krijgt, genereer je een gerichte kookuitdaging om een specifieke culinaire techniek te oefenen.

Geef ALLEEN een JSON-object terug met deze exacte velden:
{
  "challenge": "Een duidelijke, motiverende uitdagingsomschrijving (2-3 zinnen, in het Nederlands)",
  "skill": "De specifieke culinaire techniek die geoefend wordt (3-5 woorden, in het Nederlands)",
  "difficulty": "Beginner" of "Intermediate" of "Advanced",
  "timeEstimate": getal (minuten als geheel getal),
  "why": "Korte uitleg waarom deze techniek belangrijk is (1-2 zinnen, in het Nederlands)"
}

Het veld "difficulty" moet exact één van deze Engelse waarden zijn: "Beginner", "Intermediate", "Advanced".
De velden "challenge", "skill" en "why" moeten in het Nederlands zijn.
Schrijf geen tekst buiten het JSON-object.`;

type ClaudeTextBlock = { type: "text"; text: string };
type ClaudeContent = ClaudeTextBlock[];

async function callClaude(apiKey: string, ingredient: string): Promise<ChallengeResponse> {
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      model: "claude-sonnet-4-6",
      max_tokens: 1000,
      temperature: 0.7,
      system: SYSTEM_PROMPT,
      messages: [
        { role: "user", content: `Ingrediënt: ${ingredient}` },
      ],
    }),
  });

  if (!res.ok) {
    throw new Error(`Claude API fout: ${res.status}`);
  }

  const data = (await res.json()) as { content: ClaudeContent };
  const text = data.content.find((b) => b.type === "text")?.text ?? "";

  const jsonMatch = text.match(/\{[\s\S]*\}/);
  if (!jsonMatch) throw new Error("Geen JSON gevonden in antwoord");

  const parsed = JSON.parse(jsonMatch[0]) as Partial<ChallengeResponse>;

  if (
    typeof parsed.challenge !== "string" ||
    typeof parsed.skill !== "string" ||
    !["Beginner", "Intermediate", "Advanced"].includes(parsed.difficulty ?? "") ||
    typeof parsed.timeEstimate !== "number" ||
    typeof parsed.why !== "string"
  ) {
    throw new Error("Ongeldig antwoordformaat van Claude");
  }

  return parsed as ChallengeResponse;
}

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

  // Try twice: initial call, then one silent retry on parse/validation failure
  let lastError: unknown;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const result = await callClaude(apiKey, ingredient);
      return Response.json(result);
    } catch (err) {
      lastError = err;
    }
  }

  const msg = lastError instanceof Error ? lastError.message : "Onbekende fout";
  return Response.json({ error: msg }, { status: 502 });
}
