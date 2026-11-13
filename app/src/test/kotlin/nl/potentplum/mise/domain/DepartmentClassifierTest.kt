package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec §11.7: shopping lines by shop department, from `departments.json` and the vegetables' own names. */
class DepartmentClassifierTest {
    private val classifier = DepartmentClassifier(TestContent.content.departments, TestContent.content.vegetables)

    @Test
    fun `the departments come in the order of the shop`() {
        assertEquals(listOf("groente", "zuivel-eieren", "vlees-vis", "brood", "voorraad", "overig"), classifier.departmentIds)
    }

    @Test
    fun `table of lines and departments`() {
        val expected =
            mapOf(
                // Vegetables by their own names, singular or plural, in any part of a " / " name.
                "pompoen" to "groente",
                "uien" to "groente",
                "rode ui" to "groente",
                "2 kleine penen" to "groente",
                "cherrytomaatjes" to "groente",
                "bloemkool" to "groente",
                "verse peterselie" to "groente",
                "Wortels" to "groente",
                // A compound is what its last part is.
                "paprikapoeder" to "voorraad",
                "kippenbouillon" to "voorraad",
                "olijfolie" to "voorraad",
                "geitenkaas" to "zuivel-eieren",
                "roomboter" to "zuivel-eieren",
                "Crème fraîche" to "zuivel-eieren",
                "eieren" to "zuivel-eieren",
                "kippendijen" to "vlees-vis",
                "zalmfilet" to "vlees-vis",
                "rundergehakt" to "vlees-vis",
                "zuurdesembrood" to "brood",
                // Exceptions are whole keywords, which beat a part.
                "pindakaas" to "voorraad",
                "kokosmelk" to "voorraad",
                // Short keywords never match at the end of another word.
                "prei" to "groente",
                // No match: Overig.
                "koffiefilters" to "overig",
                "wc-papier" to "overig",
            )
        assertEquals(expected, expected.mapValues { (line, _) -> classifier.departmentOf(line) })
    }
}
