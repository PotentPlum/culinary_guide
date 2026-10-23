package nl.potentplum.mise.claude.enrich

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeModels
import nl.potentplum.mise.claude.ContentPart
import nl.potentplum.mise.claude.Effort
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.Tag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeEnricherTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var fixture: EnrichmentFixture? = null

    @After
    fun tearDown() {
        fixture?.db?.close()
    }

    private suspend fun TestScope.setUp(): EnrichmentFixture =
        EnrichmentFixture(folder.root, backgroundScope, StandardTestDispatcher(testScheduler)).also {
            fixture = it
            it.secrets.saveClaudeKey("sk-ant-test")
            it.recipes.save(EnrichmentFixture.recipe)
        }

    @Test
    fun `one Haiku call structures every line except MANUAL and fills only missing tag keys`() =
        runTest {
            val f = setUp()
            assertEquals(EnrichOutcome.Done, f.enricher.enrich("r1"))

            val request = f.client.lastRequest!!
            assertEquals(ClaudeModels.HAIKU, request.model)
            assertEquals(Effort.LOW, request.effort)
            val text = (request.content.single() as ContentPart.Text).text
            assertTrue(text, text.contains("0. 1 flinke pompoen (ca. 1 kg)") && text.contains("1. Snipper de ui."))

            val saved = f.recipes.get("r1")!!
            assertEquals(EnrichState.DONE, saved.enrichState)
            val (pumpkin, onion, salt) = saved.ingredients
            assertEquals(Ingredient("i0", "1 flinke pompoen (ca. 1 kg)", 1.0, null, "kg", "pompoen", "flink", ParseSource.CLAUDE), pumpkin)
            assertEquals("ui", onion.name)
            assertEquals(ParseSource.CLAUDE, onion.parseSource)
            // MANUAL is never overwritten.
            assertEquals("zeezout", salt.name)
            assertEquals(ParseSource.MANUAL, salt.parseSource)
            // The cuisine was already tagged; unknown values are dropped.
            assertEquals(
                setOf(
                    Tag(TagKey.CUISINE, "Frans"),
                    Tag(TagKey.DISH_TYPE, "Soep"),
                    Tag(TagKey.MAIN_INGREDIENT, "Groenten"),
                    Tag(TagKey.TECHNIQUE, "snipperen"),
                ),
                saved.tags,
            )
            assertEquals(
                1_200L,
                f.service
                    .thisMonth()
                    .first()
                    .inputTokens,
            )
        }

    @Test
    fun `a failure leaves the recipe alone and markFailed flags it`() =
        runTest {
            val f = setUp()
            f.recipes.setEnrichState("r1", EnrichState.PENDING)
            f.client.failure = ClaudeError.Network
            val outcome = f.enricher.enrich("r1") as EnrichOutcome.Failed
            assertTrue(outcome.retryable)
            assertEquals(EnrichmentFixture.recipe.ingredients, f.recipes.get("r1")!!.ingredients)
            assertEquals(EnrichState.PENDING, f.recipes.get("r1")!!.enrichState)

            f.enricher.markFailed("r1", outcome.error)
            assertEquals(EnrichState.FAILED, f.recipes.get("r1")!!.enrichState)
        }

    @Test
    fun `client errors are not retried and a missing key is not a failure`() =
        runTest {
            val f = setUp()
            assertFalse(EnrichOutcome.Failed(ClaudeError.Http(400)).retryable)
            assertFalse(EnrichOutcome.Failed(ClaudeError.InvalidResponse).retryable)
            assertTrue(EnrichOutcome.Failed(ClaudeError.Http(529)).retryable)

            f.secrets.deleteClaudeKey()
            val outcome = f.enricher.enrich("r1") as EnrichOutcome.Failed
            assertEquals(ClaudeError.NoKey, outcome.error)
            f.enricher.markFailed("r1", outcome.error)
            assertEquals(EnrichState.NONE, f.recipes.get("r1")!!.enrichState)
        }

    @Test
    fun `an edit to the lines during the call discards the stale reply`() =
        runTest {
            val f = setUp()
            val edited =
                EnrichmentFixture.recipe.copy(
                    ingredientGroups =
                        EnrichmentFixture.recipe.ingredientGroups.map { group ->
                            group.copy(items = listOf(Ingredient(id = "new", text = "500 g pompoen")) + group.items)
                        },
                )
            f.client.during = { f.recipes.save(edited) }
            assertEquals(EnrichOutcome.Skipped, f.enricher.enrich("r1"))
            assertEquals(edited.ingredients, f.recipes.get("r1")!!.ingredients)
        }

    @Test
    fun `nothing to enrich makes no call`() =
        runTest {
            val f = setUp()
            val complete =
                EnrichmentFixture.recipe.copy(
                    ingredientGroups =
                        EnrichmentFixture.recipe.ingredientGroups.map { group ->
                            group.copy(items = group.items.map { it.copy(parseSource = ParseSource.MANUAL) })
                        },
                    tags = TagKey.entries.map { Tag(it, "x") }.toSet(),
                )
            f.recipes.save(complete)
            assertEquals(EnrichOutcome.Done, f.enricher.enrich("r1"))
            assertEquals(0, f.client.calls)
            assertEquals(EnrichState.DONE, f.recipes.get("r1")!!.enrichState)
        }
}
