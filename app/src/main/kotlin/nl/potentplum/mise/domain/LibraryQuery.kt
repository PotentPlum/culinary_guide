package nl.potentplum.mise.domain

import kotlinx.serialization.Serializable

/** A library recipe with what the list, search and filters need (spec §6.7). */
data class LibraryItem(
    val id: String,
    val title: String,
    val sourceLabel: String,
    val imagePath: String?,
    val totalMinutes: Int?,
    val isFavorite: Boolean,
    val createdAt: Long,
    val cookedCount: Int,
    val lastCookedAt: Long?,
    /** Structured names, else the text, of every ingredient line. */
    val ingredientTexts: List<String>,
    val vegIds: Set<String>,
    val cuisines: Set<String>,
    val dishTypes: Set<String>,
    val mainIngredients: Set<String>,
    val techniques: Set<String>,
)

enum class LibrarySortOrder { RECENTLY_ADDED, ALPHABETICAL, RECENTLY_COOKED }

/** Search, quick filters ("Seizoen nu", Favorieten, Nooit gekookt, dish type) and the "Meer filters" sheet. */
@Serializable
data class LibraryFilter(
    val query: String = "",
    val seasonNow: Boolean = false,
    val favorites: Boolean = false,
    val neverCooked: Boolean = false,
    val dishType: String? = null,
    val cuisine: String? = null,
    val mainIngredient: String? = null,
    val technique: String? = null,
    val prepTime: PrepTimeBucket? = null,
) {
    val isActive: Boolean get() = this != LibraryFilter()

    /** How many "Meer filters" are set, for the chip's badge. */
    val moreCount: Int get() = listOfNotNull(cuisine, mainIngredient, technique, prepTime).size
}

object LibraryQuery {
    /**
     * [availableVegIds] are the vegetables at PEAK or SHOULDER this month: "Seizoen nu" keeps recipes using one of them
     * (spec §11.4).
     */
    fun apply(
        items: List<LibraryItem>,
        filter: LibraryFilter,
        sort: LibrarySortOrder,
        availableVegIds: Set<String>,
    ): List<LibraryItem> {
        val query = TextMatch.fold(filter.query)
        val filtered =
            items.filter { item ->
                (query.isEmpty() || matches(item, query)) &&
                    (!filter.seasonNow || item.vegIds.any { it in availableVegIds }) &&
                    (!filter.favorites || item.isFavorite) &&
                    (!filter.neverCooked || item.cookedCount == 0) &&
                    (filter.dishType == null || filter.dishType in item.dishTypes) &&
                    (filter.cuisine == null || filter.cuisine in item.cuisines) &&
                    (filter.mainIngredient == null || filter.mainIngredient in item.mainIngredients) &&
                    (filter.technique == null || filter.technique in item.techniques) &&
                    (filter.prepTime == null || PrepTimeBucket.of(item.totalMinutes) == filter.prepTime)
            }
        val collator = SeasonPresentation.dutchCollator()
        return when (sort) {
            LibrarySortOrder.RECENTLY_ADDED -> {
                filtered.sortedByDescending { it.createdAt }
            }

            LibrarySortOrder.ALPHABETICAL -> {
                filtered.sortedWith(compareBy(collator) { it.title })
            }

            LibrarySortOrder.RECENTLY_COOKED -> {
                filtered.sortedWith(compareByDescending<LibraryItem> { it.lastCookedAt ?: Long.MIN_VALUE }.thenBy(collator) { it.title })
            }
        }
    }

    /** Search across title, ingredient names and source. */
    private fun matches(
        item: LibraryItem,
        folded: String,
    ): Boolean =
        TextMatch.fold(item.title).contains(folded) ||
            TextMatch.fold(item.sourceLabel).contains(folded) ||
            item.ingredientTexts.any { TextMatch.fold(it).contains(folded) }
}
