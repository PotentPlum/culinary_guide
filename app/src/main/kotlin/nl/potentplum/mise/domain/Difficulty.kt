package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.CuratedDifficulty
import nl.potentplum.mise.content.model.TechniqueLevel

/**
 * One difficulty scale for quests, techniques and curated recipes, labelled Basis / Gemiddeld / Gevorderd (spec §5).
 * The names match the stored quest values.
 */
enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    ;

    companion object {
        fun of(level: TechniqueLevel): Difficulty =
            when (level) {
                TechniqueLevel.BASIS -> BEGINNER
                TechniqueLevel.GEMIDDELD -> INTERMEDIATE
                TechniqueLevel.GEVORDERD -> ADVANCED
            }

        fun of(difficulty: CuratedDifficulty): Difficulty =
            when (difficulty) {
                CuratedDifficulty.EASY -> BEGINNER
                CuratedDifficulty.MEDIUM -> INTERMEDIATE
                CuratedDifficulty.HARD -> ADVANCED
            }
    }
}
