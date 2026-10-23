package nl.potentplum.mise.claude

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.ExtraField
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.domain.IngredientParser

/**
 * Turns validated structured output into app data (spec §8.6). Unknown enum values are dropped (not fatal), strings
 * are trimmed, empty groups removed, a missing or invalid ingredient parse falls back to the local parse, and
 * `MANUAL` parses are never overwritten.
 */
class ResponseMapping(
    private val content: Content,
    private val parser: IngredientParser,
) {
    private val unitIds = content.units.map { it.id }.toSet()
    private val skillIds = content.skills.map { it.id }.toSet()

    private fun options(key: FacetKey) =
        content
            .facet(key)
            .options
            .map { it.value }
            .toSet()

    /** An ingredient from Claude's item, or the local parse of [text] when Claude's item has nothing usable. */
    fun ingredient(
        item: JsonObject,
        text: String,
    ): Ingredient {
        val quantity = item.number("quantity")?.takeIf { it > 0 }
        val quantityMax = item.number("quantityMax")?.takeIf { quantity != null && it > quantity }
        val unit = item.string("unit")?.takeIf { it in unitIds }
        val name = item.string("name")?.lowercase()
        val note = item.string("note")
        if (name == null && quantity == null) return Ingredient.fromLocalParse(parser.parse(text))
        return Ingredient(
            text = text,
            quantity = quantity,
            quantityMax = quantityMax,
            unit = unit,
            name = name,
            note = note,
            parseSource = ParseSource.CLAUDE,
        )
    }

    /** Tags from a `tags` object; anything outside the vocabularies is dropped. */
    fun tags(tags: JsonObject?): Set<Tag> {
        if (tags == null) return emptySet()
        return buildSet {
            tags.strings("cuisine").filter { it in options(FacetKey.CUISINE) }.forEach { add(Tag(TagKey.CUISINE, it)) }
            tags.string("dishType")?.takeIf { it in options(FacetKey.DISH_TYPE) }?.let { add(Tag(TagKey.DISH_TYPE, it)) }
            tags
                .strings(
                    "mainIngredient",
                ).filter { it in options(FacetKey.MAIN_INGREDIENT) }
                .forEach { add(Tag(TagKey.MAIN_INGREDIENT, it)) }
            tags.strings("techniques").filter { it in skillIds }.forEach { add(Tag(TagKey.TECHNIQUE, it)) }
        }
    }

    /**
     * Applies an enrichment reply (spec §8.4): every ingredient that is not `MANUAL` takes Claude's parse (or keeps its
     * local one when Claude's is unusable), and tag keys the recipe has no values for are filled in.
     */
    fun applyEnrichment(
        recipe: Recipe,
        reply: JsonObject,
    ): Recipe {
        val byIndex = reply.objects("ingredients").associateBy { it.int("index") }
        var index = 0
        val groups =
            recipe.ingredientGroups.map { group ->
                group.copy(
                    items =
                        group.items.map { ingredient ->
                            val item = byIndex[index++]
                            when {
                                ingredient.parseSource == ParseSource.MANUAL -> {
                                    ingredient
                                }

                                item == null -> {
                                    ingredient
                                }

                                else -> {
                                    ingredient(item, ingredient.text).copy(id = ingredient.id).let { parsed ->
                                        // A fallback to the local parse keeps whatever the line already had.
                                        if (parsed.parseSource == ParseSource.CLAUDE) parsed else ingredient
                                    }
                                }
                            }
                        },
                )
            }
        val existingKeys = recipe.tags.map { it.key }.toSet()
        val newTags = tags(reply["tags"] as? JsonObject).filter { it.key !in existingKeys }
        return recipe.copy(ingredientGroups = groups, tags = recipe.tags + newTags)
    }

    /** A recipe read from photos or page text; the caller sets id, source type, URL and image. */
    fun pageRecipe(
        reply: JsonObject,
        base: Recipe,
    ): Recipe =
        base.copy(
            title = reply.string("title").orEmpty(),
            description = reply.string("description").orEmpty(),
            sourceLabel = reply.string("sourceLabel") ?: base.sourceLabel,
            servings = reply.int("servings")?.takeIf { it in 1..99 },
            prepMinutes = reply.int("prepMinutes")?.takeIf { it > 0 },
            cookMinutes = reply.int("cookMinutes")?.takeIf { it > 0 },
            totalMinutes = reply.int("totalMinutes")?.takeIf { it > 0 },
            ingredientGroups =
                reply
                    .objects("ingredientGroups")
                    .map { group ->
                        IngredientGroup(
                            name = group.string("name"),
                            items = group.objects("items").mapNotNull { item -> item.string("text")?.let { ingredient(item, it) } },
                        )
                    }.filter { it.items.isNotEmpty() },
            stepGroups =
                reply
                    .objects("stepGroups")
                    .map { group -> StepGroup(name = group.string("name"), steps = group.strings("steps").map { Step(text = it) }) }
                    .filter { it.steps.isNotEmpty() },
            extraFields =
                reply.objects("extraFields").mapNotNull { field ->
                    val label = field.string("label") ?: return@mapNotNull null
                    val value = field.string("value") ?: return@mapNotNull null
                    ExtraField(label = label, value = value)
                },
            tags = tags(reply["tags"] as? JsonObject),
        )

    companion object {
        fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.contentOrNull
                ?.trim()
                ?.ifEmpty { null }

        fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull

        fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

        fun JsonObject.strings(key: String): List<String> =
            (this[key] as? JsonArray).orEmpty().mapNotNull {
                (it as? JsonPrimitive)
                    ?.takeIf { p ->
                        p.isString
                    }?.content
                    ?.trim()
                    ?.ifEmpty { null }
            }

        fun JsonObject.objects(key: String): List<JsonObject> = (this[key] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()
    }
}
