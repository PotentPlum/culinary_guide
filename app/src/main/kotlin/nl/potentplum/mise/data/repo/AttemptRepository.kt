package nl.potentplum.mise.data.repo

import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.SeasonStatus
import nl.potentplum.mise.domain.SeasonalMatcher
import nl.potentplum.mise.domain.Seasonality
import nl.potentplum.mise.ingest.ImageStore
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** What the evaluation sheet hands over (spec §6.12). Scores are the stored 2/4/6/8/10 values, or null. */
data class Evaluation(
    val cookedOn: LocalDate,
    val score: Int? = null,
    val tasteScore: Int? = null,
    val notes: String? = null,
    /** A photo staged in the import folder; moved to the journal when logged. */
    val stagedPhotoPath: String? = null,
)

/** The snapshot an attempt keeps of its recipe (spec §7.2, §11.4), so the journal survives edits and deletes. */
object AttemptSnapshot {
    fun of(
        id: String,
        recipe: Recipe,
        questId: String?,
        cookedAt: Long,
        cookedOn: LocalDate,
        content: Content,
    ): AttemptEntity {
        val vegIds = SeasonalMatcher(content.vegetables).vegetableIds(recipe.ingredients.map { it.ref })
        val month = cookedOn.monthValue
        return AttemptEntity(
            id = id,
            sourceType = if (questId != null) AttemptSourceType.QUEST else AttemptSourceType.RECIPE,
            recipeId = recipe.id,
            questId = questId,
            cookedAt = cookedAt,
            techniques = recipe.tagValues(TagKey.TECHNIQUE),
            cuisines = recipe.tagValues(TagKey.CUISINE),
            vegIds = vegIds.sorted(),
            peakVegIds =
                vegIds
                    .filter { id -> content.vegetable(id)?.let { Seasonality.statusFor(it, month) } == SeasonStatus.PEAK }
                    .sorted(),
        )
    }
}

@Singleton
class AttemptRepository
    @Inject
    constructor(
        private val attemptDao: AttemptDao,
        private val recipes: RecipeRepository,
        private val quests: QuestRepository,
        private val settings: SettingsRepository,
        private val images: ImageStore,
        private val content: ContentRepository,
        private val clock: MiseClock,
    ) {
        fun observeAll(): Flow<List<AttemptEntity>> = attemptDao.observeAll()

        /**
         * Logs a cook of [recipeId] (spec §6.12): snapshots, the photo, and quest completion when the recipe is a quest's,
         * whichever screen the cook ended on. Clears the recipe's cook session and Vanavond pin. Null when the recipe is gone.
         */
        suspend fun log(
            recipeId: String,
            evaluation: Evaluation,
        ): AttemptEntity? {
            val recipe = recipes.get(recipeId) ?: return null
            val quest = quests.forRecipe(recipeId)
            val now = clock.zonedNow()
            val cookedAt =
                if (evaluation.cookedOn == now.toLocalDate()) {
                    now.toInstant().toEpochMilli()
                } else {
                    evaluation.cookedOn
                        .atTime(now.toLocalTime())
                        .atZone(MiseClock.ZONE)
                        .toInstant()
                        .toEpochMilli()
                }
            val id = newId()
            val attempt =
                AttemptSnapshot.of(id, recipe, quest?.id, cookedAt, evaluation.cookedOn, content.content()).copy(
                    score = evaluation.score,
                    tasteScore = evaluation.tasteScore,
                    notes = evaluation.notes?.trim()?.takeIf { it.isNotEmpty() },
                    photoPath = evaluation.stagedPhotoPath?.let { images.saveJournalPhoto(id, it) },
                )
            attemptDao.upsert(attempt)
            quest?.let { quests.markCompleted(it.id, cookedAt) }
            settings.clearCookSession(recipeId)
            settings.unpinTonight(recipeId)
            return attempt
        }
    }
