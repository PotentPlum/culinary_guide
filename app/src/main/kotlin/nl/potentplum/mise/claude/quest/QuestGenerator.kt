package nl.potentplum.mise.claude.quest

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeModels
import nl.potentplum.mise.claude.ClaudeRequest
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.ContentPart
import nl.potentplum.mise.claude.Effort
import nl.potentplum.mise.claude.OutputSchemas
import nl.potentplum.mise.claude.ResponseMapping
import nl.potentplum.mise.claude.ResponseMapping.Companion.int
import nl.potentplum.mise.claude.ResponseMapping.Companion.string
import nl.potentplum.mise.claude.ResponseMapping.Companion.strings
import nl.potentplum.mise.claude.prompts.Prompts
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.db.entity.UsageFeature
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.newId
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.domain.QuestDefaults
import nl.potentplum.mise.domain.QuestHistoryEntry
import nl.potentplum.mise.domain.QuestParams
import javax.inject.Inject
import javax.inject.Singleton

/** A quest request with Automatisch and the defaults resolved (spec §11.6); shown as "what was chosen" while generating. */
data class QuestPlan(
    val params: QuestParams,
    val difficulty: Difficulty,
    /** Null when a technique was asked for: the technique is the focus then. */
    val focus: DisciplineId?,
    val inSeason: List<String>,
    val history: List<QuestHistoryEntry>,
)

/** Quest generation (spec §6.11, §8.2): plan → one Claude call → quest + hidden recipe saved together. */
@Singleton
class QuestGenerator
    @Inject
    constructor(
        private val claude: ClaudeService,
        private val content: ContentRepository,
        private val settings: SettingsRepository,
        private val quests: QuestRepository,
        private val recipes: RecipeRepository,
        private val clock: MiseClock,
    ) {
        /** Today's one-tap parameters; [offset] moves to the next candidate vegetable ("Andere quest"). */
        suspend fun dailyParams(offset: Int = 0): QuestParams {
            val today = clock.today()
            val choice =
                QuestDefaults.choose(today, content.content(), quests.attempts(), quests.pastQuests(), today.toEpochDay().toInt() + offset)
            return QuestParams.daily(choice)
        }

        suspend fun plan(params: QuestParams): QuestPlan {
            val content = content.content()
            val choice = QuestDefaults.choose(clock.today(), content, quests.attempts(), quests.pastQuests())
            val technique = params.techniqueId?.let(content::technique)
            return QuestPlan(
                params = params,
                difficulty = params.difficulty ?: choice.difficulty,
                focus = if (technique != null) null else choice.focusDiscipline,
                inSeason = choice.vegCandidates,
                history = choice.history,
            )
        }

        /** Saves nothing unless the whole reply is usable; cancelling the caller's coroutine records nothing. */
        suspend fun generate(plan: QuestPlan): QuestEntity {
            val content = content.content()
            val params = plan.params
            val request =
                ClaudeRequest(
                    model = ClaudeModels.forQuality(settings.settings.first().quality),
                    effort = Effort.MEDIUM,
                    system = Prompts.questSystem(content),
                    content =
                        listOf(
                            ContentPart.Text(
                                Prompts.questRequest(content, params, plan.difficulty, plan.focus, plan.inSeason, plan.history),
                            ),
                        ),
                    schema = OutputSchemas(content).quest(plan.inSeason),
                )
            val reply = claude.send(UsageFeature.QUEST, request).json
            val recipe = ResponseMapping(content, recipes.parser()).questRecipe(reply)
            val questText = reply.string("questText")
            val skill = reply.string("skill")
            if (questText == null || skill == null || recipe.title.isBlank() || recipe.ingredients.isEmpty() || recipe.steps.isEmpty()) {
                throw ClaudeError.InvalidResponse
            }
            val chosenVeg = reply.string("chosenVegId")?.takeIf { it in plan.inSeason }?.let(content::vegetable)
            val vegId =
                when (params.ingredientMode) {
                    IngredientMode.CUSTOM -> null
                    IngredientMode.SEASONAL -> params.vegId
                    IngredientMode.SURPRISE -> chosenVeg?.id
                }
            val ingredient =
                when (params.ingredientMode) {
                    IngredientMode.CUSTOM -> {
                        params.ingredient.trim()
                    }

                    IngredientMode.SEASONAL -> {
                        params.vegId
                            ?.let(content::vegetable)
                            ?.nl
                            ?.lowercase() ?: params.ingredient
                    }

                    IngredientMode.SURPRISE -> {
                        chosenVeg?.nl?.lowercase() ?: reply.string("chosenIngredient").orEmpty()
                    }
                }
            val now = clock.nowMillis()
            val saved = recipe.copy(createdAt = now, updatedAt = now)
            val quest =
                QuestEntity(
                    id = newId(),
                    createdAt = now,
                    isDaily = params.isDaily,
                    ingredientMode = params.ingredientMode,
                    ingredient = ingredient,
                    vegId = vegId,
                    quantity = params.quantity.takeIf { params.ingredientMode == IngredientMode.CUSTOM },
                    unit = params.unit.takeIf { params.ingredientMode == IngredientMode.CUSTOM },
                    questType = params.questType,
                    requestedDifficulty = params.difficulty,
                    focusDiscipline = plan.focus,
                    requestedTechniqueId = params.techniqueId,
                    requestedCuisine = params.cuisine,
                    timeLimit = params.timeLimit,
                    simpleAndSmall = params.simpleAndSmall,
                    text = questText,
                    skill = skill,
                    difficulty = reply.string("difficulty")?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: plan.difficulty,
                    timeEstimateMinutes = reply.int("timeEstimateMinutes")?.takeIf { it > 0 } ?: 0,
                    why = reply.string("why").orEmpty(),
                    dishType = saved.tagValues(TagKey.DISH_TYPE).firstOrNull().orEmpty(),
                    tips = (reply["recipe"] as? JsonObject)?.strings("tips").orEmpty(),
                    recipeId = saved.id,
                )
            quests.insert(quest, saved)
            return quest
        }
    }
