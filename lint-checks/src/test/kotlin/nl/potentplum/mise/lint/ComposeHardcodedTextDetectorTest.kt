package nl.potentplum.mise.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class ComposeHardcodedTextDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposeHardcodedTextDetector()

    override fun getIssues(): List<Issue> = listOf(ComposeHardcodedTextDetector.ISSUE)

    override fun lint(): TestLintTask = super.lint().allowMissingSdk()

    private val stubs: Array<TestFile> =
        arrayOf(
            kotlin(
                """
                package androidx.compose.runtime
                annotation class Composable
                @Composable fun <T> remember(key: String, calc: () -> T): T = calc()
                """,
            ).indented(),
            kotlin(
                """
                package androidx.compose.material3
                import androidx.compose.runtime.Composable
                @Composable fun Text(text: String, maxLines: Int = 1) {}
                @Composable fun Icon(name: String, contentDescription: String?) {}
                """,
            ).indented(),
        )

    fun testLiteralTextIsAnError() {
        lint()
            .files(
                *stubs,
                kotlin(
                    """
                    package test
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    @Composable fun Screen() { Text("Hallo") }
                    """,
                ).indented(),
            ).run()
            .expectErrorCount(1)
            .expectContains("Hardcoded text in Text(text = …)")
    }

    fun testTemplatesAndContentDescriptionsAreErrors() {
        lint()
            .files(
                *stubs,
                kotlin(
                    """
                    package test
                    import androidx.compose.material3.Icon
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    @Composable fun Screen(n: Int) {
                        Text(text = "${'$'}n recepten")
                        Icon("leaf", contentDescription = "Blad")
                    }
                    """,
                ).indented(),
            ).run()
            .expectErrorCount(2)
    }

    fun testResourcesSymbolsAndKeysAreClean() {
        lint()
            .files(
                *stubs,
                kotlin(
                    """
                    package test
                    import androidx.compose.material3.Icon
                    import androidx.compose.material3.Text
                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.remember
                    @Composable fun Screen(title: String, n: Int) {
                        Text(title)
                        Text("·")
                        Text("${'$'}n")
                        Icon("leaf", contentDescription = null)
                        remember(key = "scroll") { 1 }
                    }
                    """,
                ).indented(),
            ).run()
            .expectClean()
    }
}
