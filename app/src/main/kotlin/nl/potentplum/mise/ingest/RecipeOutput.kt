package nl.potentplum.mise.ingest

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

// The parser's output, field for field the `RecipeOutput` of web/lib/recipeIngestion.ts@e76778c, so the golden
// fixtures compare by JSON equality (spec §10). Mapping to Room happens later and turns default section names into
// untitled groups.

class RecipeIngestionException(
    val code: String,
    message: String,
) : Exception(message) {
    companion object {
        const val UNSUPPORTED = "UNSUPPORTED_RECIPE_FORMAT"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class IngredientItem(
    val text: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val textMetric: String? = null,
)

@Serializable
data class IngredientSection(
    val sectionName: String,
    val ingredients: List<IngredientItem>,
)

@Serializable
data class InstructionStep(
    val stepNumber: Int,
    val text: String,
)

@Serializable
data class InstructionSection(
    val sectionName: String,
    val steps: List<InstructionStep>,
)

@Serializable
data class NutritionInfo(
    val calories: String?,
    val protein: String?,
    val carbs: String?,
    val fat: String?,
)

@Serializable
data class AggregateRating(
    val ratingValue: Double?,
    val ratingCount: Double?,
)

@Serializable
data class UnitSystems(
    val hasMetric: Boolean,
)

/** Schema.org allows a string or a list for cuisine and category; the output keeps which one it was. */
@Serializable(with = StringOrArraySerializer::class)
data class StringOrArray(
    val values: List<String>,
    val isArray: Boolean,
) {
    companion object {
        val EMPTY = StringOrArray(listOf(""), isArray = false)
    }
}

object StringOrArraySerializer : KSerializer<StringOrArray> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: StringOrArray,
    ) {
        val element = if (value.isArray) JsonArray(value.values.map(::JsonPrimitive)) else JsonPrimitive(value.values.single())
        (encoder as JsonEncoder).encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): StringOrArray =
        when (val element = (decoder as JsonDecoder).decodeJsonElement()) {
            is JsonArray -> StringOrArray(element.map { it.jsonPrimitive.content }, isArray = true)
            else -> StringOrArray(listOf(element.jsonPrimitive.content), isArray = false)
        }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RecipeOutput(
    val title: String,
    val description: String,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val totalTimeMinutes: Int?,
    val servings: Int?,
    val cuisine: StringOrArray,
    val category: StringOrArray,
    val keywords: List<String>,
    val image: String?,
    val author: String?,
    val suitableForDiet: List<String>,
    val datePublished: String?,
    val aggregateRating: AggregateRating?,
    val video: String?,
    val nutrition: NutritionInfo,
    val ingredientSections: List<IngredientSection>,
    val instructionSections: List<InstructionSection>,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val unitSystems: UnitSystems? = null,
)
