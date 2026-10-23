package nl.potentplum.mise.ingest

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Ingredients from WP Recipe Maker's inline `window.wprm_recipes`, which carries US amounts and a metric unit system
 * (spec §10). Port of `extractWprmIngredients` in `web/lib/recipeIngestion.ts@cf3ec39`.
 */
object WprmExtractor {
    const val SECTION_NAME = "Ingrediënten"

    private val INLINE_SCRIPT = Regex("<script(?![^>]+\\bsrc\\b)[^>]*>([\\s\\S]*?)</script>", RegexOption.IGNORE_CASE)

    // Greedy, to capture the whole nested object; `$` is the end of the script (no multiline flag in the original).
    private val ASSIGNMENT =
        Regex(
            "window\\.wprm_recipes[${Js.WHITESPACE}]*=[${Js.WHITESPACE}]*(\\{[\\s\\S]*\\})(?:[${Js.WHITESPACE}]*;|[${Js.WHITESPACE}]*\\z)",
        )

    private val json = Json

    /** One "Ingrediënten" section, or null when the page has no WPRM data or it can't be read. */
    fun extractIngredients(html: String): List<IngredientSection>? {
        if ("wprm_recipes" !in html) return null
        return try {
            for (script in INLINE_SCRIPT.findAll(html)) {
                val content = script.groupValues[1]
                if ("wprm_recipes" !in content) continue
                val assignment = ASSIGNMENT.find(content) ?: continue
                val recipes = json.parseToJsonElement(assignment.groupValues[1]) as JsonObject
                val first = recipes.values.firstOrNull() ?: return null
                val raw = (first as? JsonObject)?.get("ingredients") as? JsonArray ?: return null
                return listOf(IngredientSection(SECTION_NAME, raw.map { ingredient(it as? JsonObject ?: JsonObject(emptyMap())) }))
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun ingredient(item: JsonObject): IngredientItem {
        val amount = field(item["amount"])
        val unit = field(item["unit"])
        val name = field(item["name"])
        val notes = field(item["notes"])
        val usText = text(amount, unit, name, notes)

        val metric = ((item["unit_systems"] as? JsonObject)?.get("unit-system-2") as? JsonObject)
        val metricAmount = field(metric?.get("amount"))
        if (metricAmount.isNotEmpty()) {
            val metricText = text(metricAmount, field(metric?.get("unit")), name, notes)
            if (metricText != usText) return IngredientItem(usText, metricText)
        }
        return IngredientItem(usText)
    }

    /** `String(value ?? "").trim()`. */
    private fun field(value: JsonElement?): String = if (value == null || value == JsonNull) "" else Js.trim(Js.string(value))

    private fun text(
        amount: String,
        unit: String,
        name: String,
        notes: String,
    ): String {
        val text = listOf(amount, unit, name).filter { it.isNotEmpty() }.joinToString(" ")
        return if (notes.isEmpty()) {
            text
        } else if (text.isEmpty()) {
            notes
        } else {
            "$text, $notes"
        }
    }
}
