package nl.potentplum.mise.claude.enrich

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import javax.inject.Inject
import javax.inject.Singleton

/** Runs enrichment in the background; a seam so tests don't need WorkManager. */
fun interface EnrichmentScheduler {
    fun schedule(recipeId: String)
}

/** Unique expedited work per recipe that needs a network connection (spec §14); a newer request replaces an older one. */
class WorkManagerEnrichmentScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : EnrichmentScheduler {
        override fun schedule(recipeId: String) {
            val request =
                OneTimeWorkRequestBuilder<EnrichmentWorker>()
                    .setInputData(workDataOf(EnrichmentWorker.KEY_RECIPE_ID to recipeId))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .addTag(EnrichmentWorker.TAG)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork("${EnrichmentWorker.TAG}-$recipeId", ExistingWorkPolicy.REPLACE, request)
        }
    }

/** Asks for enrichment after a URL import or a save with new or changed lines, when a key is stored (spec §6.9). */
@Singleton
class EnrichmentQueue
    @Inject
    constructor(
        private val secrets: SecretStore,
        private val recipes: RecipeRepository,
        private val scheduler: EnrichmentScheduler,
    ) {
        /** True when enrichment was queued; the recipe then shows "wordt aangevuld" until the worker finishes. */
        suspend fun request(recipeId: String): Boolean {
            if (secrets.keyState.first().status != ClaudeKeyStatus.STORED) return false
            val recipe = recipes.get(recipeId) ?: return false
            if (!RecipeEnricher.needsEnrichment(recipe)) return false
            recipes.setEnrichState(recipeId, EnrichState.PENDING)
            scheduler.schedule(recipeId)
            return true
        }
    }

@Module
@InstallIn(SingletonComponent::class)
interface EnrichmentModule {
    @Binds
    fun scheduler(impl: WorkManagerEnrichmentScheduler): EnrichmentScheduler
}
