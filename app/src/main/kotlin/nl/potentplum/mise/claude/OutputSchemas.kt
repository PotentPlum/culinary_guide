package nl.potentplum.mise.claude

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.model.FacetKey

/**
 * Output schemas built at runtime from the content assets, so enums always match (spec §8.6): technique ids (skills
 * only), facet values, unit ids and in-season vegetable ids. Within the structured-output subset: every object has
 * `additionalProperties: false` and all properties required; optional values are nullable; no numeric bounds.
 */
class OutputSchemas(
    private val content: Content,
) {
    private val unitIds = content.units.map { it.id }
    private val skillIds = content.skills.map { it.id }

    private fun options(key: FacetKey) = content.facet(key).options.map { it.value }

    /** `{ text, quantity|null, quantityMax|null, unit|null, name|null, note|null }`; [withText] false for enrichment. */
    fun ingredientItem(withText: Boolean = true): JsonObject =
        obj(
            buildMap {
                if (withText) put("text", string())
                put("quantity", nullable("number"))
                put("quantityMax", nullable("number"))
                put("unit", nullableEnum(unitIds))
                put("name", nullable("string"))
                put("note", nullable("string"))
            },
        )

    /** cuisine[], dishType (exactly one), mainIngredient[], techniques[]. */
    private fun tags(withDishType: Boolean = true): JsonObject =
        obj(
            buildMap {
                put("cuisine", array(enumOf(options(FacetKey.CUISINE))))
                if (withDishType) put("dishType", enumOf(options(FacetKey.DISH_TYPE)))
                put("mainIngredient", array(enumOf(options(FacetKey.MAIN_INGREDIENT))))
                put("techniques", array(enumOf(skillIds)))
            },
        )

    /** Enrichment (spec §8.4): one parse per ingredient line by its index, plus the tags. */
    fun enrichment(): JsonObject =
        obj(
            mapOf(
                "ingredients" to array(obj(mapOf("index" to integer()) + ingredientItem(withText = false).properties())),
                "tags" to tags(),
            ),
        )

    /** A recipe read from photos or page text (spec §8.6). */
    fun pageRecipe(): JsonObject =
        obj(
            mapOf(
                "title" to string(),
                "description" to string(),
                "sourceLabel" to nullable("string"),
                "servings" to nullable("integer"),
                "prepMinutes" to nullable("integer"),
                "cookMinutes" to nullable("integer"),
                "totalMinutes" to nullable("integer"),
                "ingredientGroups" to array(obj(mapOf("name" to nullable("string"), "items" to array(ingredientItem())))),
                "stepGroups" to array(obj(mapOf("name" to nullable("string"), "steps" to array(string())))),
                "extraFields" to array(obj(mapOf("label" to string(), "value" to string()))),
                "tags" to tags(),
            ),
        )

    /** A quest with its recipe (spec §8.6); [inSeasonVegIds] are the ids "Verras me" may choose from. */
    fun quest(inSeasonVegIds: List<String>): JsonObject =
        obj(
            mapOf(
                "questText" to string(),
                "skill" to string(),
                "difficulty" to enumOf(listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")),
                "timeEstimateMinutes" to integer(),
                "why" to string(),
                "dishType" to enumOf(options(FacetKey.DISH_TYPE)),
                "chosenIngredient" to string(),
                "chosenVegId" to nullableEnum(inSeasonVegIds),
                "tags" to tags(withDishType = false),
                "recipe" to
                    obj(
                        mapOf(
                            "title" to string(),
                            "servings" to integer(),
                            "ingredientGroups" to array(obj(mapOf("name" to nullable("string"), "items" to array(ingredientItem())))),
                            "steps" to array(obj(mapOf("text" to string()))),
                            "tips" to array(string()),
                        ),
                    ),
            ),
        )

    companion object {
        fun string(): JsonObject = buildJsonObject { put("type", "string") }

        fun integer(): JsonObject = buildJsonObject { put("type", "integer") }

        fun nullable(type: String): JsonObject = JsonObject(mapOf("type" to JsonArray(listOf(JsonPrimitive(type), JsonPrimitive("null")))))

        fun enumOf(values: List<String>): JsonObject =
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("string"),
                    "enum" to JsonArray(values.map(::JsonPrimitive)),
                ),
            )

        /** A value from [values] or null; just null when there are no values (an empty enum is invalid). */
        fun nullableEnum(values: List<String>): JsonObject =
            if (values.isEmpty()) {
                JsonObject(mapOf("type" to JsonPrimitive("null")))
            } else {
                JsonObject(mapOf("anyOf" to JsonArray(listOf(enumOf(values), JsonObject(mapOf("type" to JsonPrimitive("null")))))))
            }

        fun array(items: JsonObject): JsonObject = JsonObject(mapOf("type" to JsonPrimitive("array"), "items" to items))

        fun obj(properties: Map<String, JsonElement>): JsonObject =
            JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(properties),
                    "required" to JsonArray(properties.keys.map(::JsonPrimitive)),
                    "additionalProperties" to JsonPrimitive(false),
                ),
            )

        private fun JsonObject.properties(): Map<String, JsonElement> = (this["properties"] as JsonObject).toMap()
    }
}
