package nl.potentplum.mise.ingest

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigInteger
import kotlin.math.ceil

/**
 * Schema.org JSON-LD recipe extractor: a faithful port of `web/lib/recipeIngestion.ts@e76778c` (spec §10). Every
 * function reproduces the golden cases in `data/fixtures/recipe-ingestion/golden.json`. The default section names
 * ("Ingredients", "Instructions") stay byte-identical; the mapping to Room turns them into untitled groups.
 */
object JsonLdRecipeParser {
    const val DEFAULT_INGREDIENTS = "Ingredients"
    const val DEFAULT_INSTRUCTIONS = "Instructions"

    private val json = Json { isLenient = false }

    // ── ISO 8601 durations ───────────────────────────────────────────────────

    private val DURATION =
        Regex(
            "^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?$",
            RegexOption.IGNORE_CASE,
        )

    /** "PT1H15M" → 75; seconds round up to the next minute; absent, zero or invalid → null. */
    fun parseIso8601Duration(value: String?): Int? {
        if (value.isNullOrEmpty()) return null
        val match = DURATION.find(value) ?: return null

        fun group(index: Int) = match.groupValues[index].ifEmpty { "0" }
        val total =
            group(3).toLong() * 1440 + group(4).toLong() * 60 + group(5).toLong() + ceil(group(6).toDouble() / 60).toLong()
        return if (total == 0L) null else total.toInt()
    }

    // ── Text ─────────────────────────────────────────────────────────────────

    private val BLOCK_CLOSERS = Regex("</(p|li|td|th|div|h[1-6]|section|article)>", RegexOption.IGNORE_CASE)
    private val BREAKS = Regex("<br[${Js.WHITESPACE}]*/?>[${Js.WHITESPACE}]*", RegexOption.IGNORE_CASE)
    private val TAGS = Regex("<[^>]+>")
    private val DECIMAL_ENTITY = Regex("&#(\\d+);")
    private val HEX_ENTITY = Regex("&#x([0-9a-f]+);", RegexOption.IGNORE_CASE)
    private val WHITESPACE_RUN = Regex("[${Js.WHITESPACE}]+")

    /** Removes tags (block closers and `<br>` become a space) and decodes common entities. */
    fun stripHtml(text: String): String {
        var result = BLOCK_CLOSERS.replace(text, " ")
        result = BREAKS.replace(result, " ")
        result = TAGS.replace(result, "")
        result =
            result
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
        result = DECIMAL_ENTITY.replace(result) { fromCharCode(it.groupValues[1].toBigInteger()) }
        return HEX_ENTITY.replace(result) { fromCharCode(it.groupValues[1].toBigInteger(16)) }
    }

    /** `String.fromCharCode`: one UTF-16 unit, modulo 2^16. */
    private fun fromCharCode(code: BigInteger): String =
        code
            .mod(UTF16_UNITS)
            .toInt()
            .toChar()
            .toString()

    private val UTF16_UNITS = BigInteger.valueOf(0x10000)

    /** [stripHtml], then typographic quotes and degree signs normalised, whitespace collapsed and trimmed. */
    fun cleanText(text: String): String =
        Js.trim(
            WHITESPACE_RUN.replace(
                stripHtml(text)
                    .replace('˚', '°')
                    .replace('’', '\'')
                    .replace('‘', '\'')
                    .replace('“', '"')
                    .replace('”', '"')
                    .replace(' ', ' '),
                " ",
            ),
        )

    // ── Finding the Recipe node ──────────────────────────────────────────────

    private val PRIORITY_KEYS = listOf("@graph", "mainEntity", "mainEntityOfPage")

    /** Depth-first search for a node whose `@type` is (or includes) "Recipe"; wrapper keys are searched first. */
    fun findRecipeNode(value: JsonElement?): JsonObject? {
        when (value) {
            is JsonArray -> return value.firstNotNullOfOrNull(::findRecipeNode)
            !is JsonObject -> return null
            else -> Unit
        }
        val type = value["@type"]
        if (type == JsonPrimitive("Recipe") || (type is JsonArray && JsonPrimitive("Recipe") in type)) return value
        for (key in PRIORITY_KEYS) {
            if (key in value) findRecipeNode(value[key])?.let { return it }
        }
        for ((key, child) in value) {
            if (key in PRIORITY_KEYS || key == "@context") continue
            if (child is JsonObject || child is JsonArray) findRecipeNode(child)?.let { return it }
        }
        return null
    }

    private val LD_JSON =
        Regex(
            "<script[^>]+type[${Js.WHITESPACE}]*=[${Js.WHITESPACE}]*[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>",
            RegexOption.IGNORE_CASE,
        )

    /** Every parseable `application/ld+json` block; malformed blocks are skipped silently. */
    fun extractJsonLdBlocks(html: String): List<JsonElement> =
        LD_JSON.findAll(html).mapNotNull { runCatching { json.parseToJsonElement(it.groupValues[1]) }.getOrNull() }.toList()

    // ── Ingredients ──────────────────────────────────────────────────────────

    private val DIGIT = Regex("\\d")

    /** A header ends with ":" or is all caps without digits ("DRESSING", "FOR THE SAUCE"). */
    fun isIngredientHeader(item: String): Boolean {
        val trimmed = Js.trim(item)
        if (trimmed.isEmpty()) return false
        if (trimmed.endsWith(":")) return true
        return trimmed == trimmed.uppercase() && !DIGIT.containsMatchIn(trimmed) && trimmed.length > 1
    }

    private fun sectionName(raw: String): String = Js.trim(Js.trim(raw).removeSuffix(":"))

    /** Splits a flat `recipeIngredient` list into sections at header lines. */
    fun parseIngredientSections(ingredients: List<String>): List<IngredientSection> {
        val sections = mutableListOf<IngredientSection>()
        var name = DEFAULT_INGREDIENTS
        var items = mutableListOf<IngredientItem>()
        for (item in ingredients) {
            if (isIngredientHeader(item)) {
                if (items.isNotEmpty()) {
                    sections += IngredientSection(name, items)
                    items = mutableListOf()
                }
                name = sectionName(item)
            } else {
                items += IngredientItem(cleanText(item))
            }
        }
        sections += IngredientSection(name, items)
        return sections
    }

    // ── Instructions ─────────────────────────────────────────────────────────

    private fun isHowToSection(item: JsonElement): Boolean = item is JsonObject && item["@type"] == JsonPrimitive("HowToSection")

    private fun stepText(item: JsonElement): String {
        if (Js.isString(item)) return (item as JsonPrimitive).content
        if (item is JsonObject) {
            item["text"]?.takeIf(Js::isString)?.let { return (it as JsonPrimitive).content }
            item["name"]?.takeIf(Js::isString)?.let { return (it as JsonPrimitive).content }
        }
        return Js.string(item)
    }

    private fun steps(items: List<JsonElement>): List<InstructionStep> =
        items.mapIndexed { index, step -> InstructionStep(index + 1, cleanText(stepText(step))) }

    /**
     * `recipeInstructions` as plain strings, `HowToStep`s, `HowToSection`s (one section each) or a mix: loose steps join
     * the preceding section, and steps before the first section go to the default one.
     */
    fun parseInstructionSections(raw: JsonElement?): List<InstructionSection> {
        if (raw == null || raw == JsonNull) return emptyList()
        val items = raw as? JsonArray ?: listOf(raw)
        if (items.isEmpty()) return emptyList()
        if (items.none(::isHowToSection)) return listOf(InstructionSection(DEFAULT_INSTRUCTIONS, steps(items)))

        val result = mutableListOf<InstructionSection>()
        var name = DEFAULT_INSTRUCTIONS
        var loose = mutableListOf<JsonElement>()
        for (item in items) {
            if (isHowToSection(item)) {
                if (loose.isNotEmpty()) {
                    result += InstructionSection(name, steps(loose))
                    loose = mutableListOf()
                }
                val section = item as JsonObject
                val sectionName =
                    cleanText(section["name"].takeUnless { it == null || it == JsonNull }?.let(Js::string) ?: DEFAULT_INSTRUCTIONS)
                result += InstructionSection(sectionName, steps(section["itemListElement"] as? JsonArray ?: emptyList()))
                name = sectionName
            } else {
                loose += item
            }
        }
        if (loose.isNotEmpty()) result += InstructionSection(name, steps(loose))
        return result
    }

    // ── Metadata ─────────────────────────────────────────────────────────────

    private fun nutrition(raw: JsonElement?): NutritionInfo {
        if (raw !is JsonObject) return NutritionInfo(null, null, null, null)

        fun field(key: String): String? {
            val value = raw[key]
            if (value == null || value == JsonNull) return null
            return Js.trim(Js.string(value)).takeUnless { it.isEmpty() || it == "null" }
        }
        return NutritionInfo(field("calories"), field("proteinContent"), field("carbohydrateContent"), field("fatContent"))
    }

    private val FIRST_NUMBER = Regex("(\\d+)")

    private fun servings(raw: JsonElement?): Int? {
        if (!Js.truthy(raw)) return null
        return FIRST_NUMBER
            .find(Js.string(raw))
            ?.groupValues
            ?.get(1)
            ?.toBigInteger()
            ?.toInt()
    }

    private fun stringOrArray(value: JsonElement?): StringOrArray =
        when (value) {
            null, JsonNull -> StringOrArray.EMPTY
            is JsonArray -> StringOrArray(value.map(Js::string), isArray = true)
            else -> StringOrArray(listOf(Js.string(value)), isArray = false)
        }

    private fun stringList(value: JsonElement?): List<String> =
        when (value) {
            null, JsonNull -> {
                emptyList()
            }

            is JsonArray -> {
                value.map(Js::string).filter { it.isNotEmpty() }
            }

            else -> {
                val text = Js.trim(Js.string(value))
                if (text.isEmpty()) emptyList() else text.split(",").map(Js::trim).filter { it.isNotEmpty() }
            }
        }

    private fun nonBlank(value: JsonElement?): String? = value?.let { Js.trim(Js.string(it)).ifEmpty { null } }

    private fun image(value: JsonElement?): String? =
        when (value) {
            null, JsonNull -> {
                null
            }

            is JsonArray -> {
                // As in the original: the first non-empty string wins, even when it trims to nothing.
                val first = value.firstOrNull { Js.truthy(it) && Js.isString(it) }
                if (first != null) Js.trim(Js.string(first)).ifEmpty { null } else value.firstOrNull { it is JsonObject }?.let(::image)
            }

            is JsonObject -> {
                (value["url"].orNull() ?: value["contentUrl"].orNull())?.takeIf(Js::truthy)?.let(::nonBlank)
            }

            is JsonPrimitive -> {
                if (value.isString) Js.trim(value.content).ifEmpty { null } else null
            }
        }

    private fun author(value: JsonElement?): String? =
        when (value) {
            null, JsonNull -> {
                null
            }

            is JsonArray -> {
                value
                    .mapNotNull(::author)
                    .filter { it.isNotEmpty() }
                    .ifEmpty { null }
                    ?.joinToString(", ")
            }

            is JsonObject -> {
                value["name"]?.takeIf(Js::truthy)?.let(::nonBlank)
            }

            is JsonPrimitive -> {
                if (value.isString) Js.trim(value.content).ifEmpty { null } else null
            }
        }

    private fun rating(value: JsonElement?): AggregateRating? {
        if (value !is JsonObject) return null

        fun number(key: String): Double? = value[key]?.takeUnless { it == JsonNull }?.let(Js::parseFloat)
        val ratingValue = number("ratingValue")
        val ratingCount = number("ratingCount") ?: number("reviewCount")
        return if (ratingValue == null && ratingCount == null) null else AggregateRating(ratingValue, ratingCount)
    }

    private fun video(value: JsonElement?): String? =
        when (value) {
            null, JsonNull -> {
                null
            }

            is JsonArray -> {
                value.firstOrNull { it is JsonObject }?.let(::video)
            }

            is JsonObject -> {
                (value["contentUrl"].orNull() ?: value["embedUrl"].orNull() ?: value["url"].orNull())?.takeIf(Js::truthy)?.let(::nonBlank)
            }

            is JsonPrimitive -> {
                if (value.isString) Js.trim(value.content).ifEmpty { null } else null
            }
        }

    /** JavaScript `??`: null and missing fall through. */
    private fun JsonElement?.orNull(): JsonElement? = takeUnless { it == null || it == JsonNull }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Parses a page's HTML into a [RecipeOutput]. WPRM ingredients (with metric text) win over `recipeIngredient`.
     *
     * @throws RecipeIngestionException with [RecipeIngestionException.UNSUPPORTED] when no Recipe node is found.
     */
    fun ingestRecipeFromHtml(html: String): RecipeOutput {
        val recipe =
            extractJsonLdBlocks(html).firstNotNullOfOrNull(::findRecipeNode)
                ?: throw RecipeIngestionException(
                    RecipeIngestionException.UNSUPPORTED,
                    "No valid Schema.org Recipe node found in any JSON-LD block on this page.",
                )

        val schemaIngredients = parseIngredientSections((recipe["recipeIngredient"] as? JsonArray)?.map(Js::string).orEmpty())
        val wprm = WprmExtractor.extractIngredients(html)
        val hasMetric = wprm != null && wprm.any { section -> section.ingredients.any { it.textMetric != null } }

        return RecipeOutput(
            title = cleanText(recipe["name"].orNull()?.let(Js::string) ?: ""),
            description = cleanText(recipe["description"].orNull()?.let(Js::string) ?: ""),
            prepTimeMinutes = parseIso8601Duration(recipe["prepTime"].orNull()?.let(Js::string)),
            cookTimeMinutes = parseIso8601Duration(recipe["cookTime"].orNull()?.let(Js::string)),
            totalTimeMinutes = parseIso8601Duration(recipe["totalTime"].orNull()?.let(Js::string)),
            servings = servings(recipe["recipeYield"]),
            cuisine = stringOrArray(recipe["recipeCuisine"]),
            category = stringOrArray(recipe["recipeCategory"]),
            keywords = stringList(recipe["keywords"]),
            image = image(recipe["image"]),
            author = author(recipe["author"]),
            suitableForDiet = stringList(recipe["suitableForDiet"]),
            datePublished = recipe["datePublished"]?.takeIf(Js::truthy)?.let(::nonBlank),
            aggregateRating = rating(recipe["aggregateRating"]),
            video = video(recipe["video"]),
            nutrition = nutrition(recipe["nutrition"]),
            ingredientSections = wprm ?: schemaIngredients,
            instructionSections = parseInstructionSections(recipe["recipeInstructions"]),
            unitSystems = if (hasMetric) UnitSystems(hasMetric = true) else null,
        )
    }
}
