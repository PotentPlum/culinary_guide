package nl.potentplum.mise.feature.journal

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.AttemptRepository
import nl.potentplum.mise.data.repo.Evaluation
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Spec §6.13: Tijdlijn entries can be edited and deleted with undo; Inzichten shows the season tally and badges. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class JournalFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var recipes: RecipeRepository

    @Inject lateinit var attempts: AttemptRepository

    @Before
    fun setUp() {
        hilt.inject()
    }

    private fun seed(): String =
        runBlocking {
            recipes.save(
                Recipe(
                    id = "soup",
                    title = "Pompoensoep",
                    sourceType = RecipeSourceType.MANUAL,
                    ingredientGroups =
                        listOf(
                            IngredientGroup(
                                items =
                                    listOf(
                                        Ingredient(text = "1 pompoen", quantity = 1.0, name = "pompoen", parseSource = ParseSource.MANUAL),
                                    ),
                            ),
                        ),
                    tags = setOf(Tag(TagKey.TECHNIQUE, "julienne"), Tag(TagKey.CUISINE, "Nederlands")),
                ),
            )
            attempts.log("soup", Evaluation(cookedOn = LocalDate.of(2026, 10, 22), score = 8, tasteScore = 10))!!.id
        }

    private fun launch() {
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }
        click("Dagboek")
    }

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun `an entry shows in Tijdlijn with its scores, and can be edited`() {
        val id = seed()
        launch()
        compose.reveal("Uitvoering: Goed · Smaak: Top")
        click("Pompoensoep")
        compose.reveal("Notities (optioneel)")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Meer salie")
        click("Opslaan")
        compose.reveal("Meer salie")
        assertEquals("Meer salie", runBlocking { attempts.observeAll().first() }.single { it.id == id }.notes)
    }

    @Test
    fun `deleting an entry can be undone`() {
        seed()
        launch()
        click("Pompoensoep")
        click("Uit dagboek verwijderen")
        compose.reveal("Verwijderd uit je dagboek")
        click("Ongedaan maken")
        compose.reveal("Uitvoering: Goed · Smaak: Top")
        assertEquals(1, runBlocking { attempts.observeAll().first() }.size)
    }

    @Test
    fun `Inzichten shows the stats, techniques and badges`() {
        seed()
        launch()
        click("Inzichten")
        compose.reveal("deze maand gekookt")
        compose.reveal(hasContentDescription("Insignes"))
        compose.reveal("De Messenvirtuoos")
        compose.reveal("Nog 7 keer oefenen")
    }

    @Test
    fun `an empty journal invites the first entry and leads to Vandaag`() {
        launch()
        compose.reveal("Leg je eerste gekookte recept")
        click("Naar Vandaag")
        compose.reveal("Begin met je eerste recept")
    }
}
