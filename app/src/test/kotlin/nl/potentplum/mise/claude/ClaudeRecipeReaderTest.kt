package nl.potentplum.mise.claude

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import nl.potentplum.mise.claude.enrich.EnrichmentFixture
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.settings.Quality
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.testing.FakeContentRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ClaudeRecipeReaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var fixture: EnrichmentFixture? = null

    @After
    fun tearDown() {
        fixture?.db?.close()
    }

    private val reply =
        """
        {"title":"Bietensalade","description":"","sourceLabel":null,"servings":2,"prepMinutes":null,"cookMinutes":null,
         "totalMinutes":10,"ingredientGroups":[{"name":null,"items":[
           {"text":"300 g biet","quantity":300,"quantityMax":null,"unit":"g","name":"biet","note":null}]}],
         "stepGroups":[{"name":null,"steps":["Snijd de biet."]}],"extraFields":[],
         "tags":{"cuisine":[],"dishType":"Salade","mainIngredient":[],"techniques":[]}}
        """

    private suspend fun TestScope.reader(quality: Quality): Pair<ClaudeRecipeReader, EnrichmentFixture> {
        val f = EnrichmentFixture(folder.root, backgroundScope, StandardTestDispatcher(testScheduler)).also { fixture = it }
        f.secrets.saveClaudeKey("sk-ant-test")
        f.client.reply = Json.parseToJsonElement(reply).jsonObject
        val settings =
            SettingsRepository(
                PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "settings.preferences_pb") },
                MiseClock.fixed(LocalDate.of(2026, 10, 22)),
            )
        settings.setQuality(quality)
        return ClaudeRecipeReader(f.service, FakeContentRepository(), settings, f.recipes) to f
    }

    @Test
    fun `photos go to the Kwaliteit model with medium effort, images first`() =
        runTest {
            val (reader, f) = reader(Quality.BEST)
            val recipe = reader.fromPhotos(listOf(byteArrayOf(1), byteArrayOf(2)))
            val request = f.client.lastRequest!!
            assertEquals(ClaudeModels.OPUS, request.model)
            assertEquals(Effort.MEDIUM, request.effort)
            assertEquals(listOf(true, true, false), request.content.map { it is ContentPart.Image })
            assertEquals("Lees het recept op deze 2 foto's.", (request.content.last() as ContentPart.Text).text)
            assertEquals("Bietensalade", recipe.title)
            assertEquals(RecipeSourceType.PHOTO, recipe.sourceType)
            // Already structured and tagged, so it never goes through enrichment.
            assertEquals(EnrichState.DONE, recipe.enrichState)
            assertEquals(
                1_200L,
                f.service
                    .thisMonth()
                    .first()
                    .inputTokens,
            )
        }

    @Test
    fun `zero or more than five photos are refused before any call`() =
        runTest {
            val (reader, f) = reader(Quality.FAST)
            assertTrue(runCatching { reader.fromPhotos(emptyList()) }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { reader.fromPhotos(List(6) { byteArrayOf(1) }) }.exceptionOrNull() is IllegalArgumentException)
            assertEquals(0, f.client.calls)
        }

    @Test
    fun `page text keeps the url and follows Kwaliteit too`() =
        runTest {
            val (reader, f) = reader(Quality.FAST)
            val recipe = reader.fromPage("https://example.nl/biet", "Bietensalade …")
            assertEquals(ClaudeModels.HAIKU, f.client.lastRequest!!.model)
            assertEquals(RecipeSourceType.PAGE, recipe.sourceType)
            assertEquals("https://example.nl/biet", recipe.sourceUrl)
        }
}
