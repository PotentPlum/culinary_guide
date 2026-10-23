package nl.potentplum.mise.feature.recipes

import kotlinx.serialization.Serializable
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.ExtraField
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.data.repo.newId
import nl.potentplum.mise.domain.IngredientParser

/** A group being edited: its optional header and its lines as text, one ingredient or step per line. */
@Serializable
data class EditableGroup(
    val id: String = newId(),
    val name: String = "",
    val text: String = "",
)

@Serializable
data class EditableField(
    val id: String = newId(),
    val label: String = "",
    val value: String = "",
)

/** Everything the edit screen holds; serialisable, so an edit survives process death (spec §15.5). */
@Serializable
data class RecipeEditorState(
    val id: String,
    val title: String = "",
    val description: String = "",
    val servings: String = "",
    val prepMinutes: String = "",
    val cookMinutes: String = "",
    val totalMinutes: String = "",
    val sourceLabel: String = "",
    val sourceUrl: String = "",
    val notes: String = "",
    val imagePath: String? = null,
    val ingredientGroups: List<EditableGroup> = listOf(EditableGroup()),
    val stepGroups: List<EditableGroup> = listOf(EditableGroup()),
    val extraFields: List<EditableField> = emptyList(),
    val cuisines: Set<String> = emptySet(),
    val dishType: String? = null,
    val mainIngredients: Set<String> = emptySet(),
    val techniques: Set<String> = emptySet(),
) {
    val hasTitle: Boolean get() = title.isNotBlank()
    val hasIngredients: Boolean get() = ingredientGroups.any { lines(it.text).isNotEmpty() }
    val hasSteps: Boolean get() = stepGroups.any { lines(it.text).isNotEmpty() }
}

internal fun lines(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

object RecipeEditorMapper {
    fun fromRecipe(recipe: Recipe): RecipeEditorState =
        RecipeEditorState(
            id = recipe.id,
            title = recipe.title,
            description = recipe.description,
            servings = recipe.servings?.toString().orEmpty(),
            prepMinutes = recipe.prepMinutes?.toString().orEmpty(),
            cookMinutes = recipe.cookMinutes?.toString().orEmpty(),
            totalMinutes = recipe.totalMinutes?.toString().orEmpty(),
            sourceLabel = recipe.sourceLabel,
            sourceUrl = recipe.sourceUrl.orEmpty(),
            notes = recipe.notes.orEmpty(),
            imagePath = recipe.imagePath,
            ingredientGroups =
                recipe.ingredientGroups
                    .map { group -> EditableGroup(group.id, group.name.orEmpty(), group.items.joinToString("\n") { it.text }) }
                    .ifEmpty { listOf(EditableGroup()) },
            stepGroups =
                recipe.stepGroups
                    .map { group -> EditableGroup(group.id, group.name.orEmpty(), group.steps.joinToString("\n") { it.text }) }
                    .ifEmpty { listOf(EditableGroup()) },
            extraFields = recipe.extraFields.map { EditableField(it.id, it.label, it.value) },
            cuisines = recipe.tagValues(TagKey.CUISINE).toSet(),
            dishType = recipe.tagValues(TagKey.DISH_TYPE).firstOrNull(),
            mainIngredients = recipe.tagValues(TagKey.MAIN_INGREDIENT).toSet(),
            techniques = recipe.tagValues(TagKey.TECHNIQUE).toSet(),
        )

    /**
     * The edited recipe. Lines whose text is unchanged keep their structured fields (a hand-corrected `MANUAL` parse
     * survives); changed and new lines are parsed locally at once (spec §6.8). Empty groups and fields are dropped.
     */
    fun toRecipe(
        state: RecipeEditorState,
        original: Recipe?,
        parser: IngredientParser,
    ): Recipe {
        val ingredientPool =
            original
                ?.ingredients
                .orEmpty()
                .groupBy { it.text }
                .mapValues { it.value.toMutableList() }
                .toMutableMap()
        val stepPool =
            original
                ?.steps
                .orEmpty()
                .groupBy { it.text }
                .mapValues { it.value.toMutableList() }
                .toMutableMap()
        val base = original ?: Recipe(id = state.id, title = "", sourceType = RecipeSourceType.MANUAL)
        return base.copy(
            title = state.title.trim(),
            description = state.description.trim(),
            servings =
                state.servings
                    .trim()
                    .toIntOrNull()
                    ?.takeIf { it in 1..99 },
            prepMinutes = minutes(state.prepMinutes),
            cookMinutes = minutes(state.cookMinutes),
            totalMinutes = minutes(state.totalMinutes),
            sourceLabel = state.sourceLabel.trim(),
            sourceUrl = state.sourceUrl.trim().ifEmpty { null },
            notes = state.notes.trim().ifEmpty { null },
            imagePath = state.imagePath,
            ingredientGroups =
                state.ingredientGroups
                    .map { group ->
                        IngredientGroup(
                            id = group.id,
                            name = group.name.trim().ifEmpty { null },
                            items =
                                lines(group.text).map { line ->
                                    ingredientPool[line]?.removeFirstOrNull() ?: Ingredient.fromLocalParse(parser.parse(line))
                                },
                        )
                    }.filter { it.items.isNotEmpty() || it.name != null },
            stepGroups =
                state.stepGroups
                    .map { group ->
                        StepGroup(
                            id = group.id,
                            name = group.name.trim().ifEmpty { null },
                            steps = lines(group.text).map { line -> stepPool[line]?.removeFirstOrNull() ?: Step(text = line) },
                        )
                    }.filter { it.steps.isNotEmpty() || it.name != null },
            extraFields =
                state.extraFields
                    .filter { it.label.isNotBlank() && it.value.isNotBlank() }
                    .map { ExtraField(it.id, it.label.trim(), it.value.trim()) },
            tags =
                buildSet {
                    state.cuisines.forEach { add(Tag(TagKey.CUISINE, it)) }
                    state.dishType?.let { add(Tag(TagKey.DISH_TYPE, it)) }
                    state.mainIngredients.forEach { add(Tag(TagKey.MAIN_INGREDIENT, it)) }
                    state.techniques.forEach { add(Tag(TagKey.TECHNIQUE, it)) }
                },
        )
    }

    private fun minutes(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }

    /** True when any ingredient line is new or changed: those need (re-)enrichment. */
    fun ingredientsChanged(
        saved: Recipe,
        original: Recipe?,
    ): Boolean {
        val before =
            original
                ?.ingredients
                .orEmpty()
                .map { it.id }
                .toSet()
        return saved.ingredients.any { it.id !in before }
    }
}
