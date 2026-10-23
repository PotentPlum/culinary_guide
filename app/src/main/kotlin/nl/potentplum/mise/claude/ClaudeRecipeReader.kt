package nl.potentplum.mise.claude

import kotlinx.coroutines.flow.first
import nl.potentplum.mise.claude.prompts.Prompts
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.UsageFeature
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads a recipe with Claude (spec §6.9, §8.2): from 1–5 cookbook photos, or from page text when a page has no JSON-LD.
 * The model follows Kwaliteit; the result is a draft with structured ingredients and tags, so it skips enrichment.
 */
@Singleton
class ClaudeRecipeReader
    @Inject
    constructor(
        private val claude: ClaudeService,
        private val content: ContentRepository,
        private val settings: SettingsRepository,
        private val recipes: RecipeRepository,
    ) {
        /** [jpegs] are already downscaled (≤ 1568 px); 1–5 of them. */
        suspend fun fromPhotos(jpegs: List<ByteArray>): Recipe {
            require(jpegs.size in 1..MAX_PHOTOS) { "1 to $MAX_PHOTOS photos" }
            val parts = jpegs.map { ContentPart.Image(it) } + ContentPart.Text(Prompts.photoRequest(jpegs.size))
            return read(UsageFeature.PHOTO_IMPORT, parts, Recipe(title = "", sourceType = RecipeSourceType.PHOTO))
        }

        /** The page text fallback (spec §8.5); [text] is the visible text, already trimmed to 60k characters. */
        suspend fun fromPage(
            url: String,
            text: String,
        ): Recipe =
            read(
                UsageFeature.PAGE_IMPORT,
                listOf(ContentPart.Text(Prompts.pageRequest(url, text))),
                Recipe(title = "", sourceType = RecipeSourceType.PAGE, sourceUrl = url),
            )

        private suspend fun read(
            feature: UsageFeature,
            parts: List<ContentPart>,
            base: Recipe,
        ): Recipe {
            val content = content.content()
            val request =
                ClaudeRequest(
                    model = ClaudeModels.forQuality(settings.settings.first().quality),
                    effort = Effort.MEDIUM,
                    system = Prompts.photoSystem(content),
                    content = parts,
                    schema = OutputSchemas(content).pageRecipe(),
                )
            val reply = claude.send(feature, request).json
            return ResponseMapping(content, recipes.parser()).pageRecipe(reply, base).copy(enrichState = EnrichState.DONE)
        }

        companion object {
            const val MAX_PHOTOS = 5
        }
    }
