package nl.potentplum.mise.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.dao.BackupDao
import nl.potentplum.mise.data.db.dao.CurriculumDao
import nl.potentplum.mise.data.db.dao.QuestDao
import nl.potentplum.mise.data.db.dao.RecipeDao
import nl.potentplum.mise.data.db.dao.ShoppingDao
import nl.potentplum.mise.data.db.dao.UsageDao
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.CheckAttemptEntity
import nl.potentplum.mise.data.db.entity.ClaudeUsageEntity
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import nl.potentplum.mise.data.db.entity.IngredientEntity
import nl.potentplum.mise.data.db.entity.IngredientGroupEntity
import nl.potentplum.mise.data.db.entity.LessonProgressEntity
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.RecipeEntity
import nl.potentplum.mise.data.db.entity.RecipeExtraFieldEntity
import nl.potentplum.mise.data.db.entity.RecipeTagEntity
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity
import nl.potentplum.mise.data.db.entity.StepEntity
import nl.potentplum.mise.data.db.entity.StepGroupEntity

/**
 * The phone is the only home of the data. Schema changes get a numbered [Migration] in [MiseDatabase.MIGRATIONS] and a
 * `MigrationTestHelper` test; destructive migration is never used (spec §7.2). Schemas are exported to `app/schemas/`.
 */
@Database(
    version = 2,
    exportSchema = true,
    entities = [
        RecipeEntity::class,
        IngredientGroupEntity::class,
        IngredientEntity::class,
        StepGroupEntity::class,
        StepEntity::class,
        RecipeExtraFieldEntity::class,
        RecipeTagEntity::class,
        QuestEntity::class,
        AttemptEntity::class,
        ShoppingItemEntity::class,
        ClaudeUsageEntity::class,
        LessonProgressEntity::class,
        CheckAttemptEntity::class,
        EvidenceEntity::class,
    ],
)
@TypeConverters(Converters::class)
abstract class MiseDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun questDao(): QuestDao

    abstract fun attemptDao(): AttemptDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun usageDao(): UsageDao

    abstract fun backupDao(): BackupDao

    abstract fun curriculumDao(): CurriculumDao

    companion object {
        const val NAME = "mise.db"

        /** v2 adds the curriculum progress tables (spec §6.17). */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `curriculum_lesson` (`lessonId` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`lessonId`))",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `curriculum_check` (`id` TEXT NOT NULL, `moduleId` TEXT NOT NULL, " +
                            "`takenAt` INTEGER NOT NULL, `total` INTEGER NOT NULL, `correct` INTEGER NOT NULL, " +
                            "`safetyTotal` INTEGER NOT NULL, `safetyCorrect` INTEGER NOT NULL, `passed` INTEGER NOT NULL, " +
                            "`wrongItemIds` TEXT NOT NULL, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_curriculum_check_moduleId_takenAt` ON `curriculum_check` (`moduleId`, `takenAt`)",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `curriculum_evidence` (`id` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, " +
                            "`loggedAt` INTEGER NOT NULL, `band` TEXT NOT NULL, `notes` TEXT, `photoPath` TEXT, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_curriculum_evidence_exerciseId_loggedAt` " +
                            "ON `curriculum_evidence` (`exerciseId`, `loggedAt`)",
                    )
                }
            }

        /** Every schema change after v1 adds its migration here. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
