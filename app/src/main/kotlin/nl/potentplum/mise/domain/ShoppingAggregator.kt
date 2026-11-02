package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Dimension
import nl.potentplum.mise.content.model.MeasureUnit

/** One item on the list, as stored (spec §7.2): quantities are raw scaled amounts, not yet rounded. */
data class ShoppingEntry(
    val id: String,
    val text: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    val recipeId: String? = null,
    val recipeTitle: String? = null,
    val checked: Boolean = false,
    val position: Int = 0,
)

/** A line on screen: one item, or several combined. Ticking or deleting it acts on all of [itemIds]. */
data class ShoppingListLine(
    val key: String,
    val text: String,
    val itemIds: List<String>,
    /** The source recipes, as chips (spec §6.14). */
    val recipes: List<String>,
    val checked: Boolean,
)

data class ShoppingList(
    /** Items from recipes, combined where possible, A–Z; ticked lines last. */
    val combined: List<ShoppingListLine>,
    /** "Los": manual items in the order added; ticked lines last. */
    val manual: List<ShoppingListLine>,
) {
    val isEmpty: Boolean get() = combined.isEmpty() && manual.isEmpty()
    val lines: List<ShoppingListLine> get() = combined + manual
}

/**
 * Shopping list aggregation (spec §11.7). Items with a name are grouped by name; within a group, amounts in the same
 * dimension are summed through the base unit (g, ml, stuks) and shown in the best display unit. Different dimensions
 * and different OTHER units ("2 teen knoflook" + "1 tl knoflookpoeder") stay separate lines; items without a name are
 * never merged.
 */
class ShoppingAggregator(
    units: List<MeasureUnit>,
) {
    private val unitsById = units.associateBy { it.id }
    private val scaler = IngredientScaler(units)
    private val gram = unitsById.getValue("g")
    private val milliliter = unitsById.getValue("ml")

    fun aggregate(entries: List<ShoppingEntry>): ShoppingList {
        val (fromRecipes, manual) = entries.partition { it.recipeId != null || it.name != null }
        val combined =
            fromRecipes
                .groupBy { entry ->
                    entry.name
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { "$it|${bucket(entry)}" }
                        ?: "item|${entry.id}"
                }.map { (key, group) -> line(key, group) }
                .sortedWith(compareBy<ShoppingListLine> { it.checked }.thenBy(COLLATOR) { it.text })
        val loose =
            manual
                .sortedBy { it.position }
                .map { line("item|${it.id}", listOf(it)) }
                .sortedBy { it.checked }
        return ShoppingList(combined, loose)
    }

    /** Which items may be summed together under one name. */
    private fun bucket(entry: ShoppingEntry): String {
        if (entry.quantity == null) return "none"
        val unit = entry.unit?.let(unitsById::get) ?: return Dimension.COUNT.name
        return when (unit.dimension) {
            Dimension.OTHER -> "other:${unit.id}"

            // "2 el olie" + "1 el olie" stays in el; mixed spoons and ml are summed in ml.
            else -> unit.dimension.name
        }
    }

    private fun line(
        key: String,
        group: List<ShoppingEntry>,
    ): ShoppingListLine {
        val first = group.first()
        val text =
            if (group.size == 1 && (first.name == null || first.quantity == null)) {
                first.text
            } else if (first.quantity == null) {
                // The same name without amounts ("zout" twice): one line.
                first.text
            } else {
                format(group)
            }
        return ShoppingListLine(
            key = key,
            text = text,
            itemIds = group.map { it.id },
            recipes = group.mapNotNull { it.recipeTitle }.distinct(),
            checked = group.all { it.checked },
        )
    }

    private fun format(group: List<ShoppingEntry>): String {
        val first = group.first()
        val units = group.map { it.unit }.distinct()
        val unit = first.unit?.let(unitsById::get)
        val display =
            if (units.size == 1) {
                scaler.round(group.sumOf { it.quantity ?: 0.0 }, unit)
            } else {
                // Mixed units of one dimension: sum in the base unit.
                val base =
                    when (unit?.dimension ?: Dimension.COUNT) {
                        Dimension.MASS -> gram

                        Dimension.VOLUME -> milliliter

                        // "2 stuks" + "1": plain count.
                        else -> null
                    }
                val total = group.sumOf { entry -> (entry.quantity ?: 0.0) * (entry.unit?.let(unitsById::get)?.factor ?: 1.0) }
                scaler.round(total, base)
            }
        val parsed = ParsedIngredient(first.text, display.quantity, null, display.unit?.id, first.name?.trim(), parsed = true)
        return scaler.format(parsed)
    }

    private companion object {
        val COLLATOR: java.text.Collator =
            java.text.Collator
                .getInstance(java.util.Locale.forLanguageTag("nl-NL"))
                .apply { strength = java.text.Collator.PRIMARY }
    }
}
