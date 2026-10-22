package nl.potentplum.mise.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.IngredientEntity
import nl.potentplum.mise.data.db.entity.IngredientGroupEntity
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.RecipeEntity
import nl.potentplum.mise.data.db.entity.RecipeExtraFieldEntity
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.RecipeTagEntity
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity
import nl.potentplum.mise.data.db.entity.SourceMeta
import nl.potentplum.mise.data.db.entity.StepEntity
import nl.potentplum.mise.data.db.entity.StepGroupEntity
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.domain.Difficulty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiseDatabaseTest {
    private lateinit var db: MiseDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MiseDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = db.close()

    private fun recipe(
        id: String,
        inLibrary: Boolean = true,
    ) = RecipeEntity(
        id = id,
        title = "Pompoensoep",
        sourceType = RecipeSourceType.MANUAL,
        inLibrary = inLibrary,
        sourceMeta = SourceMeta(keywords = listOf("soep")),
        createdAt = 1,
        updatedAt = 1,
    )

    private suspend fun writeGraph(id: String) {
        db.recipeDao().replaceGraph(
            recipe = recipe(id),
            ingredientGroups = listOf(IngredientGroupEntity("g1", id, 0), IngredientGroupEntity("g2", id, 1, "Voor de saus")),
            ingredients =
                listOf(
                    IngredientEntity("i1", "g1", 0, "500 g pompoen", 500.0, null, "g", "pompoen", null, ParseSource.LOCAL),
                    IngredientEntity("i2", "g2", 0, "2-3 el room", 2.0, 3.0, "el", "room"),
                ),
            stepGroups = listOf(StepGroupEntity("s1", id, 0)),
            steps = listOf(StepEntity("st1", "s1", 0, "Snijd de pompoen.")),
            extraFields = listOf(RecipeExtraFieldEntity("x1", id, 0, "Wijnpairing", "Chablis")),
            tags = listOf(RecipeTagEntity(id, TagKey.TECHNIQUE, "brunoise"), RecipeTagEntity(id, TagKey.CUISINE, "Frans")),
        )
    }

    @Test
    fun `a recipe graph round trips`() =
        runTest {
            writeGraph("r1")
            val graph = db.recipeDao().getGraph("r1")!!
            assertEquals(listOf("soep"), graph.recipe.sourceMeta?.keywords)
            assertEquals(2, graph.ingredientGroups.size)
            val sauce = graph.ingredientGroups.single { it.group.name == "Voor de saus" }
            assertEquals(3.0, sauce.items.single().quantityMax!!, 0.0)
            assertEquals(
                1,
                graph.stepGroups
                    .single()
                    .steps.size,
            )
            assertEquals("Chablis", graph.extraFields.single().value)
            assertEquals(setOf(TagKey.TECHNIQUE, TagKey.CUISINE), graph.tags.map { it.key }.toSet())
        }

    @Test
    fun `replacing a graph removes old children`() =
        runTest {
            writeGraph("r1")
            db.recipeDao().replaceGraph(recipe("r1"), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
            val graph = db.recipeDao().getGraph("r1")!!
            assertTrue(graph.ingredientGroups.isEmpty())
            assertTrue(graph.stepGroups.isEmpty())
            assertTrue(graph.tags.isEmpty())
        }

    @Test
    fun `deleting a recipe cascades to its children and keeps its attempts`() =
        runTest {
            writeGraph("r1")
            db.attemptDao().upsert(
                AttemptEntity("a1", AttemptSourceType.RECIPE, recipeId = "r1", cookedAt = 10, score = 8, vegIds = listOf("pumpkin")),
            )
            db.recipeDao().delete("r1")

            assertNull(db.recipeDao().getGraph("r1"))
            val attempt = db.attemptDao().get("a1")!!
            assertNull(attempt.recipeId)
            assertEquals(listOf("pumpkin"), attempt.vegIds)
        }

    @Test
    fun `deleting a quest deletes its hidden recipe`() =
        runTest {
            db.recipeDao().upsertRecipe(recipe("qr", inLibrary = false))
            val quest = quest("q1", recipeId = "qr")
            db.questDao().upsert(quest)
            db.attemptDao().upsert(AttemptEntity("a1", AttemptSourceType.QUEST, recipeId = "qr", questId = "q1", cookedAt = 1))

            db.questDao().deleteWithRecipe(quest)

            assertNull(db.questDao().get("q1"))
            assertFalse(db.recipeDao().exists("qr"))
            val attempt = db.attemptDao().get("a1")!!
            assertNull(attempt.questId)
            assertNull(attempt.recipeId)
        }

    @Test
    fun `the library only shows recipes in the library`() =
        runTest {
            db.recipeDao().upsertRecipe(recipe("r1"))
            db.recipeDao().upsertRecipe(recipe("hidden", inLibrary = false))
            assertEquals(
                listOf("r1"),
                db
                    .recipeDao()
                    .observeLibrary()
                    .first()
                    .map { it.id },
            )
        }

    @Test
    fun `cooked stats are derived from attempts`() =
        runTest {
            db.recipeDao().upsertRecipe(recipe("r1"))
            db.attemptDao().upsert(AttemptEntity("a1", AttemptSourceType.RECIPE, recipeId = "r1", cookedAt = 10))
            db.attemptDao().upsert(AttemptEntity("a2", AttemptSourceType.RECIPE, recipeId = "r1", cookedAt = 30))
            val stats =
                db
                    .attemptDao()
                    .observeCookedStats()
                    .first()
                    .single()
            assertEquals(2, stats.count)
            assertEquals(30, stats.lastCookedAt)
        }

    @Test
    fun `the shopping badge counts unticked items`() =
        runTest {
            val dao = db.shoppingDao()
            dao.upsert(
                listOf(
                    ShoppingItemEntity("s1", "3 uien", position = 0, createdAt = 1),
                    ShoppingItemEntity("s2", "1 prei", position = 1, createdAt = 1),
                ),
            )
            assertEquals(2, dao.observeUncheckedCount().first())
            dao.setChecked("s1", true)
            assertEquals(1, dao.observeUncheckedCount().first())
            assertEquals(listOf("s2", "s1"), dao.observeAll().first().map { it.id })
            assertEquals(2, dao.nextPosition())
            dao.deleteChecked()
            assertEquals(listOf("s2"), dao.observeAll().first().map { it.id })
        }

    private fun quest(
        id: String,
        recipeId: String,
    ) = QuestEntity(
        id = id,
        createdAt = 1,
        isDaily = true,
        ingredientMode = IngredientMode.SURPRISE,
        ingredient = "pastinaak",
        vegId = "parsnip",
        questType = QuestType.CLAUDE_CHOICE,
        text = "Maak een pastinaakpuree",
        skill = "Puree",
        difficulty = Difficulty.BEGINNER,
        timeEstimateMinutes = 30,
        why = "Oefen je mesvaardigheid",
        dishType = "Bijgerecht",
        tips = listOf("Gebruik bloemige aardappel"),
        recipeId = recipeId,
    )
}
