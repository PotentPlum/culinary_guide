package nl.potentplum.mise.claude.enrich

import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeModels
import nl.potentplum.mise.claude.ClaudeRequest
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.ContentPart
import nl.potentplum.mise.claude.Effort
import nl.potentplum.mise.claude.OutputSchemas
import nl.potentplum.mise.claude.ResponseMapping
import nl.potentplum.mise.claude.prompts.Prompts
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.db.entity.UsageFeature
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed interface EnrichOutcome {
    /** Claude's parse and tags are saved, or there was nothing left to enrich. */
    data object Done : EnrichOutcome

    /** The recipe is gone, or its ingredient lines changed while Claude was working (a newer run follows). */
    data object Skipped : EnrichOutcome

    data class Failed(
        val error: ClaudeError,
    ) : EnrichOutcome {
        /** Worth another try later: the client already retried these a few times right away (spec §8.1). */
        val retryable: Boolean
            get() = error is ClaudeError.Network || error is ClaudeError.RateLimited || (error is ClaudeError.Http && error.code >= 500)
    }
}

/**
 * Enrichment (spec §6.9, §8.4): one Haiku call per recipe for structured ingredients and tags. `MANUAL` parses and
 * tags the recipe already has are kept. Never touches `enrichState` on failure; the caller decides between a retry and
 * [markFailed].
 */
@Singleton
class RecipeEnricher
    @Inject
    constructor(
        private val recipes: RecipeRepository,
        private val claude: ClaudeService,
        private val content: ContentRepository,
    ) {
        suspend fun enrich(recipeId: String): EnrichOutcome {
            val recipe = recipes.get(recipeId) ?: return EnrichOutcome.Skipped
            if (!needsEnrichment(recipe)) {
                recipes.setEnrichState(recipeId, EnrichState.DONE)
                return EnrichOutcome.Done
            }
            val content = content.content()
            val lines = recipe.ingredients.map { it.text }
            val request =
                ClaudeRequest(
                    model = ClaudeModels.ENRICHMENT,
                    effort = Effort.LOW,
                    system = Prompts.enrichmentSystem(content),
                    content = listOf(ContentPart.Text(Prompts.enrichmentRequest(recipe.title, lines, recipe.steps.map { it.text }))),
                    schema = OutputSchemas(content).enrichment(),
                )
            val reply =
                try {
                    claude.send(UsageFeature.ENRICH, request).json
                } catch (e: ClaudeError) {
                    return EnrichOutcome.Failed(e)
                }
            // The indexes in the reply refer to the lines that were sent; an edit in the meantime makes them stale.
            val latest = recipes.get(recipeId) ?: return EnrichOutcome.Skipped
            if (latest.ingredients.map { it.text } != lines) return EnrichOutcome.Skipped
            val enriched = ResponseMapping(content, recipes.parser()).applyEnrichment(latest, reply)
            recipes.save(enriched.copy(enrichState = EnrichState.DONE))
            return EnrichOutcome.Done
        }

        /** No key means "not enriched yet", not a failure: adding a key later offers it again (spec §6.15). */
        suspend fun markFailed(
            recipeId: String,
            error: ClaudeError,
        ) = recipes.setEnrichState(recipeId, if (error == ClaudeError.NoKey) EnrichState.NONE else EnrichState.FAILED)

        companion object {
            fun needsEnrichment(recipe: Recipe): Boolean {
                val missingTags = TagKey.entries.any { key -> recipe.tags.none { it.key == key } }
                val openLines = recipe.ingredients.any { it.parseSource != ParseSource.MANUAL }
                return recipe.ingredients.isNotEmpty() && (openLines || missingTags)
            }
        }
    }
