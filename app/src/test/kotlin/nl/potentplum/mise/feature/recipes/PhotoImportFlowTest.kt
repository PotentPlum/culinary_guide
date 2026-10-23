package nl.potentplum.mise.feature.recipes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.SharedContent
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeModels
import nl.potentplum.mise.claude.ContentPart
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.ingest.ImageStore
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.FakeClaudeClient
import nl.potentplum.mise.testing.FakeEnrichmentScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/** Spec §6.9: shared photos → reorder/remove → Lees recept (recorded reply) → preview → Bewaren. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoImportFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var recipes: RecipeRepository

    @Inject
    lateinit var secrets: SecretStore

    @Inject
    lateinit var images: ImageStore

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hilt.inject()
        FakeClaudeClient.reset()
        FakeEnrichmentScheduler.scheduled.clear()
        FakeClaudeClient.reply =
            FakeClaudeClient.json(
                """
                {"title":"Bietensalade","description":"","sourceLabel":"Groente!","servings":2,"prepMinutes":10,"cookMinutes":null,
                 "totalMinutes":10,
                 "ingredientGroups":[{"name":null,"items":[
                   {"text":"300 g gekookte biet","quantity":300,"quantityMax":null,"unit":"g","name":"biet","note":"gekookt"}]}],
                 "stepGroups":[{"name":null,"steps":["Snijd de biet in partjes."]}],
                 "extraFields":[],"tags":{"cuisine":[],"dishType":"Salade","mainIngredient":["Groenten"],"techniques":[]}}
                """,
            )
    }

    /** A real image file, shared as a file URI; [width] × [height] tells the photos apart. */
    private fun photo(
        width: Int,
        height: Int,
    ): Uri {
        val file = File(context.cacheDir, "shared-${width}x$height.png")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    private fun share(vararg uris: Uri) {
        compose.setContent {
            MiseTheme {
                MiseAppShell(
                    today = LocalDate.of(2026, 10, 22),
                    uncheckedShoppingCount = 0,
                    pendingShare = SharedContent.Images(uris.toList()),
                )
            }
        }
    }

    private fun click(text: String) {
        val node = compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    /** The photo count is an eyebrow, which TalkBack reads from its content description. */
    private fun waitForCount(count: Int) =
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("$count van 5 foto's").fetchSemanticsNodes().isNotEmpty()
        }

    private fun saveKey() = runBlocking { secrets.saveClaudeKey("sk-ant-test") }

    @Test
    fun `shared photos are read by Claude, previewed and saved with the first photo as image`() {
        saveKey()
        share(photo(40, 20), photo(20, 40), photo(30, 30))
        waitForCount(3)
        // Photo 2 to the front, photo 3 out: Claude sees 20×40 then 40×20.
        // The strip scrolls sideways; photo 3's buttons start off-screen.
        compose.onNodeWithContentDescription("Foto 2 naar voren").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Foto 3 verwijderen").performScrollTo().performClick()
        waitForCount(2)
        click("Lees recept")
        waitForText("Bewaren")
        // Below the photo header, so scroll the preview to it.
        compose.onNode(hasScrollAction() and hasScrollToNodeAction()).performScrollToNode(hasText("300 g biet", substring = true))
        click("Bewaren")
        waitForText("Kookmodus")

        val request = FakeClaudeClient.requests.single()
        assertEquals(ClaudeModels.SONNET, request.model)
        val sizes =
            request.content.filterIsInstance<ContentPart.Image>().map { image ->
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size).let { it.width to it.height }
            }
        assertEquals(listOf(20 to 40, 40 to 20), sizes)

        val saved =
            runBlocking {
                recipes.get(
                    recipes
                        .observeLibrary()
                        .first()
                        .single()
                        .id,
                )!!
            }
        assertEquals("Bietensalade", saved.title)
        assertEquals(RecipeSourceType.PHOTO, saved.sourceType)
        assertEquals(EnrichState.DONE, saved.enrichState)
        assertEquals(images.recipeImage(saved.id).absolutePath, saved.imagePath)
        val stored = BitmapFactory.decodeFile(saved.imagePath)
        assertEquals(20 to 40, stored.width to stored.height)
        // Photo imports are already structured: no enrichment, and the temporary photos are gone.
        assertEquals(emptyList<String>(), FakeEnrichmentScheduler.scheduled)
        assertTrue(
            images
                .importDir()
                .listFiles()
                .orEmpty()
                .isEmpty(),
        )
    }

    @Test
    fun `without a key the photos wait and Lees recept is disabled`() {
        share(photo(40, 20))
        waitForCount(1)
        waitForText("Sleutel toevoegen")
        compose.onNode(hasText("Lees recept"), useUnmergedTree = false).assertIsNotEnabled()
        assertEquals(0, FakeClaudeClient.requests.size)
    }

    @Test
    fun `a refusal shows the Dutch message and keeps the photos`() {
        saveKey()
        FakeClaudeClient.failure = ClaudeError.Refused(null)
        share(photo(40, 20))
        waitForCount(1)
        click("Lees recept")
        waitForText("Claude wilde dit verzoek niet uitvoeren.")
        compose.onNodeWithContentDescription("1 van 5 foto's").assertExists()
    }

    @Test
    fun `an unreadable result opens edit mode with the gaps marked`() {
        saveKey()
        FakeClaudeClient.reply =
            FakeClaudeClient.json(
                """{"title":"","description":"","sourceLabel":null,"servings":null,"prepMinutes":null,"cookMinutes":null,
                "totalMinutes":null,"ingredientGroups":[],"stepGroups":[],"extraFields":[],
                "tags":{"cuisine":[],"dishType":null,"mainIngredient":[],"techniques":[]}}""",
            )
        share(photo(40, 20))
        waitForCount(1)
        click("Lees recept")
        waitForText("Vul de ontbrekende delen aan.")
    }
}
