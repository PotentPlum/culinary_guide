package nl.potentplum.mise.claude

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputSchemasTest {
    private val content = TestContent.content
    private val schemas = OutputSchemas(content)

    /** Every object in [schema] closes its properties and requires all of them; no numeric bounds anywhere (spec §8.6). */
    private fun assertSubset(
        schema: JsonElement,
        path: String = "$",
    ) {
        when (schema) {
            is JsonObject -> {
                if (schema["type"] == JsonPrimitive("object")) {
                    assertEquals(path, JsonPrimitive(false), schema["additionalProperties"])
                    val properties = schema["properties"]!!.jsonObject.keys
                    assertEquals(path, properties, schema["required"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
                }
                listOf("minimum", "maximum", "minItems", "maxItems", "minLength").forEach { assertFalse("$path.$it", it in schema) }
                schema.forEach { (key, value) -> assertSubset(value, "$path.$key") }
            }

            is JsonArray -> {
                schema.forEach { assertSubset(it, path) }
            }

            else -> {
                Unit
            }
        }
    }

    @Test
    fun `every schema stays within the structured-output subset`() {
        assertSubset(schemas.enrichment())
        assertSubset(schemas.pageRecipe())
        assertSubset(schemas.quest(listOf("pumpkin", "kale")))
    }

    @Test
    fun `enums come from the content assets`() {
        val item = schemas.ingredientItem()["properties"]!!.jsonObject
        val unitEnum =
            item["unit"]!!
                .jsonObject["anyOf"]!!
                .jsonArray[0]
                .jsonObject["enum"]!!
                .jsonArray
        assertEquals(content.units.size, unitEnum.size)
        val tags =
            schemas
                .pageRecipe()["properties"]!!
                .jsonObject["tags"]!!
                .jsonObject["properties"]!!
                .jsonObject
        val techniques =
            tags["techniques"]!!
                .jsonObject["items"]!!
                .jsonObject["enum"]!!
                .jsonArray
                .map { it.jsonPrimitive.content }
        assertEquals(content.skills.map { it.id }, techniques)
        assertFalse("koekenpan" in techniques)
    }

    @Test
    fun `verras me can only choose an in-season vegetable, or none`() {
        val quest = schemas.quest(listOf("pumpkin"))["properties"]!!.jsonObject
        val choice = quest["chosenVegId"]!!.jsonObject["anyOf"]!!.jsonArray
        assertEquals(listOf("pumpkin"), choice[0].jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content })
        val empty = OutputSchemas(content).quest(emptyList())["properties"]!!.jsonObject["chosenVegId"]!!.jsonObject
        assertEquals(JsonPrimitive("null"), empty["type"])
    }

    @Test
    fun `quests have no checkpoints`() {
        val keys = schemas.quest(emptyList())["properties"]!!.jsonObject.keys
        assertTrue(keys.none { it.contains("checkpoint", ignoreCase = true) || it.contains("criteria", ignoreCase = true) })
    }
}
