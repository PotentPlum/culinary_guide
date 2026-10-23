package nl.potentplum.mise.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.MiseDatabase
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.dao.QuestDao
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.domain.PastQuest
import javax.inject.Inject
import javax.inject.Singleton

/** Quests and their hidden recipes (spec §6.11, §7.2). */
@Singleton
class QuestRepository
    @Inject
    constructor(
        private val db: MiseDatabase,
        private val questDao: QuestDao,
        private val attemptDao: AttemptDao,
        private val clock: MiseClock,
    ) {
        fun observeAll(): Flow<List<QuestEntity>> = questDao.observeAll()

        fun observe(id: String): Flow<QuestEntity?> = questDao.observe(id)

        suspend fun get(id: String): QuestEntity? = questDao.get(id)

        suspend fun forRecipe(recipeId: String): QuestEntity? = questDao.forRecipe(recipeId)

        /** Today's quest van vandaag: the newest daily quest created today (a regenerated one replaces it). */
        fun observeDaily(): Flow<QuestEntity?> {
            val start =
                clock
                    .today()
                    .atStartOfDay(MiseClock.ZONE)
                    .toInstant()
                    .toEpochMilli()
            return questDao.observeDailySince(start)
        }

        /** The quest and its recipe in one transaction, so a cancelled or failed generation leaves nothing behind. */
        suspend fun insert(
            quest: QuestEntity,
            recipe: Recipe,
        ) {
            val rows = RecipeRows(recipe)
            db.withTransaction {
                db.recipeDao().replaceGraph(
                    rows.recipe,
                    rows.ingredientGroups,
                    rows.ingredients,
                    rows.stepGroups,
                    rows.steps,
                    rows.extraFields,
                    rows.tags,
                )
                questDao.upsert(quest)
            }
        }

        /** Sets COMPLETED once; a later attempt keeps the first completion date. */
        suspend fun markCompleted(
            id: String,
            at: Long = clock.nowMillis(),
        ) {
            val quest = questDao.get(id) ?: return
            if (quest.status == QuestStatus.COMPLETED) return
            questDao.upsert(quest.copy(status = QuestStatus.COMPLETED, completedAt = at))
        }

        /** Deletes the quest and its hidden recipe; attempts keep their snapshots (spec §7.2). */
        suspend fun delete(id: String) {
            questDao.get(id)?.let { questDao.deleteWithRecipe(it) }
        }

        /** Input for [nl.potentplum.mise.domain.QuestDefaults]: every quest with its title and latest execution score. */
        suspend fun pastQuests(): List<PastQuest> = questDao.pastQuests()

        suspend fun attempts() = attemptDao.all()
    }
