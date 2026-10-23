package nl.potentplum.mise.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.MiseDatabase
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.testing.FakeContentRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecipeRepositoryTest {
    private lateinit var db: MiseDatabase
    private lateinit var repository: RecipeRepository

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MiseDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository = RecipeRepository(db.recipeDao(), db.attemptDao(), FakeContentRepository(), MiseClock.fixed(LocalDate.of(2026, 10, 22)))
    }

    @After
    fun tearDown() = db.close()

    private val recipe =
        Recipe(
            title = "Stamppot boerenkool",
            sourceType = RecipeSourceType.MANUAL,
            ingredientGroups =
                listOf(
                    IngredientGroup(
                        items =
                            listOf(
                                Ingredient(
                                    text = "1 kg aardappelen",
                                    quantity = 1.0,
                                    unit = "kg",
                                    name = "aardappelen",
                                    parseSource = ParseSource.LOCAL,
                                ),
                                Ingredient(
                                    text = "500 g boerenkool",
                                    quantity = 500.0,
                                    unit = "g",
                                    name = "boerenkool",
                                    parseSource = ParseSource.LOCAL,
                                ),
                            ),
                    ),
                    IngredientGroup(name = "Voor de jus", items = listOf(Ingredient(text = "50 g boter"))),
                ),
            stepGroups = listOf(StepGroup(steps = listOf(Step(text = "Kook de aardappelen."), Step(text = "Stamp alles fijn.")))),
            extraFields = listOf(ExtraField(label = "Wijnpairing", value = "Riesling")),
            tags = setOf(Tag(TagKey.CUISINE, "Nederlands"), Tag(TagKey.DISH_TYPE, "Hoofdgerecht")),
        )

    @Test
    fun `the whole graph round trips in order`() =
        runTest {
            val saved = repository.save(recipe)
            val read = repository.get(saved.id)!!
            assertEquals(saved, read)
            assertEquals(listOf(null, "Voor de jus"), read.ingredientGroups.map { it.name })
            assertEquals(listOf("Kook de aardappelen.", "Stamp alles fijn."), read.steps.map { it.text })
            assertTrue(read.createdAt > 0)
        }

    @Test
    fun `saving again replaces the children`() =
        runTest {
            val saved = repository.save(recipe)
            repository.save(saved.copy(ingredientGroups = saved.ingredientGroups.take(1), tags = emptySet()))
            val read = repository.get(saved.id)!!
            assertEquals(1, read.ingredientGroups.size)
            assertTrue(read.tags.isEmpty())
        }

    @Test
    fun `library items carry vegetables, tags and cooked stats`() =
        runTest {
            val saved = repository.save(recipe)
            db.attemptDao().upsert(AttemptEntity("a1", AttemptSourceType.RECIPE, recipeId = saved.id, cookedAt = 100))
            db.attemptDao().upsert(AttemptEntity("a2", AttemptSourceType.RECIPE, recipeId = saved.id, cookedAt = 200))

            val item = repository.observeLibrary().first().single()
            assertEquals(setOf("kale", "potato"), item.vegIds)
            assertEquals(setOf("Nederlands"), item.cuisines)
            assertEquals(2, item.cookedCount)
            assertEquals(200L, item.lastCookedAt)
            assertTrue("boerenkool" in item.ingredientTexts)
        }

    @Test
    fun `favourite and delete`() =
        runTest {
            val saved = repository.save(recipe)
            repository.setFavorite(saved.id, true)
            assertTrue(repository.get(saved.id)!!.isFavorite)
            repository.delete(saved.id)
            assertNull(repository.get(saved.id))
        }

    @Test
    fun `duplicates are found by url`() =
        runTest {
            val saved = repository.save(recipe.copy(sourceUrl = "https://ah.nl/r/1"))
            assertEquals(saved.id, repository.findInLibraryByUrl("https://ah.nl/r/1"))
            assertNull(repository.findInLibraryByUrl("https://ah.nl/r/2"))
        }
}
