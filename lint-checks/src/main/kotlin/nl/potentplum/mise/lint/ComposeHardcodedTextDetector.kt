package nl.potentplum.mise.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * Android's `HardcodedText` only looks at XML layouts. This is its Compose counterpart: a string literal
 * with letters passed as user-visible text to a `@Composable` function (spec §15.4: Dutch UI only, all
 * text in `strings.xml`).
 */
class ComposeHardcodedTextDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolve() ?: return
                if (!method.hasAnnotation(COMPOSABLE)) return
                if (node.isInsidePreview()) return
                val mapping = context.evaluator.computeArgumentMapping(node, method)
                for ((argument, parameter) in mapping) {
                    if (parameter.name !in TEXT_PARAMETERS) continue
                    if (argument.hasLiteralText()) {
                        context.report(
                            ISSUE,
                            argument,
                            context.getLocation(argument),
                            "Hardcoded text in `${node.methodName ?: method.name}(${parameter.name} = …)`: use a string resource from `data/res/values/strings.xml`",
                        )
                    }
                }
            }
        }

    /** `@Preview` functions show sample data, not UI text. */
    private fun UElement.isInsidePreview(): Boolean =
        getParentOfType(UMethod::class.java, true)?.uAnnotations?.any { annotation ->
            annotation.qualifiedName?.substringAfterLast('.')?.endsWith("Preview") == true
        } == true

    private fun UExpression.hasLiteralText(): Boolean =
        when (val expression = skipParenthesizedExprDown()) {
            is ULiteralExpression -> (expression.value as? String)?.any(Char::isLetter) == true
            is UPolyadicExpression -> expression.operands.any { it.hasLiteralText() }
            else -> false
        }

    companion object {
        private const val COMPOSABLE = "androidx.compose.runtime.Composable"

        private val TEXT_PARAMETERS =
            setOf(
                "text",
                "contentDescription",
                "label",
                "title",
                "placeholder",
                "message",
                "actionLabel",
                "onClickLabel",
                "stateDescription",
            )

        val ISSUE: Issue =
            Issue.create(
                id = "ComposeHardcodedText",
                briefDescription = "Hardcoded text in a composable",
                explanation =
                    "All user-visible text is Dutch and lives in `data/res/values/strings.xml`. " +
                        "Use `stringResource(R.string.…)` or `styledStringResource(R.string.…)` instead of a literal.",
                category = Category.I18N,
                priority = 8,
                severity = Severity.ERROR,
                implementation = Implementation(ComposeHardcodedTextDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}
