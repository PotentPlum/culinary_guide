package nl.potentplum.mise.claude.quest

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ContentPart
import nl.potentplum.mise.claude.enrich.EnrichmentFixture
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.db.entity.TimeLimit
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.domain.QuestParams
import nl.potentplum.mise.testing.FakeContentRepository
import nl.potentplum.mise.testing.TestContent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class QuestGeneratorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var fixture: EnrichmentFixture? = null
    private val clock = MiseClock.fixed(LocalDate.of(2026, 10, 22))

    @After
    fun tearDown() {
        fixture?.db?.close()
    }

    private fun reply(
        chosenVegId: String? = null,
        steps: List<String> = listOf("Rooster de pastinaak.", "Maak de saus."),
    ): JsonObject =
        Json
            .parseToJsonElement(
                """
                {"questText":"Rooster pastinaak tot hij karamelliseert.","skill":"Groenten roosteren","difficulty":"INTERMEDIATE",
                 "timeEstimateMinutes":45,"why":"Roosteren maakt zoet.","dishType":"Bijgerecht","chosenIngredient":"pastinaak",
                 "chosenVegId":null,
                 "tags":{"cuisine":["Nederlands"],"mainIngredient":["Groenten"],"techniques":["groenten-roasten","onbekend"]},
                 "recipe":{"title":"Geroosterde pastinaak","servings":2,
                   "ingredientGroups":[{"name":null,"items":[
                     {"text":"500 g pastinaak","quantity":500,"quantityMax":null,"unit":"g","name":"pastinaak","note":null}]}],
                   "steps":[],"tips":["Snijd gelijke stukken."]}}
                """,
            ).jsonObject
            .let { base ->
                val recipe = base["recipe"]!!.jsonObject
                JsonObject(
                    base +
                        mapOf(
                            "chosenVegId" to (chosenVegId?.let(::JsonPrimitive) ?: JsonNull),
                            "recipe" to
                                JsonObject(recipe + ("steps" to JsonArray(steps.map { JsonObject(mapOf("text" to JsonPrimitive(it))) }))),
                        ),
                )
            }

    private suspend fun TestScope.setUp(): Triple<QuestGenerator, QuestRepository, EnrichmentFixture> {
        val f = EnrichmentFixture(folder.root, backgroundScope, StandardTestDispatcher(testScheduler)).also { fixture = it }
        f.secrets.saveClaudeKey("sk-ant-test")
        f.client.reply = reply()
        val settings =
            SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "s.preferences_pb") }, clock)
        val quests = QuestRepository(f.db, f.db.questDao(), f.db.attemptDao(), clock)
        return Triple(QuestGenerator(f.service, FakeContentRepository(), settings, quests, f.recipes, clock), quests, f)
    }

    private fun EnrichmentFixture.requestText() = (client.lastRequest!!.content.single() as ContentPart.Text).text

    @Test
    fun `the daily quest uses today's seasonal vegetable, Basis and the neglected discipline`() =
        runTest {
            val (generator, quests, f) = setUp()
            val params = generator.dailyParams()
            assertEquals(IngredientMode.SEASONAL, params.ingredientMode)
            assertTrue(params.isDaily)
            assertEquals(params, generator.dailyParams())
            assertTrue(params.vegId != generator.dailyParams(offset = 1).vegId)

            val plan = generator.plan(params)
            assertEquals(Difficulty.BEGINNER, plan.difficulty)
            assertEquals(DisciplineId.MECHANICAL, plan.focus)

            val quest = generator.generate(plan)
            val veg = TestContent.content.vegetable(params.vegId!!)!!
            val text = f.requestText()
            assertTrue(text, text.startsWith("Ingrediënt: ${veg.nl.lowercase()} (seizoensgroente, id: ${veg.id})"))
            assertTrue(text, text.contains("Moeilijkheid: basis (BEGINNER)"))
            assertTrue(text, text.contains("Focus: oefen vooral Mechanisch Meesterschap"))
            assertFalse(text, text.contains("Groenten in het seizoen"))
            assertFalse(text, text.contains("Niet herhalen"))

            assertEquals(veg.id, quest.vegId)
            assertEquals(veg.nl.lowercase(), quest.ingredient)
            assertEquals(Difficulty.INTERMEDIATE, quest.difficulty)
            assertEquals(listOf("Snijd gelijke stukken."), quest.tips)
            assertEquals(quest, quests.observeDaily().first())

            val recipe = f.recipes.get(quest.recipeId)!!
            assertEquals(RecipeSourceType.QUEST, recipe.sourceType)
            assertFalse(recipe.inLibrary)
            assertEquals(listOf("Bijgerecht"), recipe.tagValues(TagKey.DISH_TYPE))
            assertEquals(listOf("groenten-roasten"), recipe.tagValues(TagKey.TECHNIQUE))
            assertEquals(45, recipe.totalMinutes)
            assertEquals(emptyList<Any>(), f.recipes.observeLibrary().first())
        }

    @Test
    fun `Verras me sends the candidates, and only a listed id counts as chosen`() =
        runTest {
            val (generator, _, f) = setUp()
            val plan = generator.plan(QuestParams(questType = QuestType.SAUS, difficulty = Difficulty.ADVANCED))
            val candidate = plan.inSeason[1]
            f.client.reply = reply(chosenVegId = candidate)
            val quest = generator.generate(plan)
            val text = f.requestText()
            assertTrue(text, text.contains("Soort quest: saus (geen hele maaltijd)"))
            assertTrue(text, text.contains("Moeilijkheid: gevorderd (ADVANCED)"))
            assertTrue(text, text.contains("Groenten in het seizoen (id: naam):\n- ${plan.inSeason[0]}:"))
            assertEquals(candidate, quest.vegId)
            assertEquals(Difficulty.ADVANCED, quest.requestedDifficulty)

            f.client.reply = reply(chosenVegId = "not-listed")
            val other = generator.generate(plan)
            assertNull(other.vegId)
            assertEquals("pastinaak", other.ingredient)
        }

    @Test
    fun `a technique replaces the focus, and earlier quests are not repeated`() =
        runTest {
            val (generator, _, f) = setUp()
            generator.generate(generator.plan(QuestParams()))
            val params =
                QuestParams(
                    ingredientMode = IngredientMode.CUSTOM,
                    ingredient = "restje kool ",
                    quantity = 0.5,
                    unit = "kg",
                    techniqueId = "julienne",
                    cuisine = "Italiaans",
                    timeLimit = TimeLimit.UNDER_30,
                    simpleAndSmall = true,
                )
            val plan = generator.plan(params)
            assertNull(plan.focus)
            val quest = generator.generate(plan)
            val text = f.requestText()
            assertTrue(text, text.startsWith("Ingrediënt: restje kool (hoeveelheid: 0,5 kg)"))
            assertTrue(text, text.contains("Techniek: Julienne (id: julienne)"))
            assertFalse(text, text.contains("Focus:"))
            assertTrue(text, text.contains("Keuken: Italiaans\nBeschikbare tijd: minder dan 30 minuten\nKlein & eenvoudig"))
            assertTrue(
                text,
                text.contains("Niet herhalen (eerdere quests, techniek en titel):\n- Groenten roosteren: Geroosterde pastinaak"),
            )
            assertEquals("restje kool", quest.ingredient)
            assertEquals(params.copy(ingredient = "restje kool"), QuestParams.of(quest))
        }

    @Test
    fun `an unusable reply saves nothing`() =
        runTest {
            val (generator, quests, f) = setUp()
            f.client.reply = reply(steps = emptyList())
            val error = runCatching { generator.generate(generator.plan(QuestParams())) }.exceptionOrNull()
            assertEquals(ClaudeError.InvalidResponse, error)
            assertEquals(emptyList<Any>(), quests.observeAll().first())
        }

    @Test
    fun `completion is recorded once and deleting a quest deletes its recipe`() =
        runTest {
            val (generator, quests, f) = setUp()
            val quest = generator.generate(generator.plan(QuestParams()))
            quests.markCompleted(quest.id, at = 100)
            quests.markCompleted(quest.id, at = 200)
            assertEquals(QuestStatus.COMPLETED, quests.get(quest.id)!!.status)
            assertEquals(100L, quests.get(quest.id)!!.completedAt)
            quests.delete(quest.id)
            assertNull(quests.get(quest.id))
            assertNull(f.recipes.get(quest.recipeId))
        }
}
