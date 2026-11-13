package nl.potentplum.mise.domain

import kotlinx.serialization.Serializable
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestIngredient
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.TimeLimit
import nl.potentplum.mise.data.db.entity.ingredients

/** What the user asked for (spec §6.11); everything optional is empty by default. Kept on the quest for "Andere quest". */
@Serializable
data class QuestParams(
    val ingredientMode: IngredientMode = IngredientMode.SURPRISE,
    /** Zelf kiezen: up to [MAX_INGREDIENTS] rows, each free text (max 200 characters) with an optional quantity and unit. */
    val ingredients: List<QuestIngredient> = listOf(QuestIngredient("")),
    /** Seizoensgroente: up to [MAX_INGREDIENTS] in-season vegetables. */
    val vegIds: List<String> = emptyList(),
    val questType: QuestType = QuestType.CLAUDE_CHOICE,
    /** Null = Automatisch (spec §11.6). */
    val difficulty: Difficulty? = null,
    val techniqueId: String? = null,
    val cuisine: String? = null,
    val timeLimit: TimeLimit? = null,
    val simpleAndSmall: Boolean = false,
    val isDaily: Boolean = false,
) {
    /**
     * What the quest must use, in the order given: the filled-in rows (Zelf kiezen) or the chosen vegetables with their
     * names (Seizoensgroente). Empty for Verras me, where Claude chooses.
     */
    fun chosenIngredients(content: Content): List<QuestIngredient> =
        when (ingredientMode) {
            IngredientMode.CUSTOM -> ingredients.map { it.copy(name = it.name.trim(), vegId = null) }.filter { it.name.isNotEmpty() }
            IngredientMode.SEASONAL -> vegIds.mapNotNull(content::vegetable).map { QuestIngredient(it.nl.lowercase(), it.id) }
            IngredientMode.SURPRISE -> emptyList()
        }

    companion object {
        const val MAX_INGREDIENT_LENGTH = 200
        const val MAX_INGREDIENTS = 5

        /** "Andere quest" and "opnieuw" use the parameters a quest was generated with. */
        fun of(quest: QuestEntity): QuestParams =
            QuestParams(
                ingredientMode = quest.ingredientMode,
                ingredients =
                    quest.ingredients
                        .takeIf { quest.ingredientMode == IngredientMode.CUSTOM && it.isNotEmpty() }
                        ?: listOf(QuestIngredient("")),
                vegIds = if (quest.ingredientMode == IngredientMode.SEASONAL) quest.ingredients.mapNotNull { it.vegId } else emptyList(),
                questType = quest.questType,
                difficulty = quest.requestedDifficulty,
                techniqueId = quest.requestedTechniqueId,
                cuisine = quest.requestedCuisine,
                timeLimit = quest.timeLimit,
                simpleAndSmall = quest.simpleAndSmall,
                isDaily = quest.isDaily,
            )

        /** The one-tap Quest van vandaag: today's seasonal vegetable, everything else from the defaults. */
        fun daily(choice: QuestDefaultChoice): QuestParams =
            if (choice.vegId != null) {
                QuestParams(ingredientMode = IngredientMode.SEASONAL, vegIds = listOf(choice.vegId), isDaily = true)
            } else {
                QuestParams(isDaily = true)
            }
    }
}
