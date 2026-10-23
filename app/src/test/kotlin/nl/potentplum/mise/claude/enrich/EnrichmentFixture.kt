package nl.potentplum.mise.claude.enrich

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import nl.potentplum.mise.claude.ClaudeClient
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeRequest
import nl.potentplum.mise.claude.ClaudeResponse
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.KeyCheck
import nl.potentplum.mise.claude.Usage
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.MiseDatabase
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.FakeCipher
import nl.potentplum.mise.testing.FakeContentRepository
import java.io.File
import java.time.LocalDate

/** A recorded enrichment reply, a fake client and a real in-memory database. */
class EnrichmentFixture(
    folder: File,
    scope: CoroutineScope,
    dispatcher: TestDispatcher,
) {
    val db: MiseDatabase =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    val recipes = RecipeRepository(db.recipeDao(), db.attemptDao(), FakeContentRepository(), MiseClock.fixed(LocalDate.of(2026, 10, 22)))
    val secrets =
        SecretStore(
            PreferenceDataStoreFactory.create(scope = scope) { File(folder, "secrets.preferences_pb") },
            FakeCipher(),
            dispatcher,
        )
    val client = FakeClient()
    val service = ClaudeService(client, secrets, db.usageDao(), MiseClock.fixed(LocalDate.of(2026, 10, 22)))
    val enricher = RecipeEnricher(recipes, service, FakeContentRepository())

    class FakeClient : ClaudeClient {
        var reply: JsonObject = REPLY
        var failure: ClaudeError? = null
        var calls = 0
        var lastRequest: ClaudeRequest? = null

        /** Runs while the "request" is in flight, to simulate an edit in the meantime. */
        var during: suspend () -> Unit = {}

        override suspend fun send(
            key: String,
            request: ClaudeRequest,
        ): ClaudeResponse {
            calls++
            lastRequest = request
            during()
            failure?.let { throw it }
            return ClaudeResponse(reply, Usage(1_200, 300), request.model)
        }

        override suspend fun checkKey(key: String) = KeyCheck.VALID
    }

    companion object {
        val recipe =
            Recipe(
                id = "r1",
                title = "Pompoensoep",
                sourceType = RecipeSourceType.URL,
                ingredientGroups =
                    listOf(
                        IngredientGroup(
                            id = "g1",
                            items =
                                listOf(
                                    Ingredient(id = "i0", text = "1 flinke pompoen (ca. 1 kg)"),
                                    Ingredient(id = "i1", text = "2 uien", quantity = 2.0, name = "uien", parseSource = ParseSource.LOCAL),
                                    Ingredient(id = "i2", text = "zout", name = "zeezout", parseSource = ParseSource.MANUAL),
                                ),
                        ),
                    ),
                stepGroups = listOf(StepGroup(id = "s1", steps = listOf(Step(id = "st1", text = "Snipper de ui.")))),
                tags = setOf(Tag(TagKey.CUISINE, "Frans")),
            )

        val REPLY: JsonObject =
            Json
                .parseToJsonElement(
                    """
                    {
                      "ingredients": [
                        {"index": 0, "quantity": 1, "quantityMax": null, "unit": "kg", "name": "pompoen", "note": "flink"},
                        {"index": 1, "quantity": 2, "quantityMax": null, "unit": null, "name": "ui", "note": null},
                        {"index": 2, "quantity": null, "quantityMax": null, "unit": null, "name": "zout", "note": null}
                      ],
                      "tags": {
                        "cuisine": ["Italiaans"],
                        "dishType": "Soep",
                        "mainIngredient": ["Groenten", "Ruimtevoer"],
                        "techniques": ["snipperen"]
                      }
                    }
                    """.trimIndent(),
                ).jsonObject
    }
}
