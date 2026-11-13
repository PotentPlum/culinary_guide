package nl.potentplum.mise.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.data.db.entity.Band
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import nl.potentplum.mise.data.db.entity.LessonProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every migration gets a test here (spec §7.2): the data survives and the result matches the exported schema. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MiseDatabase::class.java)

    @Test
    fun `the exported v1 schema matches the database`() =
        runTest {
            helper.createDatabase(DB, 1).use { db -> insertRecipe(db) }
            helper.runMigrationsAndValidate(DB, 1, true).close()
        }

    @Test
    fun `1 to 2 adds the curriculum tables and keeps the recipes`() =
        runTest {
            helper.createDatabase(DB, 1).use { db -> insertRecipe(db) }
            helper.runMigrationsAndValidate(DB, 2, true, MiseDatabase.MIGRATION_1_2).close()

            val room =
                Room
                    .databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MiseDatabase::class.java, DB)
                    .addMigrations(*MiseDatabase.MIGRATIONS)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertEquals("Stamppot", room.recipeDao().getRecipe("r1")?.title)
                val dao = room.curriculumDao()
                dao.upsertLessons(listOf(LessonProgressEntity("EGG-1.M1.L1", 5)))
                dao.upsertEvidence(listOf(EvidenceEntity("e1", "EGG-1.M1.X1", 6, Band.COMPETENT)))
                assertEquals(listOf("EGG-1.M1.L1"), dao.lessons().map { it.lessonId })
                assertEquals(Band.COMPETENT, dao.evidence().single().band)
            } finally {
                room.close()
            }
        }

    private fun insertRecipe(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO recipe (id, title, description, sourceType, sourceLabel, isFavorite, inLibrary, enrichState, " +
                "createdAt, updatedAt) VALUES ('r1', 'Stamppot', '', 'MANUAL', '', 0, 1, 'NONE', 1, 1)",
        )
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
