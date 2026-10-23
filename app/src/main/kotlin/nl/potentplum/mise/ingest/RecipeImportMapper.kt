package nl.potentplum.mise.ingest

import nl.potentplum.mise.data.db.entity.Nutrition
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.SourceMeta
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.domain.IngredientParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * A parsed web page → a [Recipe] (spec §10 "Mapping to Room"): default section names become untitled groups, metric
 * text wins, every line goes through the local parser, and Schema.org extras go to [SourceMeta] (stored, not shown).
 */
class RecipeImportMapper(
    private val parser: IngredientParser,
) {
    fun map(
        output: RecipeOutput,
        sourceUrl: String,
        now: Long,
    ): Recipe =
        Recipe(
            title = output.title,
            description = output.description,
            sourceType = RecipeSourceType.URL,
            sourceUrl = sourceUrl,
            sourceLabel = sourceLabel(sourceUrl),
            author = output.author,
            imageUrl = output.image,
            servings = output.servings?.takeIf { it in 1..99 },
            prepMinutes = output.prepTimeMinutes,
            cookMinutes = output.cookTimeMinutes,
            totalMinutes = output.totalTimeMinutes ?: sumOrNull(output.prepTimeMinutes, output.cookTimeMinutes),
            sourceMeta =
                SourceMeta(
                    ratingValue = output.aggregateRating?.ratingValue,
                    ratingCount = output.aggregateRating?.ratingCount?.toInt(),
                    datePublished = output.datePublished,
                    keywords = output.keywords,
                    diet = output.suitableForDiet,
                    nutrition = output.nutrition.let { Nutrition(it.calories, it.protein, it.carbs, it.fat) },
                    video = output.video,
                ),
            createdAt = now,
            updatedAt = now,
            ingredientGroups =
                output.ingredientSections
                    .map { section ->
                        IngredientGroup(
                            name = groupName(section.sectionName),
                            items =
                                section.ingredients
                                    .map { (it.textMetric ?: it.text).trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { Ingredient.fromLocalParse(parser.parse(it)) },
                        )
                    }.filter { it.items.isNotEmpty() },
            stepGroups =
                output.instructionSections
                    .map { section ->
                        StepGroup(
                            name = groupName(section.sectionName),
                            steps =
                                section.steps
                                    .map { it.text.trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { Step(text = it) },
                        )
                    }.filter { it.steps.isNotEmpty() },
        )

    private fun sumOrNull(
        a: Int?,
        b: Int?,
    ): Int? = if (a == null && b == null) null else (a ?: 0) + (b ?: 0)

    companion object {
        /** The parser's (and WPRM's) default section names, which mean "no header" (spec §10). */
        private val DEFAULT_SECTION_NAMES = setOf("ingredients", "instructions", "ingrediënten", "bereiding")

        fun groupName(sectionName: String): String? =
            sectionName.trim().takeUnless {
                it.isEmpty() ||
                    it.lowercase() in DEFAULT_SECTION_NAMES
            }

        /** "https://www.leukerecepten.nl/…" → "leukerecepten.nl". */
        fun sourceLabel(url: String): String = url.toHttpUrlOrNull()?.host?.removePrefix("www.") ?: ""
    }
}
