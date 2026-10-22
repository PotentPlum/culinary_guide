# Legacy prompt: recipe-from-photos

Verbatim from `web/app/api/recipe-from-images/route.ts` at commit cf3ec39. `${...}` placeholders were filled from `facets.json` option values at runtime. Reference only: the Android app rewrites these for structured outputs (see spec.md §8).

## `SYSTEM_PROMPT`

```text
Je bent een kookassistent die recepten uit foto's leest.
Extraheer het recept uit de afbeelding(en) en geef ALLEEN een geldig JSON-object terug.
Combineer meerdere foto's als ze onderdelen van hetzelfde recept tonen.
Converteer alle maten naar metrisch systeem (g, ml, °C — geen oz, cups, °F).
Schrijf geen tekst buiten het JSON-object.
```

## `text`

```text
Lees ${images.length === 1 ? "dit recept" : "deze receptfoto's"} en geef het recept terug als JSON:
{
  "title": "naam",
  "description": "optionele korte beschrijving",
  "prepTimeMinutes": 20,
  "cookTimeMinutes": 30,
  "totalTimeMinutes": 50,
  "servings": 4,
  "ingredients": ["200g bloem", "3 eieren"],
  "steps": ["Stap 1 tekst", "Stap 2 tekst"],
  "tags": {
    "cuisine": ["optioneel — een of meer van: ${CUISINES}"],
    "dishType": ["VERPLICHT — een van: ${DISH_TYPES}"],
    "mainIngredient": ["een of meer van: ${MAIN_INGR}"],
    "prepTime": ["VERPLICHT — een van: ${PREP_TIMES}, gebaseerd op totalTimeMinutes"]
  },
  "taxonomy": {
    "techniques": ["techniek-ID's die voorkomen in dit recept, uit: julienne, brunoise, batonnet, snipperen, chiffonade, sauteren, schroeien, smoren, sudderen, koken, blancheren, groenten-roasten, groenten-roerbakken, emulgeren, fond-deglaceren, marineren, pekelen, roux, bechamel, tomatensaus, hollandaise, mayonaise, vinaigrette"],
    "cuisines": ["nul of meer van: Aziatisch, Chinees, Frans, Grieks, Indiaas, Italiaans, Japans, Koreaans, Latijns-Amerikaans, Mediterraan, Mexicaans, Midden-Oosten, Nederlands, Noord-Afrikaans, Spaans, Thai, Overig"],
    "ingredients": ["hoofdingrediënten in het Nederlands, kleine letters, maximaal 8"]
  }
}

Regels:
- Alle maten in metrisch: oz→g, cups→ml, °F→°C
- Tijden als geheel getal in minuten, of null als niet vermeld
- servings: aantal porties als geheel getal, of null als niet vermeld
- tags.cuisine: alleen invullen als duidelijk uit het recept — anders weglaten of lege array
- tags.dishType en tags.prepTime zijn verplicht
- taxonomy.techniques: alleen technieken die duidelijk zichtbaar zijn in het recept
- taxonomy.cuisines: alleen invullen als duidelijk uit het recept — anders lege array
- taxonomy.ingredients: maximaal 8 hoofdingrediënten, in het Nederlands, kleine letters
- Ingrediënten vóór stappen
- Alleen JSON, geen extra tekst
```
