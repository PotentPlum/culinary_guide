package nl.potentplum.mise.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.dao.QuestDao
import nl.potentplum.mise.data.db.dao.RecipeDao
import nl.potentplum.mise.data.db.dao.ShoppingDao
import nl.potentplum.mise.data.db.dao.UsageDao
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.ClaudeUsageEntity
import nl.potentplum.mise.data.db.entity.IngredientEntity
import nl.potentplum.mise.data.db.entity.IngredientGroupEntity
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
    version = 1,
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
    ],
)
@TypeConverters(Converters::class)
abstract class MiseDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    abstract fun questDao(): QuestDao

    abstract fun attemptDao(): AttemptDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun usageDao(): UsageDao

    companion object {
        const val NAME = "mise.db"

        /** Every schema change after v1 adds its migration here. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
