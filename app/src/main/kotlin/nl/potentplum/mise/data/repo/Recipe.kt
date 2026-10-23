package nl.potentplum.mise.data.repo

import nl.potentplum.mise.data.db.dao.RecipeGraph
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.IngredientEntity
import nl.potentplum.mise.data.db.entity.IngredientGroupEntity
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeEntity
import nl.potentplum.mise.data.db.entity.RecipeExtraFieldEntity
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.RecipeTagEntity
import nl.potentplum.mise.data.db.entity.SourceMeta
import nl.potentplum.mise.data.db.entity.StepEntity
import nl.potentplum.mise.data.db.entity.StepGroupEntity
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.domain.IngredientRef
import nl.potentplum.mise.domain.ParsedIngredient
import java.util.UUID

/** A whole recipe as the app works with it: ordered groups, structured ingredients, steps, extra fields and tags. */
data class Recipe(
    val id: String = newId(),
    val title: String,
    val description: String = "",
    val sourceType: RecipeSourceType,
    val sourceUrl: String? = null,
    val sourceLabel: String = "",
    val author: String? = null,
    val imagePath: String? = null,
    val imageUrl: String? = null,
    val servings: Int? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val totalMinutes: Int? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val inLibrary: Boolean = true,
    val enrichState: EnrichState = EnrichState.NONE,
    val sourceMeta: SourceMeta? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val ingredientGroups: List<IngredientGroup> = emptyList(),
    val stepGroups: List<StepGroup> = emptyList(),
    val extraFields: List<ExtraField> = emptyList(),
    val tags: Set<Tag> = emptySet(),
) {
    val ingredients: List<Ingredient> get() = ingredientGroups.flatMap { it.items }
    val steps: List<Step> get() = stepGroups.flatMap { it.steps }

    fun tagValues(key: TagKey): List<String> = tags.filter { it.key == key }.map { it.value }
}

data class IngredientGroup(
    val id: String = newId(),
    /** Null = the untitled default group. */
    val name: String? = null,
    val items: List<Ingredient> = emptyList(),
)

data class Ingredient(
    val id: String = newId(),
    val text: String,
    val quantity: Double? = null,
    val quantityMax: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    val note: String? = null,
    val parseSource: ParseSource = ParseSource.NONE,
) {
    val parsed: ParsedIngredient
        get() = ParsedIngredient(text, quantity, quantityMax, unit, name, note, parsed = parseSource != ParseSource.NONE)

    val ref: IngredientRef get() = IngredientRef(name, text)

    companion object {
        /** A line structured by the local parser (spec §11.2). */
        fun fromLocalParse(
            parse: ParsedIngredient,
            id: String = newId(),
        ): Ingredient =
            Ingredient(
                id = id,
                text = parse.text,
                quantity = parse.quantity,
                quantityMax = parse.quantityMax,
                unit = parse.unit,
                name = parse.name,
                note = parse.note,
                parseSource = if (parse.parsed) ParseSource.LOCAL else ParseSource.NONE,
            )
    }
}

data class StepGroup(
    val id: String = newId(),
    val name: String? = null,
    val steps: List<Step> = emptyList(),
)

data class Step(
    val id: String = newId(),
    val text: String,
)

data class ExtraField(
    val id: String = newId(),
    val label: String,
    val value: String,
)

data class Tag(
    val key: TagKey,
    val value: String,
)

fun newId(): String = UUID.randomUUID().toString()

// ── Entity mapping ───────────────────────────────────────────────────────────

fun RecipeGraph.toRecipe(): Recipe =
    Recipe(
        id = recipe.id,
        title = recipe.title,
        description = recipe.description,
        sourceType = recipe.sourceType,
        sourceUrl = recipe.sourceUrl,
        sourceLabel = recipe.sourceLabel,
        author = recipe.author,
        imagePath = recipe.imagePath,
        imageUrl = recipe.imageUrl,
        servings = recipe.servings,
        prepMinutes = recipe.prepMinutes,
        cookMinutes = recipe.cookMinutes,
        totalMinutes = recipe.totalMinutes,
        notes = recipe.notes,
        isFavorite = recipe.isFavorite,
        inLibrary = recipe.inLibrary,
        enrichState = recipe.enrichState,
        sourceMeta = recipe.sourceMeta,
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt,
        ingredientGroups =
            ingredientGroups.sortedBy { it.group.position }.map { group ->
                IngredientGroup(
                    id = group.group.id,
                    name = group.group.name,
                    items =
                        group.items.sortedBy { it.position }.map {
                            Ingredient(it.id, it.text, it.quantity, it.quantityMax, it.unit, it.name, it.note, it.parseSource)
                        },
                )
            },
        stepGroups =
            stepGroups.sortedBy { it.group.position }.map { group ->
                StepGroup(group.group.id, group.group.name, group.steps.sortedBy { it.position }.map { Step(it.id, it.text) })
            },
        extraFields = extraFields.sortedBy { it.position }.map { ExtraField(it.id, it.label, it.value) },
        tags = tags.map { Tag(it.key, it.value) }.toSet(),
    )

/** The rows [nl.potentplum.mise.data.db.dao.RecipeDao.replaceGraph] writes, with positions from list order. */
class RecipeRows(
    recipe: Recipe,
) {
    val recipe =
        RecipeEntity(
            id = recipe.id,
            title = recipe.title,
            description = recipe.description,
            sourceType = recipe.sourceType,
            sourceUrl = recipe.sourceUrl,
            sourceLabel = recipe.sourceLabel,
            author = recipe.author,
            imagePath = recipe.imagePath,
            imageUrl = recipe.imageUrl,
            servings = recipe.servings,
            prepMinutes = recipe.prepMinutes,
            cookMinutes = recipe.cookMinutes,
            totalMinutes = recipe.totalMinutes,
            notes = recipe.notes,
            isFavorite = recipe.isFavorite,
            inLibrary = recipe.inLibrary,
            enrichState = recipe.enrichState,
            sourceMeta = recipe.sourceMeta,
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt,
        )
    val ingredientGroups = recipe.ingredientGroups.mapIndexed { i, g -> IngredientGroupEntity(g.id, recipe.id, i, g.name) }
    val ingredients =
        recipe.ingredientGroups.flatMap { group ->
            group.items.mapIndexed { i, it ->
                IngredientEntity(it.id, group.id, i, it.text, it.quantity, it.quantityMax, it.unit, it.name, it.note, it.parseSource)
            }
        }
    val stepGroups = recipe.stepGroups.mapIndexed { i, g -> StepGroupEntity(g.id, recipe.id, i, g.name) }
    val steps = recipe.stepGroups.flatMap { group -> group.steps.mapIndexed { i, it -> StepEntity(it.id, group.id, i, it.text) } }
    val extraFields = recipe.extraFields.mapIndexed { i, it -> RecipeExtraFieldEntity(it.id, recipe.id, i, it.label, it.value) }
    val tags = recipe.tags.map { RecipeTagEntity(recipe.id, it.key, it.value) }
}
