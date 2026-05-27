import type { NextRequest } from "next/server";
import { ingestRecipeFromHtml, RecipeIngestionError } from "@/lib/recipeIngestion";
import { requireSession } from "@/lib/auth";

const BROWSER_HEADERS: Record<string, string> = {
  "User-Agent":
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
  Accept:
    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
  "Accept-Language": "nl-NL,nl;q=0.9,en-US;q=0.8,en;q=0.7",
  "Accept-Encoding": "gzip, deflate, br",
  "Cache-Control": "max-age=0",
  "Upgrade-Insecure-Requests": "1",
  "Sec-Fetch-Dest": "document",
  "Sec-Fetch-Mode": "navigate",
  "Sec-Fetch-Site": "none",
  "Sec-Fetch-User": "?1",
  "Sec-CH-UA": '"Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"',
  "Sec-CH-UA-Mobile": "?0",
  "Sec-CH-UA-Platform": '"Windows"',
};

const BLOCKED_HOST_PATTERNS = [
  /^localhost$/i,
  /^127\./,
  /^10\./,
  /^192\.168\./,
  /^172\.(1[6-9]|2\d|3[01])\./,
  /^169\.254\./,
  /^0\.0\.0\.0$/,
  /^::1$/,
  /^::ffff:/i,
  /^fc[0-9a-f]{2}:/i,
  /^fd[0-9a-f]{2}:/i,
];

export async function POST(req: NextRequest) {
  const { error: authError } = await requireSession();
  if (authError) return authError;

  let url: string;
  try {
    const body = (await req.json()) as { url?: unknown };
    url = typeof body.url === "string" ? body.url.trim() : "";
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }

  if (!url) {
    return Response.json({ error: "Geen URL opgegeven." }, { status: 400 });
  }

  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    return Response.json({ error: "Ongeldige URL." }, { status: 400 });
  }

  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    return Response.json(
      { error: "Alleen http(s) URLs zijn toegestaan." },
      { status: 400 },
    );
  }

  if (BLOCKED_HOST_PATTERNS.some((p) => p.test(parsed.hostname))) {
    return Response.json(
      { error: "URL niet toegestaan." },
      { status: 400 },
    );
  }

  let html: string;
  try {
    const upstream = await fetch(url, {
      headers: BROWSER_HEADERS,
      redirect: "error",
    });

    if (!upstream.ok) {
      return Response.json(
        { error: `De pagina kon niet worden opgehaald (HTTP ${upstream.status}).` },
        { status: 502 },
      );
    }

    const rawContentLength = upstream.headers.get("content-length");
    const contentLength = rawContentLength !== null ? parseInt(rawContentLength, 10) : null;
    if (contentLength !== null && (isNaN(contentLength) || contentLength > 5_000_000)) {
      return Response.json(
        { error: "De pagina is te groot om te verwerken." },
        { status: 422 },
      );
    }

    // Also cap actual bytes read to guard against absent or lying Content-Length headers.
    const MAX_BYTES = 5_000_000;
    const reader = upstream.body?.getReader();
    if (!reader) {
      return Response.json(
        { error: "De URL kon niet worden bereikt. Controleer de link en probeer opnieuw." },
        { status: 502 },
      );
    }
    const chunks: Uint8Array[] = [];
    let totalBytes = 0;
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      totalBytes += value.byteLength;
      if (totalBytes > MAX_BYTES) {
        await reader.cancel();
        return Response.json(
          { error: "De pagina is te groot om te verwerken." },
          { status: 422 },
        );
      }
      chunks.push(value);
    }
    html = new TextDecoder().decode(
      chunks.reduce((acc, chunk) => {
        const merged = new Uint8Array(acc.byteLength + chunk.byteLength);
        merged.set(acc, 0);
        merged.set(chunk, acc.byteLength);
        return merged;
      }, new Uint8Array(0)),
    );
  } catch {
    return Response.json(
      { error: "De URL kon niet worden bereikt. Controleer de link en probeer opnieuw." },
      { status: 502 },
    );
  }

  try {
    const recipe = ingestRecipeFromHtml(html);

    // Secondary Haiku call: extract taxonomy from the parsed recipe
    let taxonomy = { techniques: [] as string[], cuisines: [] as string[], ingredients: [] as string[] };
    const apiKey = process.env.CLAUDE_API_KEY;
    if (apiKey) {
      try {
        const ingredientText = recipe.ingredientSections
          .flatMap((s: { ingredients: { text: string }[] }) => s.ingredients.map((i) => i.text))
          .join('\n');
        const taxonomyPrompt = `Recipe: "${recipe.title}"\nIngredients:\n${ingredientText}\n\nExtract JSON with exactly these fields:\n{\n  "techniques": [technique IDs used, from: julienne, brunoise, sauteren, schroeien, smoren, blancheren, groenten-roasten, groenten-roerbakken, emulgeren, marineren, pekelen, roux, bechamel, tomatensaus, hollandaise, mayonaise, vinaigrette, fond-deglaceren],\n  "cuisines": [from: Aziatisch, Chinees, Frans, Grieks, Indiaas, Italiaans, Japans, Koreaans, Latijns-Amerikaans, Mediterraan, Mexicaans, Midden-Oosten, Nederlands, Noord-Afrikaans, Spaans, Thai, Overig],\n  "ingredients": [main ingredients in Dutch, lowercase, max 8]\n}\nReturn ONLY the JSON object, no explanation.`;

        const haikuRes = await fetch("https://api.anthropic.com/v1/messages", {
          method: "POST",
          headers: {
            "x-api-key": apiKey,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json",
          },
          body: JSON.stringify({
            model: "claude-haiku-4-5-20251001",
            max_tokens: 300,
            messages: [{ role: "user", content: taxonomyPrompt }],
          }),
        });

        if (haikuRes.ok) {
          const haikuData = (await haikuRes.json()) as { content: { type: string; text: string }[] };
          const rawText = haikuData.content.find((b) => b.type === "text")?.text ?? "{}";
          const jsonMatch = rawText.match(/\{[\s\S]*\}/);
          if (jsonMatch) {
            taxonomy = JSON.parse(jsonMatch[0]);
          }
        }
      } catch {
        // fail gracefully — taxonomy stays as empty arrays
      }
    }

    return Response.json({ ...recipe, taxonomy });
  } catch (err) {
    if (err instanceof RecipeIngestionError && err.code === "UNSUPPORTED_RECIPE_FORMAT") {
      return Response.json(
        {
          error:
            "Deze pagina bevat geen herkenbaar recept in gestructureerde data (Schema.org). " +
            "Probeer een andere receptpagina.",
        },
        { status: 422 },
      );
    }
    return Response.json(
      { error: "Onverwachte fout bij het verwerken van het recept." },
      { status: 500 },
    );
  }
}
