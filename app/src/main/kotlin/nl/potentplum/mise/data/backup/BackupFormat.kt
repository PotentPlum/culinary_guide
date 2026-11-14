package nl.potentplum.mise.data.backup

import kotlinx.serialization.Serializable
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.Band
import nl.potentplum.mise.data.db.entity.CheckAttemptEntity
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.LessonProgressEntity
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestIngredient
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity
import nl.potentplum.mise.data.db.entity.SourceMeta
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.db.entity.TimeLimit
import nl.potentplum.mise.data.repo.ExtraField
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.data.settings.LibrarySort
import nl.potentplum.mise.data.settings.Quality
import nl.potentplum.mise.domain.Difficulty

/*
 * `backup.json` inside a Mise backup zip (spec §13.1), documented in `data/schemas/backup.schema.json`; the legacy
 * converter writes the same format. These classes are the contract: renaming a field or an enum constant changes the
 * format and needs a new schemaVersion. Timestamps are epoch millis (UTC). Image paths are paths inside the zip.
 */

const val BACKUP_FORMAT = "mise-backup"
const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupFile(
    /** Always [BACKUP_FORMAT]; required, so other JSON is never taken for a backup. */
    val format: String,
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val recipes: List<BackupRecipe> = emptyList(),
    val quests: List<BackupQuest> = emptyList(),
    val attempts: List<BackupAttempt> = emptyList(),
    val shoppingItems: List<BackupShoppingItem> = emptyList(),
    /** Curriculum progress (spec §6.17); optional, so older backups and older apps keep working. */
    val lessonProgress: List<BackupLessonProgress> = emptyList(),
    val checkAttempts: List<BackupCheckAttempt> = emptyList(),
    val evidence: List<BackupEvidence> = emptyList(),
    /** The non-secret settings, without the day and session state (spec §7.3). */
    val settings: BackupSettings? = null,
)

@Serializable
data class BackupRecipe(
    val id: String,
    val title: String,
    val description: String = "",
    val sourceType: RecipeSourceType,
    val sourceUrl: String? = null,
    val sourceLabel: String = "",
    val author: String? = null,
    /** "images/{id}.jpg" when the zip holds the image. */
    val image: String? = null,
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
    val createdAt: Long,
    val updatedAt: Long,
    val ingredientGroups: List<BackupIngredientGroup> = emptyList(),
    val stepGroups: List<BackupStepGroup> = emptyList(),
    val extraFields: List<BackupExtraField> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
)

@Serializable
data class BackupIngredientGroup(
    val id: String,
    val name: String? = null,
    val items: List<BackupIngredient> = emptyList(),
)

@Serializable
data class BackupIngredient(
    val id: String,
    val text: String,
    val quantity: Double? = null,
    val quantityMax: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    val note: String? = null,
    val parseSource: ParseSource = ParseSource.NONE,
)

@Serializable
data class BackupStepGroup(
    val id: String,
    val name: String? = null,
    val steps: List<BackupStep> = emptyList(),
)

@Serializable
data class BackupStep(
    val id: String,
    val text: String,
)

@Serializable
data class BackupExtraField(
    val id: String,
    val label: String,
    val value: String,
)

@Serializable
data class BackupTag(
    val key: TagKey,
    val value: String,
)

@Serializable
data class BackupQuest(
    val id: String,
    val createdAt: Long,
    val isDaily: Boolean = false,
    val status: QuestStatus = QuestStatus.OPEN,
    val completedAt: Long? = null,
    val ingredientMode: IngredientMode,
    val ingredient: String,
    val vegId: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val questType: QuestType,
    val requestedDifficulty: Difficulty? = null,
    val focusDiscipline: DisciplineId? = null,
    val requestedTechniqueId: String? = null,
    val requestedCuisine: String? = null,
    val timeLimit: TimeLimit? = null,
    val simpleAndSmall: Boolean = false,
    val text: String,
    val skill: String,
    val difficulty: Difficulty,
    val timeEstimateMinutes: Int,
    val why: String,
    val dishType: String,
    val tips: List<String> = emptyList(),
    val recipeId: String,
    /** Ingredients after the first, for quests that use several (spec §6.11). */
    val moreIngredients: List<QuestIngredient> = emptyList(),
)

@Serializable
data class BackupAttempt(
    val id: String,
    val sourceType: AttemptSourceType,
    val recipeId: String? = null,
    val questId: String? = null,
    val cookedAt: Long,
    val score: Int? = null,
    val tasteScore: Int? = null,
    val notes: String? = null,
    /** "journal/{id}.jpg" when the zip holds the photo. */
    val photo: String? = null,
    val techniques: List<String> = emptyList(),
    val cuisines: List<String> = emptyList(),
    val vegIds: List<String> = emptyList(),
    val peakVegIds: List<String> = emptyList(),
)

@Serializable
data class BackupShoppingItem(
    val id: String,
    val text: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    val recipeId: String? = null,
    val checked: Boolean = false,
    val position: Int,
    val createdAt: Long,
)

@Serializable
data class BackupLessonProgress(
    val lessonId: String,
    val completedAt: Long,
)

@Serializable
data class BackupCheckAttempt(
    val id: String,
    val moduleId: String,
    val takenAt: Long,
    val total: Int,
    val correct: Int,
    val safetyTotal: Int,
    val safetyCorrect: Int,
    val passed: Boolean,
    val wrongItemIds: List<String> = emptyList(),
)

@Serializable
data class BackupEvidence(
    val id: String,
    val exerciseId: String,
    val loggedAt: Long,
    val band: Band,
    val notes: String? = null,
    /** "evidence/{id}.jpg" when the zip holds the photo. */
    val photo: String? = null,
)

@Serializable
data class BackupSettings(
    val quality: Quality? = null,
    val seasonalNotificationsEnabled: Boolean? = null,
    val librarySort: LibrarySort? = null,
    val showQuestCard: Boolean? = null,
)

// ── Mapping ──────────────────────────────────────────────────────────────────

fun imageEntry(recipeId: String) = "images/$recipeId.jpg"

fun photoEntry(attemptId: String) = "journal/$attemptId.jpg"

fun evidenceEntry(evidenceId: String) = "evidence/$evidenceId.jpg"

fun Recipe.toBackup(hasImage: Boolean) =
    BackupRecipe(
        id = id,
        title = title,
        description = description,
        sourceType = sourceType,
        sourceUrl = sourceUrl,
        sourceLabel = sourceLabel,
        author = author,
        image = if (hasImage) imageEntry(id) else null,
        imageUrl = imageUrl,
        servings = servings,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        totalMinutes = totalMinutes,
        notes = notes,
        isFavorite = isFavorite,
        inLibrary = inLibrary,
        // An interrupted enrichment is not resumed after a restore; it is offered again (spec §13.2).
        enrichState = if (enrichState == EnrichState.PENDING) EnrichState.NONE else enrichState,
        sourceMeta = sourceMeta,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ingredientGroups =
            ingredientGroups.map { g ->
                BackupIngredientGroup(
                    g.id,
                    g.name,
                    g.items.map {
                        BackupIngredient(
                            it.id,
                            it.text,
                            it.quantity,
                            it.quantityMax,
                            it.unit,
                            it.name,
                            it.note,
                            it.parseSource,
                        )
                    },
                )
            },
        stepGroups = stepGroups.map { g -> BackupStepGroup(g.id, g.name, g.steps.map { BackupStep(it.id, it.text) }) },
        extraFields = extraFields.map { BackupExtraField(it.id, it.label, it.value) },
        tags = tags.map { BackupTag(it.key, it.value) }.sortedWith(compareBy({ it.key }, { it.value })),
    )

fun BackupRecipe.toRecipe(imagePath: String?) =
    Recipe(
        id = id,
        title = title,
        description = description,
        sourceType = sourceType,
        sourceUrl = sourceUrl,
        sourceLabel = sourceLabel,
        author = author,
        imagePath = imagePath,
        imageUrl = imageUrl,
        servings = servings,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        totalMinutes = totalMinutes,
        notes = notes,
        isFavorite = isFavorite,
        inLibrary = inLibrary,
        enrichState = enrichState,
        sourceMeta = sourceMeta,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ingredientGroups =
            ingredientGroups.map { g ->
                IngredientGroup(
                    g.id,
                    g.name,
                    g.items.map { Ingredient(it.id, it.text, it.quantity, it.quantityMax, it.unit, it.name, it.note, it.parseSource) },
                )
            },
        stepGroups = stepGroups.map { g -> StepGroup(g.id, g.name, g.steps.map { Step(it.id, it.text) }) },
        extraFields = extraFields.map { ExtraField(it.id, it.label, it.value) },
        tags = tags.map { Tag(it.key, it.value) }.toSet(),
    )

fun QuestEntity.toBackup() =
    BackupQuest(
        id,
        createdAt,
        isDaily,
        status,
        completedAt,
        ingredientMode,
        ingredient,
        vegId,
        quantity,
        unit,
        questType,
        requestedDifficulty,
        focusDiscipline,
        requestedTechniqueId,
        requestedCuisine,
        timeLimit,
        simpleAndSmall,
        text,
        skill,
        difficulty,
        timeEstimateMinutes,
        why,
        dishType,
        tips,
        recipeId,
        moreIngredients,
    )

fun BackupQuest.toEntity() =
    QuestEntity(
        id,
        createdAt,
        isDaily,
        status,
        completedAt,
        ingredientMode,
        ingredient,
        vegId,
        quantity,
        unit,
        questType,
        requestedDifficulty,
        focusDiscipline,
        requestedTechniqueId,
        requestedCuisine,
        timeLimit,
        simpleAndSmall,
        text,
        skill,
        difficulty,
        timeEstimateMinutes,
        why,
        dishType,
        tips,
        recipeId,
        moreIngredients,
    )

fun AttemptEntity.toBackup(hasPhoto: Boolean) =
    BackupAttempt(
        id,
        sourceType,
        recipeId,
        questId,
        cookedAt,
        score,
        tasteScore,
        notes,
        if (hasPhoto) photoEntry(id) else null,
        techniques,
        cuisines,
        vegIds,
        peakVegIds,
    )

fun BackupAttempt.toEntity(
    photoPath: String?,
    recipeId: String?,
    questId: String?,
) = AttemptEntity(
    id,
    sourceType,
    recipeId,
    questId,
    cookedAt,
    score,
    tasteScore,
    notes,
    photoPath,
    techniques,
    cuisines,
    vegIds,
    peakVegIds,
)

fun ShoppingItemEntity.toBackup() = BackupShoppingItem(id, text, quantity, unit, name, recipeId, checked, position, createdAt)

fun BackupShoppingItem.toEntity(recipeId: String?) =
    ShoppingItemEntity(id, text, quantity, unit, name, recipeId, checked, position, createdAt)

fun LessonProgressEntity.toBackup() = BackupLessonProgress(lessonId, completedAt)

fun BackupLessonProgress.toEntity() = LessonProgressEntity(lessonId, completedAt)

fun CheckAttemptEntity.toBackup() =
    BackupCheckAttempt(id, moduleId, takenAt, total, correct, safetyTotal, safetyCorrect, passed, wrongItemIds)

fun BackupCheckAttempt.toEntity() =
    CheckAttemptEntity(id, moduleId, takenAt, total, correct, safetyTotal, safetyCorrect, passed, wrongItemIds)

fun EvidenceEntity.toBackup(hasPhoto: Boolean) =
    BackupEvidence(id, exerciseId, loggedAt, band, notes, if (hasPhoto) evidenceEntry(id) else null)

fun BackupEvidence.toEntity(photoPath: String?) = EvidenceEntity(id, exerciseId, loggedAt, band, notes, photoPath)
