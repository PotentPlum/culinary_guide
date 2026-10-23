package nl.potentplum.mise.claude.prompts

import nl.potentplum.mise.content.Content

/**
 * The Dutch prompts (spec §8.3), rewritten from `data/reference/prompts/` without JSON-format instructions: the output
 * schema enforces the format. Each system prompt is byte-stable for a given content version, so per-request content
 * always comes last.
 */
object Prompts {
    /** Units and techniques by id, so Claude picks the right ids (the schema only lists them). */
    fun vocabulary(content: Content): String =
        buildString {
            appendLine("Eenheden (id: enkelvoud/meervoud):")
            content.units.forEach { appendLine("- ${it.id}: ${it.singular}/${it.plural}") }
            appendLine()
            appendLine("Technieken (id: naam):")
            content.skills.forEach { appendLine("- ${it.id}: ${it.nl}") }
        }.trimEnd()

    fun enrichmentSystem(content: Content): String =
        """
        Je structureert ingrediëntregels van Nederlandse recepten en tagt het recept.

        Voor elke ingrediëntregel (met zijn index):
        - Splits in hoeveelheid, eventueel een bovengrens bij een bereik ("2-3"), eenheid, naam en notitie.
        - Gebruik alleen eenheden uit de lijst hieronder; zonder passende eenheid is de eenheid leeg.
        - Zet niet-metrische maten om naar metrisch (oz naar g, cups naar ml, °F naar °C) en reken de hoeveelheid om.
        - De naam is Nederlands, in kleine letters en enkelvoud ("ui", niet "uien").
        - Een verpakkingsmaat ("à 400 g") en een bewerking ("in blokjes") horen in de notitie.
        - Als een regel geen ingrediënt is, laat hoeveelheid, eenheid en naam leeg.

        Tags: tag alleen wat het recept duidelijk ondersteunt. Kies precies één gerechttype. Technieken zijn alleen
        technieken die in de bereiding echt voorkomen.

        ${vocabulary(content)}
        """.trimIndent()

    fun enrichmentRequest(
        title: String,
        ingredientLines: List<String>,
        steps: List<String>,
    ): String =
        buildString {
            appendLine("Recept: $title")
            appendLine()
            appendLine("Ingrediëntregels:")
            ingredientLines.forEachIndexed { i, line -> appendLine("$i. $line") }
            if (steps.isNotEmpty()) {
                appendLine()
                appendLine("Bereiding:")
                steps.forEachIndexed { i, step -> appendLine("${i + 1}. $step") }
            }
        }.trimEnd()

    fun photoSystem(content: Content): String =
        """
        Je bent een kookassistent die recepten uit foto's van kookboeken en tijdschriften leest.

        - Combineer meerdere foto's tot één recept als ze bij hetzelfde recept horen.
        - Zet alle maten om naar metrisch: oz naar g, cups naar ml, °F naar °C.
        - Neem gedrukte kopjes over als groepsnamen ("Voor de saus", "Voor de marinade"); zonder kopje is de groep naamloos.
        - Neem extra gedrukte velden over als extra veld met label en waarde, zoals een wijnsuggestie, het paginanummer
          of een opmerking.
        - Als de titel van het kookboek zichtbaar is, is dat de bron.
        - Tijden zijn hele minuten, of leeg als ze er niet staan. Porties zijn een heel getal, of leeg.
        - Structureer elke ingrediëntregel in hoeveelheid, eenheid, naam (Nederlands, klein, enkelvoud) en notitie;
          de tekst is de regel zoals hij bedoeld is, in het Nederlands.
        - Tag alleen wat het recept duidelijk ondersteunt.

        ${vocabulary(content)}
        """.trimIndent()

    fun photoRequest(photoCount: Int): String =
        if (photoCount == 1) "Lees het recept op deze foto." else "Lees het recept op deze $photoCount foto's."

    /** The page text fallback (spec §8.5) uses the photo instructions with page text instead of photos. */
    fun pageRequest(
        url: String,
        text: String,
    ): String = "Lees het recept van deze webpagina ($url). De zichtbare tekst van de pagina:\n\n$text"

    fun questSystem(content: Content): String =
        """
        Je bent een kookcoach voor een thuiskok. Je bedenkt een gerichte kookoefening (een quest) die uitkomt op een
        concreet, af gerecht, met een volledig recept.

        - Schrijf in het Nederlands, coachend en motiverend.
        - Houd je strikt aan elke opgegeven parameter (ingrediënt, soort, moeilijkheid, techniek, keuken, tijd).
        - De soort quest bepaalt wat er uitkomt: een saus-quest levert een saus op, geen hele maaltijd. Een component is
          een bouwsteen zoals een pickle, crumble, gearomatiseerde olie of dressing.
        - "Klein & eenvoudig" betekent weinig ingrediënten die je meestal in huis hebt.
        - Bij "Verras me" kies je een groente uit de lijst van groenten in het seizoen en geef je haar id terug.
        - Het recept is bruikbaar: geen vage stappen, hoeveelheden in metrische eenheden.
        - Oefen vooral de opgegeven focus.
        - Herhaal geen quest uit de lijst met eerdere quests.
        - De tijdsinschatting is in hele minuten.

        ${vocabulary(content)}
        """.trimIndent()
}
