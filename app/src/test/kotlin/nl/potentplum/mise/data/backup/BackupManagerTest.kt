package nl.potentplum.mise.data.backup

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.data.db.dao.BackupDao
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity
import nl.potentplum.mise.data.db.entity.SourceMeta
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.ExtraField
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.data.settings.Quality
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.ingest.ImageStore
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.TestResources
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/** Spec §13: export → wipe → restore gives an identical database; the key is never in a backup. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class BackupManagerTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var backups: BackupManager

    @Inject lateinit var recipes: RecipeRepository

    @Inject lateinit var backupDao: BackupDao

    @Inject lateinit var images: ImageStore

    @Inject lateinit var secrets: SecretStore

    @Inject lateinit var settings: SettingsRepository

    private val key = "sk-ant-api03-SECRET-never-in-a-backup-1234"
    private val imageBytes = byteArrayOf(-1, -40, -1, 1, 2, 3, 4)
    private val photoBytes = byteArrayOf(-1, -40, -1, 9, 8, 7)

    @Before
    fun setUp() {
        hilt.inject()
    }

    private val soup =
        Recipe(
            id = "soup",
            title = "Pompoensoep",
            description = "Romig",
            sourceType = RecipeSourceType.URL,
            sourceUrl = "https://example.nl/soep",
            sourceLabel = "example.nl",
            servings = 4,
            totalMinutes = 45,
            notes = "Lekker met brood",
            isFavorite = true,
            sourceMeta = SourceMeta(ratingValue = 4.5, keywords = listOf("soep")),
            ingredientGroups =
                listOf(
                    IngredientGroup(
                        id = "g1",
                        items =
                            listOf(
                                Ingredient("i1", "1 kg pompoen", 1.0, null, "kg", "pompoen", null, ParseSource.LOCAL),
                                Ingredient("i2", "een handje salie"),
                            ),
                    ),
                    IngredientGroup(
                        id = "g2",
                        name = "Topping",
                        items = listOf(Ingredient("i3", "2-3 el pitten", 2.0, 3.0, "el", "pitten", null, ParseSource.MANUAL)),
                    ),
                ),
            stepGroups = listOf(StepGroup("s1", null, listOf(Step("st1", "Snijd."), Step("st2", "Kook.")))),
            extraFields = listOf(ExtraField("e1", "Bewaren", "3 dagen")),
            tags = setOf(Tag(TagKey.CUISINE, "Nederlands"), Tag(TagKey.TECHNIQUE, "blancheren")),
        )

    private fun seed() =
        runBlocking {
            val saved = recipes.save(soup)
            images.recipeImage(saved.id).apply { parentFile?.mkdirs() }.writeBytes(imageBytes)
            recipes.save(saved.copy(imagePath = images.recipeImage(saved.id).absolutePath))
            recipes.save(Recipe(id = "quest-recipe", title = "Gebrande prei", sourceType = RecipeSourceType.QUEST, inLibrary = false))
            backupDao.upsertQuests(
                listOf(
                    QuestEntity(
                        id = "q1",
                        createdAt = 1_000,
                        isDaily = true,
                        status = QuestStatus.COMPLETED,
                        completedAt = 2_000,
                        ingredientMode = IngredientMode.SEASONAL,
                        ingredient = "prei",
                        vegId = "leek",
                        questType = QuestType.BIJGERECHT,
                        text = "Brand prei",
                        skill = "schroeien",
                        difficulty = Difficulty.INTERMEDIATE,
                        timeEstimateMinutes = 30,
                        why = "Hoog vuur",
                        dishType = "Bijgerecht",
                        tips = listOf("Heet"),
                        recipeId = "quest-recipe",
                    ),
                ),
            )
            images.journalPhoto("a1").apply { parentFile?.mkdirs() }.writeBytes(photoBytes)
            backupDao.upsertAttempts(
                listOf(
                    AttemptEntity(
                        "a1",
                        AttemptSourceType.RECIPE,
                        "soup",
                        null,
                        5_000,
                        8,
                        10,
                        "Meer salie",
                        images.journalPhoto("a1").absolutePath,
                        listOf("blancheren"),
                        listOf("Nederlands"),
                        listOf("pumpkin"),
                        listOf("pumpkin"),
                    ),
                    AttemptEntity("a2", AttemptSourceType.QUEST, "quest-recipe", "q1", 6_000, score = 6),
                ),
            )
            backupDao.upsertShoppingItems(
                listOf(
                    ShoppingItemEntity("sh1", "1 kg pompoen", 1.0, "kg", "pompoen", "soup", false, 0, 7_000),
                    ShoppingItemEntity("sh2", "Koffiefilters", checked = true, position = 1, createdAt = 7_000),
                ),
            )
            secrets.saveClaudeKey(key)
            settings.setQuality(Quality.BEST)
        }

    private fun export(): ByteArray = ByteArrayOutputStream().also { runBlocking { backups.export(it) } }.toByteArray()

    private fun entries(zip: ByteArray): Map<String, ByteArray> =
        ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            generateSequence { input.nextEntry }.associate { it.name to input.readBytes() }
        }

    private fun wipe() =
        runBlocking {
            backupDao.deleteShoppingItems()
            backupDao.deleteAttempts()
            backupDao.deleteQuests()
            backupDao.deleteRecipes()
            images.recipeImage("soup").delete()
            images.journalPhoto("a1").delete()
            settings.setQuality(Quality.FAST)
        }

    private fun restore(
        zip: ByteArray,
        mode: RestoreMode,
    ) = runBlocking { backups.restore(backups.read(ByteArrayInputStream(zip)), mode) }

    /** The snapshot without the export time, for comparing databases. */
    private fun snapshot() = runBlocking { backups.snapshot().copy(exportedAt = 0) }

    @Test
    fun `export, wipe and restore give an identical database, images and settings`() {
        seed()
        val before = snapshot()
        val zip = export()
        wipe()
        assertTrue(snapshot().recipes.isEmpty())

        val result = restore(zip, RestoreMode.REPLACE)

        assertEquals(before, snapshot())
        assertEquals(2, result.recipes)
        assertEquals(1, result.quests)
        assertEquals(2, result.attempts)
        // The soup has an unparsed line; the hidden quest recipe isn't in the library.
        assertEquals(1, result.needsEnrichment)
        assertArrayEquals(imageBytes, images.recipeImage("soup").readBytes())
        assertArrayEquals(photoBytes, images.journalPhoto("a1").readBytes())
        assertEquals(images.recipeImage("soup").absolutePath, runBlocking { recipes.get("soup") }!!.imagePath)
        assertEquals(Quality.BEST, runBlocking { settings.current() }.quality)
    }

    @Test
    fun `the key and Keystore material are never part of a backup`() {
        seed()
        val zip = export()
        val all = entries(zip)
        assertEquals(setOf("backup.json", "images/soup.jpg", "journal/a1.jpg"), all.keys)
        val json = all.getValue("backup.json").toString(Charsets.UTF_8)
        assertFalse(json.contains(key))
        assertFalse(json.contains("sk-ant-"))
        assertFalse(json.contains("claudeKey"))
        assertFalse(String(zip, Charsets.ISO_8859_1).contains(key))
        // Day and session state stay out too.
        assertFalse(json.contains("cookSession"))
        assertFalse(json.contains("tonight"))
    }

    @Test
    fun `merge adds what is missing and lets the newer recipe win`() {
        seed()
        val zip = export()
        runBlocking {
            // Local edit after the backup: newer, so it stays.
            recipes.save(recipes.get("soup")!!.copy(title = "Pompoensoep (nieuw)"))
            backupDao.deleteAttempts()
            backupDao.deleteShoppingItems()
        }
        val result = restore(zip, RestoreMode.MERGE)
        assertEquals(0, result.recipes)
        assertEquals(2, result.attempts)
        assertEquals("Pompoensoep (nieuw)", runBlocking { recipes.get("soup") }!!.title)
        assertEquals(2, runBlocking { backupDao.shoppingItems() }.size)
        // The local image survives a merge that doesn't rewrite the recipe.
        assertTrue(images.recipeImage("soup").isFile)
    }

    @Test
    fun `merge replaces an older local recipe`() {
        seed()
        val newer =
            rewrite(export()) { file ->
                file.copy(
                    recipes =
                        file.recipes.map {
                            if (it.id ==
                                "soup"
                            ) {
                                it.copy(title = "Nieuwere soep", updatedAt = Long.MAX_VALUE / 2)
                            } else {
                                it
                            }
                        },
                )
            }
        val result = restore(newer, RestoreMode.MERGE)
        assertEquals(1, result.recipes)
        assertEquals("Nieuwere soep", runBlocking { recipes.get("soup") }!!.title)
        // Nothing was duplicated.
        assertEquals(2, runBlocking { backupDao.recipeIds() }.size)
        assertEquals(2, runBlocking { backupDao.attempts() }.size)
    }

    @Test
    fun `references to recipes or quests outside the backup are dropped, not broken`() {
        val file =
            BackupFile(
                format = BACKUP_FORMAT,
                exportedAt = 1,
                appVersion = "test",
                quests =
                    listOf(
                        BackupQuest(
                            id = "orphan",
                            createdAt = 1,
                            ingredientMode = IngredientMode.CUSTOM,
                            ingredient = "ui",
                            questType = QuestType.SAUS,
                            text = "t",
                            skill = "s",
                            difficulty = Difficulty.BEGINNER,
                            timeEstimateMinutes = 10,
                            why = "w",
                            dishType = "d",
                            recipeId = "missing",
                        ),
                    ),
                attempts = listOf(BackupAttempt("a9", AttemptSourceType.QUEST, recipeId = "missing", questId = "orphan", cookedAt = 1)),
                shoppingItems = listOf(BackupShoppingItem("s9", "ui", recipeId = "missing", position = 0, createdAt = 1)),
            )
        val result = restore(zipOf(file), RestoreMode.MERGE)
        assertEquals(0, result.quests)
        val attempt = runBlocking { backupDao.attempts() }.single()
        assertNull(attempt.recipeId)
        assertNull(attempt.questId)
        assertNull(runBlocking { backupDao.shoppingItems() }.single().recipeId)
    }

    @Test
    fun `the sample fixture parses, matching data schemas backup schema`() {
        val file = BackupManager.parse(TestResources.read("backup/sample.json"))
        assertEquals(2, file.recipes.size)
        assertEquals(
            ParseSource.NONE,
            file.recipes[0]
                .ingredientGroups[0]
                .items[2]
                .parseSource,
        )
        assertEquals(QuestStatus.COMPLETED, file.quests.single().status)
        assertEquals("journal/a1.jpg", file.attempts[0].photo)
        val result = restore(zipOf(file), RestoreMode.REPLACE)
        assertEquals(3, result.attempts)
        assertEquals("Pompoensoep met salie", runBlocking { recipes.get("r-pompoensoep") }!!.title)
    }

    @Test
    fun `other files are refused with a reason`() {
        fun reason(bytes: ByteArray): BackupException.Reason? =
            try {
                runBlocking { backups.read(ByteArrayInputStream(bytes)) }
                null
            } catch (e: BackupException) {
                e.reason
            }
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reason("not a zip".toByteArray()))
        assertEquals(BackupException.Reason.NOT_A_BACKUP, reason(zipOf(mapOf("hello.txt" to "hi"))))
        assertEquals(BackupException.Reason.CORRUPT, reason(zipOf(mapOf("backup.json" to "{ nope"))))
        assertEquals(BackupException.Reason.CORRUPT, reason(zipOf(mapOf("backup.json" to "{}"))))
        assertEquals(
            BackupException.Reason.NOT_A_BACKUP,
            reason(zipOf(mapOf("backup.json" to """{"format":"other","schemaVersion":1,"exportedAt":1,"appVersion":"x"}"""))),
        )
        assertEquals(
            BackupException.Reason.NEWER_VERSION,
            reason(zipOf(mapOf("backup.json" to """{"format":"mise-backup","schemaVersion":2,"exportedAt":1,"appVersion":"x"}"""))),
        )
    }

    @Test
    fun `entries outside the known folders are never written`() {
        val file = BackupFile(format = BACKUP_FORMAT, exportedAt = 1, appVersion = "x")
        val zip = zipOf(mapOf("backup.json" to BackupManager.JSON.encodeToString(BackupFile.serializer(), file), "../../evil.jpg" to "x"))
        val read = runBlocking { backups.read(ByteArrayInputStream(zip)) }
        try {
            assertEquals(0, read.recipeCount)
        } finally {
            backups.discard(read)
        }
        if (images.recipeImage("evil").exists()) fail("wrote outside the restore folder")
    }

    private fun zipOf(file: BackupFile): ByteArray =
        zipOf(
            mapOf("backup.json" to BackupManager.JSON.encodeToString(BackupFile.serializer(), file)),
        )

    private fun zipOf(files: Map<String, String>): ByteArray =
        ByteArrayOutputStream()
            .also { out ->
                ZipOutputStream(out).use { zip ->
                    files.forEach { (name, text) ->
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(text.toByteArray())
                        zip.closeEntry()
                    }
                }
            }.toByteArray()

    private fun rewrite(
        zip: ByteArray,
        change: (BackupFile) -> BackupFile,
    ): ByteArray {
        val all = entries(zip)
        val file = BackupManager.parse(all.getValue("backup.json").toString(Charsets.UTF_8))
        val out = ByteArrayOutputStream()
        val json = BackupManager.JSON.encodeToString(BackupFile.serializer(), change(file)).toByteArray()
        ZipOutputStream(out).use { z ->
            (all + ("backup.json" to json)).forEach { (name, bytes) ->
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
