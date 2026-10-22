package nl.potentplum.mise.content

import nl.potentplum.mise.content.model.BadgeRule
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.content.model.TechniqueCategory
import nl.potentplum.mise.testing.TestContent
import nl.potentplum.mise.testing.TestResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParserTest {
    private val content = TestContent.content

    @Test
    fun `every asset deserialises strictly with the validator's counts`() {
        assertEquals(66, content.vegetables.size)
        assertEquals(74, content.techniques.size)
        assertEquals(4, content.facets.size)
        assertEquals(3, content.disciplines.size)
        assertEquals(7, content.archetypes.size)
    }

    @Test
    fun `disciplines keep their display order`() {
        assertEquals(listOf(DisciplineId.MECHANICAL, DisciplineId.THERMAL, DisciplineId.FLAVOR), content.disciplines.map { it.id })
    }

    @Test
    fun `lookups by id and key work`() {
        assertEquals("Witte asperges", content.vegetable("asparagus-white")?.nl)
        assertNotNull(content.technique("julienne"))
        assertTrue(content.facet(FacetKey.CUISINE).options.isNotEmpty())
        assertEquals(content.vegetables.size, content.vegetablesById.size)
        assertEquals(content.techniques.size, content.techniquesById.size)
    }

    @Test
    fun `cookware guides are the only techniques without a discipline`() {
        val withoutDiscipline = content.techniques.filter { it.discipline == null }
        assertEquals(3, withoutDiscipline.size)
        assertTrue(withoutDiscipline.all { it.category == TechniqueCategory.GEREEDSCHAP })
        assertEquals(71, content.skills.size)
    }

    @Test
    fun `cross references resolve`() {
        for (technique in content.techniques) {
            technique.relatedVegIds.forEach { assertNotNull("${technique.id} → veg $it", content.vegetable(it)) }
            technique.relatedTechniqueIds.forEach { assertNotNull("${technique.id} → $it", content.technique(it)) }
            technique.parentId?.let { assertNotNull("${technique.id} → parent $it", content.technique(it)) }
        }
        for (archetype in content.archetypes) {
            val rule = archetype.rule
            if (rule is BadgeRule.Techniques) rule.techniqueIds?.forEach { assertNotNull("${archetype.id} → $it", content.technique(it)) }
        }
    }

    @Test
    fun `badge rules are typed`() {
        val rules = content.archetypes.associate { it.id to it.rule }
        assertEquals(BadgeRule.Techniques(discipline = DisciplineId.FLAVOR, minAttempts = 6, minAvgScore = 7.0), rules["emulsie-ingenieur"])
        assertEquals(BadgeRule.DistinctPeakVegetables(min = 15), rules["seizoenskok"])
        assertEquals(BadgeRule.PeakMonths(min = 12), rules["jaarrond"])
    }

    @Test(expected = UnsupportedContentException::class)
    fun `an unsupported schema version fails in strict mode`() {
        ContentParser(strict = true).parse(withVersion(2))
    }

    @Test
    fun `an unsupported schema version only warns in lenient mode`() {
        val warnings = mutableListOf<String>()
        val parsed = ContentParser(strict = false, onWarning = { warnings += it }).parse(withVersion(2))
        assertEquals(66, parsed.vegetables.size)
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().startsWith("vegetables.json"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown keys fail in strict mode`() {
        ContentParser(strict = true).parse { name ->
            val text = TestResources.read(name)
            if (name == ContentParser.DISCIPLINES) text.replaceFirst("\"nl\":", "\"surprise\": 1, \"nl\":") else text
        }
    }

    @Test
    fun `unknown keys are ignored in lenient mode`() {
        val parsed =
            ContentParser(strict = false).parse { name ->
                val text = TestResources.read(name)
                if (name == ContentParser.DISCIPLINES) text.replaceFirst("\"nl\":", "\"surprise\": 1, \"nl\":") else text
            }
        assertEquals(3, parsed.disciplines.size)
    }

    private fun withVersion(version: Int): (String) -> String =
        { name ->
            val text = TestResources.read(name)
            when (name) {
                ContentParser.VEGETABLES -> text.replaceFirst(VERSION_1, "\"schemaVersion\": $version")
                else -> text
            }
        }

    private companion object {
        val VERSION_1 = Regex("\"schemaVersion\":\\s*1")
    }
}
