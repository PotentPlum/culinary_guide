# Legacy prompt: recipe-taxonomy

Verbatim from `web/app/api/recipe/route.ts` at commit cf3ec39. `${...}` placeholders were filled from `facets.json` option values at runtime. Reference only: the Android app rewrites these for structured outputs (see spec.md §8).

## `taxonomyPrompt`

```text
Recipe: "${recipe.title}"
Ingredients:
${ingredientText}

Extract JSON with exactly these fields:
{
  "techniques": [technique IDs used, from: julienne, brunoise, sauteren, schroeien, smoren, blancheren, groenten-roasten, groenten-roerbakken, emulgeren, marineren, pekelen, roux, bechamel, tomatensaus, hollandaise, mayonaise, vinaigrette, fond-deglaceren],
  "cuisines": [from: Aziatisch, Chinees, Frans, Grieks, Indiaas, Italiaans, Japans, Koreaans, Latijns-Amerikaans, Mediterraan, Mexicaans, Midden-Oosten, Nederlands, Noord-Afrikaans, Spaans, Thai, Overig],
  "ingredients": [main ingredients in Dutch, lowercase, max 8]
}
Return ONLY the JSON object, no explanation.
```
