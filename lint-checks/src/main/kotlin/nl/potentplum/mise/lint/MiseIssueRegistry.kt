package nl.potentplum.mise.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class MiseIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(ComposeHardcodedTextDetector.ISSUE)

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(vendorName = "Mise")
}
