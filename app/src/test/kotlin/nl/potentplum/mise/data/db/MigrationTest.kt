package nl.potentplum.mise.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every migration gets a test here (spec §7.2). v1 has none yet; this checks that the exported v1 schema creates a
 * database that Room opens and validates, so the helper is ready for v2.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MiseDatabase::class.java)

    @Test
    fun `the exported v1 schema matches the database`() =
        runTest {
            helper.createDatabase(DB, 1).use { db ->
                db.execSQL(
                    "INSERT INTO recipe (id, title, description, sourceType, sourceLabel, isFavorite, inLibrary, enrichState, " +
                        "createdAt, updatedAt) VALUES ('r1', 'Stamppot', '', 'MANUAL', '', 0, 1, 'NONE', 1, 1)",
                )
            }
            helper.runMigrationsAndValidate(DB, 1, true, *MiseDatabase.MIGRATIONS).close()

            val room =
                Room
                    .databaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MiseDatabase::class.java, DB)
                    .addMigrations(*MiseDatabase.MIGRATIONS)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertEquals("Stamppot", room.recipeDao().getRecipe("r1")?.title)
            } finally {
                room.close()
            }
        }

    private companion object {
        const val DB = "migration-test.db"
    }
}
