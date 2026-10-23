package nl.potentplum.mise.ingest

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.potentplum.mise.testing.TestResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §10 acceptance: all 104 recorded cases of the TypeScript parser, compared by JSON equality. */
class RecipeIngestionGoldenTest {
    private val json = Json { encodeDefaults = true }

    private val cases: List<JsonObject> =
        Json
            .parseToJsonElement(TestResources.read("recipe-ingestion/golden.json"))
            .jsonObject
            .getValue("cases")
            .jsonArray
            .map { it.jsonObject }

    @Test
    fun `every golden case passes`() {
        assertEquals(104, cases.size)
        val failures = cases.mapNotNull(::check)
        assertTrue("${failures.size} of ${cases.size} failed:\n" + failures.joinToString("\n\n"), failures.isEmpty())
    }

    private fun check(case: JsonObject): String? {
        val name = case.getValue("test").jsonPrimitive.content
        val expectedError =
            case["error"]
                ?.jsonObject
                ?.get("code")
                ?.jsonPrimitive
                ?.content
        val actual =
            try {
                call(case.getValue("fn").jsonPrimitive.content, case.getValue("args").jsonArray)
            } catch (e: RecipeIngestionException) {
                return if (e.code == expectedError) null else "$name: unexpected ${e.code}"
            }
        if (expectedError != null) return "$name: expected error $expectedError, got $actual"
        val expected = case["result"] ?: JsonNull
        return if (sameJson(expected, actual)) null else "$name:\n  expected $expected\n  actual   $actual"
    }

    private fun call(
        fn: String,
        args: JsonArray,
    ): JsonElement {
        val arg = args.getOrNull(0)

        fun text() = arg!!.jsonPrimitive.content
        return when (fn) {
            "parseIso8601Duration" -> {
                JsonPrimitive(JsonLdRecipeParser.parseIso8601Duration(arg?.takeUnless { it == JsonNull }?.jsonPrimitive?.content))
            }

            "cleanText" -> {
                JsonPrimitive(JsonLdRecipeParser.cleanText(text()))
            }

            "stripHtml" -> {
                JsonPrimitive(JsonLdRecipeParser.stripHtml(text()))
            }

            "extractJsonLdBlocks" -> {
                JsonArray(JsonLdRecipeParser.extractJsonLdBlocks(text()))
            }

            "findRecipeNode" -> {
                JsonLdRecipeParser.findRecipeNode(arg) ?: JsonNull
            }

            "isIngredientHeader" -> {
                JsonPrimitive(JsonLdRecipeParser.isIngredientHeader(text()))
            }

            "parseIngredientSections" -> {
                json.encodeToJsonElement(JsonLdRecipeParser.parseIngredientSections(arg!!.jsonArray.map { it.jsonPrimitive.content }))
            }

            "parseInstructionSections" -> {
                json.encodeToJsonElement(JsonLdRecipeParser.parseInstructionSections(arg))
            }

            "extractWprmIngredients" -> {
                WprmExtractor.extractIngredients(text())?.let { json.encodeToJsonElement(it) } ?: JsonNull
            }

            "ingestRecipeFromHtml" -> {
                json.encodeToJsonElement(JsonLdRecipeParser.ingestRecipeFromHtml(text()))
            }

            else -> {
                error("unknown fn $fn")
            }
        }
    }

    /** JSON equality where numbers compare by value (JavaScript has one number type: 120 == 120.0). */
    private fun sameJson(
        a: JsonElement,
        b: JsonElement,
    ): Boolean =
        when {
            a is JsonObject && b is JsonObject -> {
                a.keys == b.keys && a.keys.all { sameJson(a.getValue(it), b.getValue(it)) }
            }

            a is JsonArray && b is JsonArray -> {
                a.size == b.size && a.indices.all { sameJson(a[it], b[it]) }
            }

            a is JsonNull || b is JsonNull -> {
                a == b
            }

            a is JsonPrimitive && b is JsonPrimitive -> {
                when {
                    a.isString || b.isString -> a.isString == b.isString && a.content == b.content
                    a.booleanOrNull != null || b.booleanOrNull != null -> a.booleanOrNull == b.booleanOrNull
                    else -> a.doubleOrNull == b.doubleOrNull
                }
            }

            else -> {
                false
            }
        }
}
