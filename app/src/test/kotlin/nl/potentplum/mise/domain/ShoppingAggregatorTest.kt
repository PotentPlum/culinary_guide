package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §11.7: group by name, sum within a dimension via base units, never across dimensions or unnamed items. */
class ShoppingAggregatorTest {
    private val aggregator = ShoppingAggregator(TestContent.content.units)
    private var next = 0

    private fun item(
        text: String,
        quantity: Double? = null,
        unit: String? = null,
        name: String? = null,
        recipe: String? = "Soep",
        checked: Boolean = false,
    ) = ShoppingEntry(
        id = "i${next++}",
        text = text,
        quantity = quantity,
        unit = unit,
        name = name,
        recipeId = recipe?.let { "id-$it" },
        recipeTitle = recipe,
        checked = checked,
        position = next,
    )

    private fun texts(vararg entries: ShoppingEntry) = aggregator.aggregate(entries.toList()).combined.map { it.text }

    @Test
    fun `table of combinations`() {
        data class Case(
            val entries: List<ShoppingEntry>,
            val expected: List<String>,
        )
        val cases =
            listOf(
                // Same name and unit: summed.
                Case(listOf(item("2 uien", 2.0, name = "ui"), item("1 ui", 1.0, name = "ui", recipe = "Stoof")), listOf("3 ui")),
                // Grams and kilograms: summed in grams, switched to kg from 1000.
                Case(
                    listOf(item("750 g pompoen", 750.0, "g", "pompoen"), item("0,5 kg pompoen", 0.5, "kg", "pompoen")),
                    listOf("1,3 kg pompoen"),
                ),
                Case(listOf(item("100 g boter", 100.0, "g", "boter"), item("1 ons boter", 1.0, "ons", "boter")), listOf("200 g boter")),
                // Spoons stay spoons when all lines use the same one.
                Case(listOf(item("2 el olie", 2.0, "el", "olie"), item("1 el olie", 1.0, "el", "olie")), listOf("3 el olie")),
                // Mixed volume units: millilitres.
                Case(listOf(item("2 el olie", 2.0, "el", "olie"), item("100 ml olie", 100.0, "ml", "olie")), listOf("130 ml olie")),
                // Different dimensions under one name stay apart.
                Case(
                    listOf(item("2 teen knoflook", 2.0, "teen", "knoflook"), item("1 tl knoflook", 1.0, "tl", "knoflook")),
                    listOf("1 tl knoflook", "2 tenen knoflook"),
                ),
                // Raw scaled amounts are rounded once, after summing.
                Case(listOf(item("ui", 0.4, name = "ui"), item("ui", 0.4, name = "ui")), listOf("1 ui")),
                Case(listOf(item("bloem", 123.4, "g", "bloem")), listOf("125 g bloem")),
                // No amounts under one name: one line.
                Case(listOf(item("zout", name = "zout"), item("zout", name = "zout")), listOf("zout")),
                // Unparsed lines are never merged, even when identical.
                Case(listOf(item("een handje salie"), item("een handje salie")), listOf("een handje salie", "een handje salie")),
                // Names compare case-insensitively.
                Case(listOf(item("1 Prei", 1.0, name = "Prei"), item("1 prei", 1.0, name = "prei")), listOf("2 Prei")),
            )
        cases.forEach { case -> assertEquals(case.entries.toString(), case.expected, texts(*case.entries.toTypedArray())) }
    }

    @Test
    fun `combined lines list their source recipes once`() {
        val line =
            aggregator
                .aggregate(
                    listOf(
                        item("1 ui", 1.0, name = "ui", recipe = "A"),
                        item("1 ui", 1.0, name = "ui", recipe = "B"),
                        item("1 ui", 1.0, name = "ui", recipe = "A"),
                    ),
                ).combined
                .single()
        assertEquals(listOf("A", "B"), line.recipes)
        assertEquals(3, line.itemIds.size)
    }

    @Test
    fun `manual items go to Los in the order added, ticked lines last everywhere`() {
        val list =
            aggregator.aggregate(
                listOf(
                    item("Koffiefilters", recipe = null, checked = true),
                    item("Brood", recipe = null),
                    item("1 ui", 1.0, name = "ui", checked = true),
                    item("2 wortels", 2.0, name = "wortel"),
                    item("1 appel", 1.0, name = "appel"),
                ),
            )
        assertEquals(listOf("1 appel", "2 wortel", "1 ui"), list.combined.map { it.text })
        assertEquals(listOf("Brood", "Koffiefilters"), list.manual.map { it.text })
    }

    @Test
    fun `a combined line is ticked only when all its items are`() {
        val partly =
            aggregator
                .aggregate(
                    listOf(item("1 ui", 1.0, name = "ui", checked = true), item("1 ui", 1.0, name = "ui")),
                ).combined
                .single()
        assertFalse(partly.checked)
        val all =
            aggregator
                .aggregate(
                    listOf(item("1 ui", 1.0, name = "ui", checked = true), item("1 ui", 1.0, name = "ui", checked = true)),
                ).combined
                .single()
        assertTrue(all.checked)
    }
}
