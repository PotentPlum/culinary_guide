package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Dimension
import nl.potentplum.mise.content.model.MeasureUnit

/** One ingredient line, structured where possible (spec §11.2). [parsed] false means: show and scale [text] only. */
data class ParsedIngredient(
    val text: String,
    val quantity: Double? = null,
    /** Upper bound of a range ("2–3"). */
    val quantityMax: Double? = null,
    /** A `units.json` id. */
    val unit: String? = null,
    /** Lowercase; as written by the local parser, singular when Claude parsed it. */
    val name: String? = null,
    /** "in blokjes", "à 400 g". */
    val note: String? = null,
    val parsed: Boolean = false,
)

/**
 * The offline, deterministic ingredient parser (spec §11.2): a leading quantity (integer, decimal with comma or point,
 * unicode or slash fraction, mixed number, range, or a Dutch number word), an optional unit alias, then the name, with
 * a pack size ("à 400 g"), parentheses and anything after the first comma as the note. Lines it can't read stay text.
 */
class IngredientParser(
    units: List<MeasureUnit>,
) {
    private val unitsByAlias: Map<String, MeasureUnit> = units.flatMap { unit -> unit.aliases.map { it to unit } }.toMap()

    /** Longest aliases first, so "naar smaak" wins over shorter matches and "gram" over "g". */
    private val aliases: List<String> = unitsByAlias.keys.sortedByDescending { it.length }

    fun parse(line: String): ParsedIngredient {
        val text =
            WHITESPACE
                .replace(line, " ")
                .trim()
                .removePrefix("- ")
                .removePrefix("• ")
                .removePrefix("* ")
                .trim()
        if (text.isEmpty()) return ParsedIngredient(text)

        var rest = text
        val quantity = readQuantity(rest)
        if (quantity != null) rest = rest.substring(quantity.length).trimStart()

        var unit = readUnit(rest)?.takeIf { quantity != null || it.first.dimension == Dimension.OTHER }
        if (unit != null) rest = rest.substring(unit.second).trimStart()

        val notes = mutableListOf<String>()
        PACK_SIZE.find(rest)?.let { pack ->
            notes += pack.value.trim()
            rest = rest.substring(pack.value.length).trimStart()
        }
        val commaIndex = rest.indexOf(',')
        var name = if (commaIndex >= 0) rest.substring(0, commaIndex) else rest
        val afterComma = if (commaIndex >= 0) rest.substring(commaIndex + 1).trim() else ""
        PARENTHESES.findAll(name).forEach { notes += it.groupValues[1].trim() }
        name = WHITESPACE.replace(PARENTHESES.replace(name, " "), " ").trim()
        if (afterComma.isNotEmpty()) notes += afterComma

        if (unit == null) {
            NAAR_SMAAK.find(name)?.let { match ->
                unit = unitsByAlias["naar smaak"]?.let { it to 0 }
                name = name.substring(0, match.range.first).trim()
            }
        }

        val parsed = quantity != null || unit != null || (name.isNotEmpty() && !DIGIT.containsMatchIn(text))
        if (!parsed) return ParsedIngredient(text)
        return ParsedIngredient(
            text = text,
            quantity = quantity?.min,
            quantityMax = quantity?.max,
            unit = unit?.first?.id,
            name = name.lowercase().ifEmpty { null },
            note = notes.filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { null },
            parsed = true,
        )
    }

    private data class Quantity(
        val min: Double,
        val max: Double?,
        val length: Int,
    )

    private fun readQuantity(text: String): Quantity? {
        RANGE.find(text)?.let { match ->
            val min = number(match.groupValues[1])
            val max = number(match.groupValues[2])
            if (min != null && max != null && max > min) return Quantity(min, max, match.value.length)
        }
        SINGLE_AT_START.find(text)?.let { match ->
            val value = number(match.groupValues[1]) ?: return@let
            if (value > 0) return Quantity(value, null, match.value.length)
        }
        NUMBER_WORD.find(text)?.let { match ->
            val word = match.groupValues[1].lowercase()
            val next =
                text
                    .substring(match.value.length)
                    .trimStart()
                    .substringBefore(' ')
                    .lowercase()
            if (word in ARTICLES && next in VAGUE) return null
            return Quantity(NUMBER_WORDS.getValue(word), null, match.value.length)
        }
        return null
    }

    /** The unit at the start of [text] and the length it covers, at a word boundary ("200g" works, "groene" doesn't). */
    private fun readUnit(text: String): Pair<MeasureUnit, Int>? {
        val lower = text.lowercase()
        for (alias in aliases) {
            if (!lower.startsWith(alias)) continue
            val next = lower.getOrNull(alias.length)
            if (next == null || next == ' ' || next == ',' || next == '(' || alias.endsWith('.')) {
                return unitsByAlias.getValue(alias) to alias.length
            }
        }
        return null
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private val DIGIT = Regex("\\d")
        private val PARENTHESES = Regex("\\(([^)]*)\\)")
        private val NAAR_SMAAK = Regex("\\s*\\bnaar smaak$", RegexOption.IGNORE_CASE)

        private val FRACTIONS =
            mapOf(
                '½' to 0.5,
                '⅓' to 1.0 / 3,
                '⅔' to 2.0 / 3,
                '¼' to 0.25,
                '¾' to 0.75,
                '⅕' to 0.2,
                '⅖' to 0.4,
                '⅗' to 0.6,
                '⅘' to 0.8,
                '⅙' to 1.0 / 6,
                '⅚' to 5.0 / 6,
                '⅛' to 0.125,
                '⅜' to 0.375,
                '⅝' to 0.625,
                '⅞' to 0.875,
            )
        private val FRACTION_CHARS = FRACTIONS.keys.joinToString("")

        /** Mixed number, slash fraction, number with a unicode fraction, unicode fraction, or decimal. */
        private val SINGLE = "(?:\\d+ \\d+/\\d+|\\d+/\\d+|\\d+ ?[$FRACTION_CHARS]|[$FRACTION_CHARS]|\\d+(?:[.,]\\d+)?)"

        /** Followed by a space, the end, or a glued unit ("200g"); never by another digit or a slash. */
        private const val END = "(?=$|[\\s,(]|[^\\d/.,\\s])"
        private val SINGLE_AT_START = Regex("^($SINGLE)$END")
        private val RANGE = Regex("^($SINGLE) ?(?:-|–|—|à|a|tot) ?($SINGLE)$END", RegexOption.IGNORE_CASE)

        /** "à 400 g" right after the unit: a pack size, which is part of the note and never scaled. */
        private val PACK_SIZE = Regex("^(?:à|a) \\d+(?:[.,]\\d+)? ?[a-z]+\\.?(?=$|[\\s,])", RegexOption.IGNORE_CASE)

        private val NUMBER_WORDS =
            mapOf(
                "een" to 1.0,
                "één" to 1.0,
                "twee" to 2.0,
                "drie" to 3.0,
                "vier" to 4.0,
                "vijf" to 5.0,
                "zes" to 6.0,
                "zeven" to 7.0,
                "acht" to 8.0,
                "negen" to 9.0,
                "tien" to 10.0,
                "half" to 0.5,
                "halve" to 0.5,
                "anderhalf" to 1.5,
                "anderhalve" to 1.5,
            )
        private val NUMBER_WORD = Regex("^(${NUMBER_WORDS.keys.joinToString("|")})(?=\\s)", RegexOption.IGNORE_CASE)
        private val ARTICLES = setOf("een", "één")

        /** "een paar", "een beetje": an article, not a quantity. */
        private val VAGUE = setOf("paar", "beetje", "klein", "flinke", "flink", "stuk", "of")

        /** "1,5", "1/2", "1 1/2", "1½", "½" → the number. */
        fun number(token: String): Double? {
            val t = token.trim().replace(',', '.')
            FRACTIONS[t.lastOrNull()]?.let { fraction ->
                val whole = t.dropLast(1).trim()
                return if (whole.isEmpty()) fraction else whole.toDoubleOrNull()?.plus(fraction)
            }
            if ('/' in t) {
                val parts = t.split(' ')
                val (numerator, denominator) = parts.last().split('/').map { it.toDoubleOrNull() ?: return null }
                if (denominator == 0.0) return null
                val whole = if (parts.size == 2) parts[0].toDoubleOrNull() ?: return null else 0.0
                return whole + numerator / denominator
            }
            return t.toDoubleOrNull()
        }
    }
}
