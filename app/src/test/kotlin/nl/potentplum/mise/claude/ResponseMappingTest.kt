package nl.potentplum.mise.claude

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.domain.IngredientParser
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Recorded replies (no live calls) mapped through the post-parse validation of spec §8.6. */
class ResponseMappingTest {
    private val parser = IngredientParser(TestContent.content.units)
    private val mapping = ResponseMapping(TestContent.content, parser)

    private val recipe =
        Recipe(
            title = "Stoofpot",
            sourceType = RecipeSourceType.URL,
            ingredientGroups =
                listOf(
                    IngredientGroup(
                        items =
                            listOf(
                                Ingredient.fromLocalParse(parser.parse("2 uien")),
                                Ingredient(
                                    text = "1 pond rundvlees",
                                    quantity = 500.0,
                                    unit = "g",
                                    name = "rundersukade",
                                    parseSource = ParseSource.MANUAL,
                                ),
                                Ingredient.fromLocalParse(parser.parse("iets geheims")),
                                Ingredient.fromLocalParse(parser.parse("1 cup bouillon")),
                            ),
                    ),
                ),
            tags = setOf(Tag(TagKey.CUISINE, "Frans")),
        )

    private val enrichment =
        Json
            .parseToJsonElement(
                """
                {"ingredients":[
                  {"index":0,"quantity":2,"quantityMax":null,"unit":null,"name":"Ui","note":null},
                  {"index":1,"quantity":1,"quantityMax":null,"unit":"pond","name":"rundvlees","note":null},
                  {"index":2,"quantity":null,"quantityMax":null,"unit":null,"name":null,"note":null},
                  {"index":3,"quantity":240,"quantityMax":null,"unit":"cup","name":" bouillon ","note":"warm"}
                 ],
                 "tags":{"cuisine":["Nederlands"],"dishType":"Hoofdgerecht","mainIngredient":["Rood vlees","Onbekend"],"techniques":["smoren","koekenpan","verzonnen"]}}
                """,
            ).jsonObject

    @Test
    fun `enrichment replaces local parses and never a manual one`() {
        val items = mapping.applyEnrichment(recipe, enrichment).ingredients
        assertEquals("ui", items[0].name)
        assertEquals(ParseSource.CLAUDE, items[0].parseSource)
        assertEquals(recipe.ingredients[0].id, items[0].id)
        assertEquals(recipe.ingredients[1], items[1])
        // Nothing usable from Claude: the line keeps what it had.
        assertEquals(recipe.ingredients[2], items[2])
        // An unknown unit is dropped, strings are trimmed.
        assertNull(items[3].unit)
        assertEquals("bouillon", items[3].name)
        assertEquals(240.0, items[3].quantity)
    }

    @Test
    fun `enrichment fills missing tag keys and drops unknown values`() {
        val tags = mapping.applyEnrichment(recipe, enrichment).tags
        assertEquals(setOf("Frans"), tags.filter { it.key == TagKey.CUISINE }.map { it.value }.toSet())
        assertEquals(setOf("Hoofdgerecht"), tags.filter { it.key == TagKey.DISH_TYPE }.map { it.value }.toSet())
        assertEquals(setOf("Rood vlees"), tags.filter { it.key == TagKey.MAIN_INGREDIENT }.map { it.value }.toSet())
        // Cookware guides are never tags.
        assertEquals(setOf("smoren"), tags.filter { it.key == TagKey.TECHNIQUE }.map { it.value }.toSet())
    }

    @Test
    fun `a photo reply becomes a recipe with groups, extra fields and a source`() {
        val reply =
            Json
                .parseToJsonElement(
                    """
                    {"title":" Lamsschouder ","description":"Langzaam gegaard.","sourceLabel":"Tobias","servings":4,"prepMinutes":20,
                     "cookMinutes":180,"totalMinutes":null,
                     "ingredientGroups":[
                       {"name":null,"items":[{"text":"1 lamsschouder","quantity":1,"quantityMax":null,"unit":null,"name":"lamsschouder","note":null}]},
                       {"name":"Voor de saus","items":[{"text":"2 el kappertjes","quantity":2,"quantityMax":null,"unit":"el","name":"kappertje","note":null}]},
                       {"name":"Leeg","items":[]}],
                     "stepGroups":[{"name":null,"steps":["Braad de schouder aan.","Stoof 3 uur."]}],
                     "extraFields":[{"label":"Wijnpairing","value":"Saint-Émilion"},{"label":"Pagina","value":"112"}],
                     "tags":{"cuisine":["Frans"],"dishType":"Hoofdgerecht","mainIngredient":["Rood vlees"],"techniques":["smoren"]}}
                    """,
                ).jsonObject
        val mapped = mapping.pageRecipe(reply, Recipe(title = "", sourceType = RecipeSourceType.PHOTO))
        assertEquals("Lamsschouder", mapped.title)
        assertEquals("Tobias", mapped.sourceLabel)
        assertEquals(listOf(null, "Voor de saus"), mapped.ingredientGroups.map { it.name })
        assertEquals("el", mapped.ingredientGroups[1].items[0].unit)
        assertEquals(2, mapped.steps.size)
        assertEquals(listOf("Wijnpairing", "Pagina"), mapped.extraFields.map { it.label })
        assertNull(mapped.totalMinutes)
        assertEquals(4, mapped.tags.size)
    }
}
