package nl.potentplum.mise.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQueryTest {
    private fun item(
        id: String,
        title: String,
        createdAt: Long = 0,
        cooked: Int = 0,
        lastCooked: Long? = null,
        favorite: Boolean = false,
        minutes: Int? = null,
        vegIds: Set<String> = emptySet(),
        ingredients: List<String> = emptyList(),
        dishTypes: Set<String> = emptySet(),
        cuisines: Set<String> = emptySet(),
        source: String = "",
    ) = LibraryItem(
        id,
        title,
        source,
        null,
        minutes,
        favorite,
        createdAt,
        cooked,
        lastCooked,
        ingredients,
        vegIds,
        cuisines,
        dishTypes,
        emptySet(),
        emptySet(),
    )

    private val items =
        listOf(
            item(
                "soup",
                "Pompoensoep",
                createdAt = 3,
                cooked = 2,
                lastCooked = 50,
                vegIds = setOf("pumpkin"),
                minutes = 40,
                dishTypes = setOf("Soep"),
            ),
            item(
                "stew",
                "Stamppot boerenkool",
                createdAt = 1,
                cooked = 5,
                lastCooked = 90,
                favorite = true,
                vegIds = setOf("kale", "potato"),
            ),
            item("salad", "Asperge-salade", createdAt = 2, vegIds = setOf("asparagus-white"), minutes = 20, cuisines = setOf("Frans")),
            item("cake", "Éclair", createdAt = 4, ingredients = listOf("bloem", "room"), source = "leukerecepten.nl"),
        )

    private fun ids(
        filter: LibraryFilter = LibraryFilter(),
        sort: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
        available: Set<String> = setOf("pumpkin", "kale"),
    ) = LibraryQuery.apply(items, filter, sort, available).map { it.id }

    @Test
    fun `three sort orders`() {
        assertEquals(listOf("cake", "soup", "salad", "stew"), ids())
        assertEquals(listOf("salad", "cake", "soup", "stew"), ids(sort = LibrarySortOrder.ALPHABETICAL))
        assertEquals(listOf("stew", "soup", "salad", "cake"), ids(sort = LibrarySortOrder.RECENTLY_COOKED))
    }

    @Test
    fun `search covers title, ingredients and source, ignoring accents`() {
        assertEquals(listOf("cake"), ids(LibraryFilter(query = "eclair")))
        assertEquals(listOf("cake"), ids(LibraryFilter(query = "ROOM")))
        assertEquals(listOf("cake"), ids(LibraryFilter(query = "leukerecepten")))
        assertEquals(listOf("stew"), ids(LibraryFilter(query = "boerenkool")))
    }

    @Test
    fun `quick filters`() {
        assertEquals(listOf("soup", "stew"), ids(LibraryFilter(seasonNow = true)))
        assertEquals(listOf("stew"), ids(LibraryFilter(favorites = true)))
        assertEquals(listOf("cake", "salad"), ids(LibraryFilter(neverCooked = true)))
        assertEquals(listOf("soup"), ids(LibraryFilter(dishType = "Soep")))
    }

    @Test
    fun `meer filters combine with quick filters`() {
        assertEquals(listOf("salad"), ids(LibraryFilter(cuisine = "Frans")))
        assertEquals(listOf("salad"), ids(LibraryFilter(prepTime = PrepTimeBucket.UNDER_30)))
        assertEquals(emptyList<String>(), ids(LibraryFilter(prepTime = PrepTimeBucket.UNDER_30, seasonNow = true)))
        assertEquals(2, LibraryFilter(cuisine = "Frans", prepTime = PrepTimeBucket.OVER_60).moreCount)
    }

    @Test
    fun `active filters are detected`() {
        assertFalse(LibraryFilter().isActive)
        assertTrue(LibraryFilter(query = "x").isActive)
    }
}
