package nl.potentplum.mise.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import nl.potentplum.mise.data.db.entity.SourceMeta

/** Lists that are only ever read whole are stored as JSON text (spec §7.2). */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromSourceMeta(value: SourceMeta?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toSourceMeta(value: String?): SourceMeta? = value?.let { json.decodeFromString<SourceMeta>(it) }
}
