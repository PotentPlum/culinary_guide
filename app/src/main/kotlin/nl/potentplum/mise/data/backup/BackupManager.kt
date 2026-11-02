package nl.potentplum.mise.data.backup

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import nl.potentplum.mise.BuildConfig
import nl.potentplum.mise.core.util.IoDispatcher
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.MiseDatabase
import nl.potentplum.mise.data.db.dao.BackupDao
import nl.potentplum.mise.data.db.dao.RecipeDao
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.repo.RecipeRows
import nl.potentplum.mise.data.repo.toRecipe
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.ingest.ImageStore
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class RestoreMode {
    /** By id; a recipe with a newer `updatedAt` wins, everything else is only added (spec §13.2). */
    MERGE,

    /** Wipes recipes, quests, attempts and the shopping list first. */
    REPLACE,
}

class BackupException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {
    enum class Reason {
        /** Not a zip, or no `backup.json` of format `mise-backup`. */
        NOT_A_BACKUP,

        /** Made by a newer Mise than this one. */
        NEWER_VERSION,

        /** A `backup.json` that doesn't parse. */
        CORRUPT,
    }
}

/** A backup read from a zip and unpacked into a temporary folder, ready for its summary and [BackupManager.restore]. */
class ReadBackup internal constructor(
    val file: BackupFile,
    internal val dir: File,
) {
    /** Library recipes; hidden quest recipes are counted as quests. */
    val recipeCount: Int get() = file.recipes.count { it.inLibrary }
    val questCount: Int get() = file.quests.size
    val attemptCount: Int get() = file.attempts.size

    internal fun entry(path: String?): File? = path?.takeIf(ENTRY::matches)?.let { File(dir, it) }?.takeIf { it.isFile }
}

data class RestoreResult(
    val recipes: Int,
    val quests: Int,
    val attempts: Int,
    /** Restored library recipes whose ingredients are not all structured: offer "Ingrediënten verrijken" (spec §13.2). */
    val needsEnrichment: Int,
)

private val ENTRY = Regex("""backup\.json|(images|journal)/[A-Za-z0-9_-]{1,64}\.jpg""")

/**
 * Export and restore of `Mise-backup-*.zip` (spec §13): `backup.json` plus recipe images and journal photos. The API
 * key and Keystore material are never part of it: they live in a separate store this class can't reach, and
 * `claude_usage` is left out.
 */
@Singleton
class BackupManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val db: MiseDatabase,
        private val backupDao: BackupDao,
        private val recipeDao: RecipeDao,
        private val images: ImageStore,
        private val settings: SettingsRepository,
        private val clock: MiseClock,
        @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    ) {
        /** "Mise-backup-20261102-1430.zip". */
        fun fileName(): String = "Mise-backup-${clock.zonedNow().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}.zip"

        /** Everything as a [BackupFile] (image paths are set only for files that exist). */
        suspend fun snapshot(): BackupFile =
            withContext(dispatcher) {
                val recipes = backupDao.recipeIds().mapNotNull { id -> recipeDao.getGraph(id)?.toRecipe() }
                val current = settings.current()
                BackupFile(
                    format = BACKUP_FORMAT,
                    exportedAt = clock.nowMillis(),
                    appVersion = BuildConfig.VERSION_NAME,
                    recipes = recipes.map { it.toBackup(hasImage = it.imagePath != null && images.recipeImage(it.id).isFile) },
                    quests = backupDao.quests().map { it.toBackup() },
                    attempts =
                        backupDao.attempts().map {
                            it.toBackup(
                                hasPhoto = it.photoPath != null && images.journalPhoto(it.id).isFile,
                            )
                        },
                    shoppingItems = backupDao.shoppingItems().map { it.toBackup() },
                    settings =
                        BackupSettings(
                            quality = current.quality,
                            seasonalNotificationsEnabled = current.seasonalNotificationsEnabled,
                            librarySort = current.librarySort,
                            showQuestCard = current.showQuestCard,
                        ),
                )
            }

        /** Writes the zip to [out] (a SAF document or a share file); the caller closes the stream. */
        suspend fun export(out: OutputStream): BackupFile {
            val backup = snapshot()
            withContext(dispatcher) {
                val zip = ZipOutputStream(out.buffered())
                zip.putNextEntry(ZipEntry("backup.json"))
                zip.write(JSON.encodeToString(BackupFile.serializer(), backup).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                backup.recipes.forEach { recipe -> recipe.image?.let { zip.addFile(it, images.recipeImage(recipe.id)) } }
                backup.attempts.forEach { attempt -> attempt.photo?.let { zip.addFile(it, images.journalPhoto(attempt.id)) } }
                zip.finish()
                zip.flush()
            }
            return backup
        }

        /** Unpacks and validates a backup zip; throws [BackupException]. Call [discard] or [restore] afterwards. */
        suspend fun read(input: InputStream): ReadBackup =
            withContext(dispatcher) {
                val dir = File(context.cacheDir, "restore/${UUID.randomUUID()}").apply { mkdirs() }
                try {
                    ZipInputStream(input.buffered()).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            // Only known names: nothing can be written outside the folder (zip slip).
                            if (!entry.isDirectory && ENTRY.matches(entry.name)) {
                                val target = File(dir, entry.name)
                                target.parentFile?.mkdirs()
                                target.outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    val json = File(dir, "backup.json")
                    if (!json.isFile) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
                    ReadBackup(parse(json.readText(Charsets.UTF_8)), dir)
                } catch (e: Exception) {
                    dir.deleteRecursively()
                    throw when (e) {
                        is BackupException -> e
                        is java.io.IOException -> BackupException(BackupException.Reason.NOT_A_BACKUP, e)
                        else -> e
                    }
                }
            }

        fun discard(backup: ReadBackup) {
            backup.dir.deleteRecursively()
        }

        /** Restores [backup] in one database transaction (spec §13.2), then removes its temporary folder. */
        suspend fun restore(
            backup: ReadBackup,
            mode: RestoreMode,
        ): RestoreResult =
            withContext(dispatcher) {
                try {
                    val result = db.withTransaction { write(backup, mode) }
                    if (mode == RestoreMode.REPLACE) backup.file.settings?.let { applySettings(it) }
                    result
                } finally {
                    discard(backup)
                }
            }

        private suspend fun write(
            backup: ReadBackup,
            mode: RestoreMode,
        ): RestoreResult {
            val file = backup.file
            if (mode == RestoreMode.REPLACE) {
                backupDao.deleteShoppingItems()
                backupDao.deleteAttempts()
                backupDao.deleteQuests()
                backupDao.deleteRecipes()
            }

            val versions = backupDao.recipeVersions().associate { it.id to it.updatedAt }
            val recipes = file.recipes.filter { incoming -> versions[incoming.id]?.let { incoming.updatedAt > it } ?: true }
            recipes.forEach { incoming ->
                val imagePath =
                    backup.entry(incoming.image)?.let { copy(it, images.recipeImage(incoming.id)) }
                        ?: images.recipeImage(incoming.id).takeIf { mode == RestoreMode.MERGE && it.isFile }?.absolutePath
                val rows = RecipeRows(incoming.toRecipe(imagePath))
                recipeDao.replaceGraph(
                    rows.recipe,
                    rows.ingredientGroups,
                    rows.ingredients,
                    rows.stepGroups,
                    rows.steps,
                    rows.extraFields,
                    rows.tags,
                )
            }
            val recipeIds = backupDao.recipeIds().toSet()

            val localQuests = backupDao.quests().associateBy { it.id }
            val quests =
                file.quests.filter { incoming ->
                    val local = localQuests[incoming.id]
                    incoming.recipeId in recipeIds &&
                        (local == null || (local.status == QuestStatus.OPEN && incoming.status == QuestStatus.COMPLETED))
                }
            backupDao.upsertQuests(quests.map { it.toEntity() })
            val questIds = backupDao.quests().mapTo(mutableSetOf()) { it.id }

            val localAttempts = backupDao.attempts().mapTo(mutableSetOf()) { it.id }
            val attempts = file.attempts.filter { it.id !in localAttempts }
            backupDao.upsertAttempts(
                attempts.map { incoming ->
                    val photoPath = backup.entry(incoming.photo)?.let { copy(it, images.journalPhoto(incoming.id)) }
                    incoming.toEntity(photoPath, incoming.recipeId?.takeIf { it in recipeIds }, incoming.questId?.takeIf { it in questIds })
                },
            )

            val localItems = backupDao.shoppingItems().mapTo(mutableSetOf()) { it.id }
            backupDao.upsertShoppingItems(
                file.shoppingItems.filter { it.id !in localItems }.map { it.toEntity(it.recipeId?.takeIf { id -> id in recipeIds }) },
            )

            val needsEnrichment =
                recipes.count { recipe ->
                    recipe.inLibrary && recipe.ingredientGroups.any { group -> group.items.any { it.parseSource == ParseSource.NONE } }
                }
            return RestoreResult(recipes.size, quests.size, attempts.size, needsEnrichment)
        }

        private suspend fun applySettings(restored: BackupSettings) {
            restored.quality?.let { settings.setQuality(it) }
            restored.seasonalNotificationsEnabled?.let { settings.setSeasonalNotificationsEnabled(it) }
            restored.librarySort?.let { settings.setLibrarySort(it) }
            restored.showQuestCard?.let { settings.setShowQuestCard(it) }
        }

        private fun copy(
            from: File,
            to: File,
        ): String {
            to.parentFile?.mkdirs()
            from.copyTo(to, overwrite = true)
            return to.absolutePath
        }

        private fun ZipOutputStream.addFile(
            name: String,
            source: File,
        ) {
            if (!source.isFile) return
            putNextEntry(ZipEntry(name))
            source.inputStream().use { it.copyTo(this) }
            closeEntry()
        }

        companion object {
            val JSON =
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                }

            /** Parses and checks `backup.json`; throws [BackupException]. */
            fun parse(text: String): BackupFile {
                val file =
                    try {
                        JSON.decodeFromString(BackupFile.serializer(), text)
                    } catch (e: SerializationException) {
                        throw BackupException(BackupException.Reason.CORRUPT, e)
                    } catch (e: IllegalArgumentException) {
                        throw BackupException(BackupException.Reason.CORRUPT, e)
                    }
                if (file.format != BACKUP_FORMAT) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
                if (file.schemaVersion > BACKUP_SCHEMA_VERSION) throw BackupException(BackupException.Reason.NEWER_VERSION)
                return file
            }
        }
    }
