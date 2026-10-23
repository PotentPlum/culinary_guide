package nl.potentplum.mise.domain

import kotlinx.serialization.Serializable
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.TimeLimit

/** What the user asked for (spec §6.11); everything optional is empty by default. Kept on the quest for "Andere quest". */
@Serializable
data class QuestParams(
    val ingredientMode: IngredientMode = IngredientMode.SURPRISE,
    /** Zelf kiezen: free text, max 200 characters. */
    val ingredient: String = "",
    /** Seizoensgroente. */
    val vegId: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val questType: QuestType = QuestType.CLAUDE_CHOICE,
    /** Null = Automatisch (spec §11.6). */
    val difficulty: Difficulty? = null,
    val techniqueId: String? = null,
    val cuisine: String? = null,
    val timeLimit: TimeLimit? = null,
    val simpleAndSmall: Boolean = false,
    val isDaily: Boolean = false,
) {
    companion object {
        const val MAX_INGREDIENT_LENGTH = 200

        /** "Andere quest" and "opnieuw" use the parameters a quest was generated with. */
        fun of(quest: QuestEntity): QuestParams =
            QuestParams(
                ingredientMode = quest.ingredientMode,
                ingredient = if (quest.ingredientMode == IngredientMode.CUSTOM) quest.ingredient else "",
                vegId = quest.vegId.takeIf { quest.ingredientMode == IngredientMode.SEASONAL },
                quantity = quest.quantity,
                unit = quest.unit,
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
                QuestParams(ingredientMode = IngredientMode.SEASONAL, vegId = choice.vegId, isDaily = true)
            } else {
                QuestParams(isDaily = true)
            }
    }
}
