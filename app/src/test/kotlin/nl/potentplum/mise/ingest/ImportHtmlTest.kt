package nl.potentplum.mise.ingest

import org.junit.Assert.assertEquals
import org.junit.Test

/** Imported text never shows markup (spec §6.9): links in WPRM data and links encoded as entities are plain text in the preview. */
class ImportHtmlTest {
    @Test
    fun `tags that only appear after decoding entities are removed`() {
        assertEquals(
            "2 el groene currypasta",
            JsonLdRecipeParser.cleanText("2 el &lt;a href=&quot;https://example.com/paste&quot;&gt;groene currypasta&lt;/a&gt;"),
        )
    }

    @Test
    fun `a comparison sign is text, not a tag`() {
        assertEquals("klaar in < 5 min, > 3 personen", JsonLdRecipeParser.cleanText("klaar in &lt; 5 min, &gt; 3 personen"))
    }

    @Test
    fun `WPRM names and notes with links become plain text`() {
        val page =
            """<html><script>window.wprm_recipes = {"recipe-1":{"ingredients":[
                {"amount":"3","unit":"tbsp","name":"<a href=\"https://example.com/paste\" target=\"_blank\">green curry paste</a>",
                 "notes":"see &lt;a href=&quot;https://example.com&quot;&gt;my recipe&lt;/a&gt; &amp; tips"}
            ]}};</script></html>"""
        val items = WprmExtractor.extractIngredients(page)!!.single().ingredients
        assertEquals("3 tbsp green curry paste, see my recipe & tips", items.single().text)
    }
}
