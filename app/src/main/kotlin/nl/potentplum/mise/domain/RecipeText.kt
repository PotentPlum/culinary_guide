package nl.potentplum.mise.domain

/** A recipe as plain text for "Delen" (spec §6.8): title, source, ingredients per group, numbered steps. */
object RecipeText {
    data class Group(
        val name: String?,
        val lines: List<String>,
    )

    fun format(
        title: String,
        sourceUrl: String?,
        ingredientGroups: List<Group>,
        stepGroups: List<Group>,
        ingredientsHeading: String,
        stepsHeading: String,
    ): String =
        buildString {
            appendLine(title)
            sourceUrl?.let { appendLine(it) }
            if (ingredientGroups.any { it.lines.isNotEmpty() }) {
                appendLine()
                appendLine(ingredientsHeading)
                ingredientGroups.forEach { group ->
                    group.name?.let { appendLine(it) }
                    group.lines.forEach { appendLine("- $it") }
                }
            }
            if (stepGroups.any { it.lines.isNotEmpty() }) {
                appendLine()
                appendLine(stepsHeading)
                stepGroups.forEach { group ->
                    group.name?.let { appendLine(it) }
                    group.lines.forEachIndexed { i, line -> appendLine("${i + 1}. $line") }
                }
            }
        }.trimEnd()
}
