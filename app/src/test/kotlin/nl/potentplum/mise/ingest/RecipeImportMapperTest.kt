package nl.potentplum.mise.ingest

import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.domain.IngredientParser
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeImportMapperTest {
    private val mapper = RecipeImportMapper(IngredientParser(TestContent.content.units))

    private val html =
        """
        <html><head><script type="application/ld+json">
        {"@context":"https://schema.org","@type":"Recipe","name":"Pompoensoep","description":"Romig.",
         "recipeYield":"4 porties","prepTime":"PT15M","cookTime":"PT30M","image":"https://img.example/p.jpg",
         "keywords":"soep, herfst","aggregateRating":{"ratingValue":"4.6","ratingCount":"120"},
         "recipeIngredient":["500 g pompoen, in blokjes","1 ui","VOOR DE TOPPING:","2 el pompoenpitten"],
         "recipeInstructions":[{"@type":"HowToStep","text":"Snijd de pompoen."},{"@type":"HowToStep","text":"Kook 20 minuten."}]}
        </script></head></html>
        """.trimIndent()

    @Test
    fun `maps a parsed page to a recipe`() {
        val output = JsonLdRecipeParser.ingestRecipeFromHtml(html)
        val recipe = mapper.map(output, "https://www.leukerecepten.nl/pompoensoep", now = 42)

        assertEquals("Pompoensoep", recipe.title)
        assertEquals(RecipeSourceType.URL, recipe.sourceType)
        assertEquals("leukerecepten.nl", recipe.sourceLabel)
        assertEquals(4, recipe.servings)
        assertEquals(45, recipe.totalMinutes)
        assertEquals("https://img.example/p.jpg", recipe.imageUrl)
        assertEquals(listOf("soep", "herfst"), recipe.sourceMeta?.keywords)
        assertEquals(4.6, recipe.sourceMeta?.ratingValue)
        assertEquals(42, recipe.createdAt)

        // The default "Ingredients" section becomes an untitled group; a printed header stays a header.
        assertEquals(listOf(null, "VOOR DE TOPPING"), recipe.ingredientGroups.map { it.name })
        val pumpkin = recipe.ingredientGroups[0].items[0]
        assertEquals("pompoen", pumpkin.name)
        assertEquals("in blokjes", pumpkin.note)
        assertEquals(ParseSource.LOCAL, pumpkin.parseSource)
        assertNull(recipe.stepGroups.single().name)
        assertEquals(2, recipe.steps.size)
    }

    @Test
    fun `metric text wins over us text`() {
        val output =
            JsonLdRecipeParser.ingestRecipeFromHtml(html).copy(
                ingredientSections = listOf(IngredientSection("Ingrediënten", listOf(IngredientItem("2 cups milk", "500 ml milk")))),
            )
        val item = mapper.map(output, "https://a.nl/x", 0).ingredients.single()
        assertEquals("500 ml milk", item.text)
        assertEquals(500.0, item.quantity)
    }

    @Test
    fun `default section names mean no header`() {
        listOf("Ingredients", "Instructions", "Ingrediënten", "Bereiding", " ").forEach { assertNull(RecipeImportMapper.groupName(it)) }
        assertEquals("Voor de saus", RecipeImportMapper.groupName("Voor de saus"))
    }
}
