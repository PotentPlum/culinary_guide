package nl.potentplum.mise.claude.enrich

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.data.db.entity.EnrichState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnrichmentQueueTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var fixture: EnrichmentFixture? = null
    private val scheduled = mutableListOf<String>()

    @After
    fun tearDown() {
        fixture?.db?.close()
    }

    private suspend fun TestScope.queue(): Pair<EnrichmentFixture, EnrichmentQueue> {
        val f = EnrichmentFixture(folder.root, backgroundScope, StandardTestDispatcher(testScheduler)).also { fixture = it }
        f.recipes.save(EnrichmentFixture.recipe)
        return f to EnrichmentQueue(f.secrets, f.recipes, scheduler = { scheduled += it })
    }

    @Test
    fun `without a key nothing is queued and the recipe stays NONE`() =
        runTest {
            val (f, queue) = queue()
            assertFalse(queue.request("r1"))
            assertEquals(emptyList<String>(), scheduled)
            assertEquals(EnrichState.NONE, f.recipes.get("r1")!!.enrichState)
        }

    @Test
    fun `with a key the recipe is PENDING and scheduled once`() =
        runTest {
            val (f, queue) = queue()
            f.secrets.saveClaudeKey("sk-ant-test")
            assertTrue(queue.request("r1"))
            assertEquals(listOf("r1"), scheduled)
            assertEquals(EnrichState.PENDING, f.recipes.get("r1")!!.enrichState)
        }

    @Test
    fun `a recipe without ingredients or an unknown id is not queued`() =
        runTest {
            val (f, queue) = queue()
            f.secrets.saveClaudeKey("sk-ant-test")
            f.recipes.save(EnrichmentFixture.recipe.copy(id = "empty", ingredientGroups = emptyList()))
            assertFalse(queue.request("empty"))
            assertFalse(queue.request("missing"))
            assertEquals(emptyList<String>(), scheduled)
        }
}
