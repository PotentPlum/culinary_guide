package nl.potentplum.mise.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.dao.QuestDao
import nl.potentplum.mise.data.db.dao.RecipeDao
import nl.potentplum.mise.data.db.dao.ShoppingDao
import nl.potentplum.mise.data.db.dao.UsageDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): MiseDatabase =
        Room
            .databaseBuilder(context, MiseDatabase::class.java, MiseDatabase.NAME)
            .addMigrations(*MiseDatabase.MIGRATIONS)
            .build()

    @Provides
    fun recipeDao(db: MiseDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun questDao(db: MiseDatabase): QuestDao = db.questDao()

    @Provides
    fun attemptDao(db: MiseDatabase): AttemptDao = db.attemptDao()

    @Provides
    fun shoppingDao(db: MiseDatabase): ShoppingDao = db.shoppingDao()

    @Provides
    fun usageDao(db: MiseDatabase): UsageDao = db.usageDao()
}
