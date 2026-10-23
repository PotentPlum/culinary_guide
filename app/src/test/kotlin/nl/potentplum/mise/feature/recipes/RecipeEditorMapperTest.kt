package nl.potentplum.mise.feature.recipes

import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.domain.IngredientParser
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeEditorMapperTest {
    private val parser = IngredientParser(TestContent.content.units)

    private val manual = Ingredient(text = "2 uien", quantity = 2.0, name = "ui", parseSource = ParseSource.MANUAL)
    private val original =
        Recipe(
            title = "Uiensoep",
            sourceType = RecipeSourceType.URL,
            isFavorite = true,
            servings = 4,
            ingredientGroups =
                listOf(
                    IngredientGroup(items = listOf(manual, Ingredient(text = "1 l bouillon", parseSource = ParseSource.NONE))),
                ),
            stepGroups = listOf(StepGroup(steps = listOf(Step(text = "Snijd de uien."), Step(text = "Kook alles.")))),
            tags = setOf(Tag(TagKey.CUISINE, "Frans")),
        )

    @Test
    fun `a round trip without edits changes nothing`() {
        val state = RecipeEditorMapper.fromRecipe(original)
        assertEquals(original, RecipeEditorMapper.toRecipe(state, original, parser))
        assertFalse(RecipeEditorMapper.ingredientsChanged(original, original))
    }

    @Test
    fun `unchanged lines keep a manual parse, changed lines are parsed again`() {
        val state = RecipeEditorMapper.fromRecipe(original)
        val edited =
            state.copy(ingredientGroups = state.ingredientGroups.map { it.copy(text = "2 uien\n1,5 l runderbouillon\n\n50 g boter") })
        val saved = RecipeEditorMapper.toRecipe(edited, original, parser)
        val items = saved.ingredients
        assertEquals(manual, items[0])
        assertEquals(ParseSource.LOCAL, items[1].parseSource)
        assertEquals(1.5, items[1].quantity)
        assertEquals("boter", items[2].name)
        assertTrue(RecipeEditorMapper.ingredientsChanged(saved, original))
    }

    @Test
    fun `fields, groups, extra fields and tags`() {
        val state =
            RecipeEditorMapper.fromRecipe(original).copy(
                title = "  Franse uiensoep ",
                servings = "6",
                totalMinutes = "abc",
                stepGroups = listOf(EditableGroup(name = "Voor de croutons", text = "Rooster het brood."), EditableGroup()),
                extraFields = listOf(EditableField(label = "Wijnpairing", value = "Chablis"), EditableField(label = "Leeg")),
                dishType = "Soep",
                cuisines = emptySet(),
            )
        val saved = RecipeEditorMapper.toRecipe(state, original, parser)
        assertEquals("Franse uiensoep", saved.title)
        assertEquals(6, saved.servings)
        assertNull(saved.totalMinutes)
        assertEquals(listOf("Voor de croutons"), saved.stepGroups.map { it.name })
        assertEquals(1, saved.extraFields.size)
        assertEquals(setOf(Tag(TagKey.DISH_TYPE, "Soep")), saved.tags)
        assertTrue(saved.isFavorite)
        assertEquals(RecipeSourceType.URL, saved.sourceType)
    }

    @Test
    fun `a new recipe is manual`() {
        val state = RecipeEditorState(id = "new", title = "Pesto", ingredientGroups = listOf(EditableGroup(text = "1 bos basilicum")))
        val saved = RecipeEditorMapper.toRecipe(state, null, parser)
        assertEquals(RecipeSourceType.MANUAL, saved.sourceType)
        assertEquals("new", saved.id)
        assertEquals("basilicum", saved.ingredients.single().name)
        assertTrue(saved.stepGroups.isEmpty())
        assertFalse(state.hasSteps)
    }
}
