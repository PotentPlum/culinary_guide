package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.TechniqueCategory
import nl.potentplum.mise.content.model.TechniqueLevel
import nl.potentplum.mise.feature.vegetable.VegetableLinks
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    private val content = TestContent.content

    private fun search(query: String) = content.vegetables.filter { VegetableSearch.matches(it, query) }.map { it.id }

    @Test
    fun `search ignores case and diacritics`() {
        assertEquals(listOf("corn"), search("MAIS"))
        assertEquals(search("suikermais"), search("Suikermaïs"))
    }

    @Test
    fun `search matches the english alias and the family`() {
        assertTrue("asparagus-white" in search("white asparagus"))
        assertTrue(search("paddenstoel").isNotEmpty())
        assertEquals(content.vegetables.size, search("  ").size)
    }

    @Test
    fun `calendar groups by family with dutch ordering inside`() {
        val sections = CalendarLayout.sections(content.vegetables, CalendarGrouping.FAMILY)
        assertEquals(
            content.vegetables
                .map { it.family.nl }
                .toSet()
                .size,
            sections.size,
        )
        assertEquals(content.vegetables.size, sections.sumOf { it.vegetables.size })
        val collator = SeasonPresentation.dutchCollator()
        sections.zipWithNext().forEach { (a, b) -> assertTrue(collator.compare(a.family, b.family) < 0) }
        sections.forEach { section ->
            section.vegetables.zipWithNext().forEach { (a, b) -> assertTrue(collator.compare(a.nl, b.nl) <= 0) }
        }
    }

    @Test
    fun `calendar a to z is one section`() {
        val sections = CalendarLayout.sections(content.vegetables, CalendarGrouping.ALPHABETICAL)
        assertEquals(1, sections.size)
        assertNull(sections.single().family)
    }

    @Test
    fun `techniques follow the category order and keep every technique`() {
        val groups = TechniqueCatalog.grouped(content.techniques)
        assertEquals(TechniqueCategory.entries.filter { c -> content.techniques.any { it.category == c } }, groups.map { it.category })
        assertEquals(74, groups.sumOf { g -> g.subgroups.sumOf { it.techniques.size } })
    }

    @Test
    fun `technique filters combine level and search`() {
        val basis = TechniqueCatalog.grouped(content.techniques, level = TechniqueLevel.BASIS)
        assertTrue(basis.flatMap { g -> g.subgroups.flatMap { it.techniques } }.all { it.level == TechniqueLevel.BASIS })
        val found = TechniqueCatalog.grouped(content.techniques, query = "JULIËNNE").flatMap { g -> g.subgroups.flatMap { it.techniques } }
        assertTrue(found.any { it.id == "julienne" })
    }

    @Test
    fun `children are the techniques that build on a technique`() {
        val parent = content.techniques.first { it.parentId != null }.parentId!!
        assertTrue(TechniqueCatalog.children(content.techniques, parent).all { it.parentId == parent })
    }

    @Test
    fun `substitution text links to a vegetable by any of its names`() {
        assertEquals("asparagus-green", VegetableLinks.find(content, "Groene asperges"))
        assertEquals("carrot", VegetableLinks.find(content, "peen"))
        assertNull(VegetableLinks.find(content, "Hollandaisesaus"))
        assertNull(VegetableLinks.find(content, "Witte asperges", exclude = "asparagus-white"))
    }
}
