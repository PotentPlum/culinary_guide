package nl.potentplum.mise.ingest

/** Reads what other apps share into Mise (spec §6.9). */
object SharedContentParser {
    private val URL = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}…"

    /** The first http(s) URL in shared text such as "Bekijk dit recept https://… via Chrome", without trailing punctuation. */
    fun firstUrl(text: String): String? = URL.find(text)?.value?.trimEnd { it in TRAILING_PUNCTUATION }
}
