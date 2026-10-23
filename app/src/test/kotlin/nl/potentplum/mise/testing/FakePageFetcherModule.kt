package nl.potentplum.mise.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import nl.potentplum.mise.core.network.FetcherModule
import nl.potentplum.mise.ingest.FetchedPage
import nl.potentplum.mise.ingest.PageFetcher
import nl.potentplum.mise.ingest.RecipeImportError

/** Pages by URL for UI tests; anything else is unreachable. */
object FakePageFetcher : PageFetcher {
    val pages = mutableMapOf<String, String>()

    override suspend fun fetch(input: String): FetchedPage = FetchedPage(input, pages[input] ?: throw RecipeImportError.Unreachable)

    fun recipePage(
        name: String,
        ingredients: List<String>,
        steps: List<String>,
    ): String {
        fun list(items: List<String>) = items.joinToString(",") { "\"$it\"" }
        return """<html><head><script type="application/ld+json">{"@type":"Recipe","name":"$name","recipeYield":"4",
            "recipeIngredient":[${list(ingredients)}],"recipeInstructions":[${list(steps)}]}</script></head></html>"""
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [FetcherModule::class])
object FakePageFetcherModule {
    @Provides
    fun pageFetcher(): PageFetcher = FakePageFetcher
}
