package nl.potentplum.mise.claude.enrich

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import nl.potentplum.mise.R
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.platform.BackgroundNotifications

/** One recipe's enrichment (spec §6.9). A failure never touches the saved recipe; it only flags it and notifies. */
@HiltWorker
class EnrichmentWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val enricher: RecipeEnricher,
        private val recipes: RecipeRepository,
        private val notifications: BackgroundNotifications,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val recipeId = inputData.getString(KEY_RECIPE_ID) ?: return Result.failure()
            return when (val outcome = enricher.enrich(recipeId)) {
                EnrichOutcome.Done, EnrichOutcome.Skipped -> {
                    Result.success()
                }

                is EnrichOutcome.Failed -> {
                    if (outcome.retryable && runAttemptCount < MAX_RETRIES) return Result.retry()
                    enricher.markFailed(recipeId, outcome.error)
                    if (outcome.error != ClaudeError.NoKey) {
                        recipes.get(recipeId)?.let { notifications.enrichmentFailed(it.id, it.title) }
                    }
                    Result.failure()
                }
            }
        }

        /** Expedited work runs as a foreground service on API 30 and needs this. */
        override suspend fun getForegroundInfo(): ForegroundInfo =
            ForegroundInfo(
                BackgroundNotifications.ENRICHING_ID,
                notifications.progress(applicationContext.getString(R.string.recipe_enriching)),
            )

        companion object {
            const val KEY_RECIPE_ID = "recipeId"
            const val TAG = "enrich"
            const val MAX_RETRIES = 3
        }
    }
