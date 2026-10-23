package nl.potentplum.mise.ingest

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import java.math.BigDecimal

/**
 * The few JavaScript semantics the parser port relies on, so it behaves exactly like the TypeScript original:
 * `String(x)`, truthiness, `parseFloat`, and JavaScript's (Unicode) notion of whitespace.
 */
internal object Js {
    /** JavaScript `\s` and `String.prototype.trim()` whitespace. */
    const val WHITESPACE = "\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF"
    private val TRIM = Regex("^[$WHITESPACE]+|[$WHITESPACE]+$")
    private val FLOAT = Regex("^[+-]?(?:Infinity|\\d+\\.?\\d*(?:[eE][+-]?\\d+)?|\\.\\d+(?:[eE][+-]?\\d+)?)")

    fun trim(text: String): String = TRIM.replace(text, "")

    /** `String(value)` for JSON values. */
    fun string(value: JsonElement?): String =
        when (value) {
            null, JsonNull -> {
                "null"
            }

            is JsonPrimitive -> {
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.content
                    else -> number(value.doubleOrNull ?: return value.content)
                }
            }

            is JsonArray -> {
                value.joinToString(",") { if (it == JsonNull) "" else string(it) }
            }

            is JsonObject -> {
                "[object Object]"
            }
        }

    /** JavaScript's shortest round-trip number formatting for the common cases. */
    fun number(value: Double): String =
        when {
            value.isNaN() -> "NaN"
            value.isInfinite() -> if (value > 0) "Infinity" else "-Infinity"
            value == Math.rint(value) && kotlin.math.abs(value) < 1e21 -> BigDecimal(value).toBigInteger().toString()
            else -> value.toString()
        }

    /** JavaScript truthiness of a JSON value; [missing] values (`undefined`) are falsy. */
    fun truthy(value: JsonElement?): Boolean =
        when (value) {
            null, JsonNull -> {
                false
            }

            is JsonPrimitive -> {
                when {
                    value.isString -> value.content.isNotEmpty()
                    value.booleanOrNull != null -> value.booleanOrNull!!
                    else -> value.doubleOrNull.let { it != null && it != 0.0 && !it.isNaN() }
                }
            }

            else -> {
                true
            }
        }

    /** `parseFloat(String(value))`, or null for NaN. */
    fun parseFloat(value: JsonElement): Double? {
        val match = FLOAT.find(trim(string(value)))?.value ?: return null
        return when {
            match.endsWith("Infinity") -> if (match.startsWith("-")) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
            else -> match.toDouble()
        }
    }

    fun isObject(value: JsonElement?): Boolean = value is JsonObject

    /** `typeof value === "string"`. */
    fun isString(value: JsonElement?): Boolean = value is JsonPrimitive && value.isString
}
