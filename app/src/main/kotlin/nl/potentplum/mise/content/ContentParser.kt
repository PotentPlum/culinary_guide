package nl.potentplum.mise.content

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.potentplum.mise.content.model.ArchetypesFile
import nl.potentplum.mise.content.model.DisciplinesFile
import nl.potentplum.mise.content.model.FacetsFile
import nl.potentplum.mise.content.model.TechniquesFile
import nl.potentplum.mise.content.model.VegetablesFile

class UnsupportedContentException(
    message: String,
) : IllegalStateException(message)

/**
 * Parses the content files. In strict mode (debug builds and tests) unknown keys and unsupported `schemaVersion`s throw;
 * otherwise unknown keys are ignored and a version mismatch is reported through [onWarning] (spec §7.1).
 */
class ContentParser(
    private val strict: Boolean,
    private val onWarning: (String) -> Unit = {},
) {
    private val json =
        Json {
            ignoreUnknownKeys = !strict
        }

    /** [read] returns the text of an asset file by name, e.g. `vegetables.json`. */
    fun parse(read: (String) -> String): Content =
        Content(
            vegetables = decode(read, VEGETABLES, VegetablesFile.serializer()).vegetables,
            techniques = decode(read, TECHNIQUES, TechniquesFile.serializer()).techniques,
            facets = decode(read, FACETS, FacetsFile.serializer()).facets,
            disciplines = decode(read, DISCIPLINES, DisciplinesFile.serializer()).disciplines,
            archetypes = decode(read, ARCHETYPES, ArchetypesFile.serializer()).archetypes,
        )

    private fun <T> decode(
        read: (String) -> String,
        file: String,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val text = read(file)
        val version =
            json
                .parseToJsonElement(text)
                .jsonObject["schemaVersion"]
                ?.jsonPrimitive
                ?.int
        if (version != SUPPORTED_SCHEMA_VERSION) {
            val message = "$file has schemaVersion $version; this build supports $SUPPORTED_SCHEMA_VERSION"
            if (strict) throw UnsupportedContentException(message) else onWarning(message)
        }
        return json.decodeFromString(deserializer, text)
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val VEGETABLES = "vegetables.json"
        const val TECHNIQUES = "techniques.json"
        const val FACETS = "facets.json"
        const val DISCIPLINES = "disciplines.json"
        const val ARCHETYPES = "archetypes.json"
    }
}
