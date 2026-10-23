package nl.potentplum.mise.testing

import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository

class FakeContentRepository(
    private val content: Content = TestContent.content,
) : ContentRepository {
    override suspend fun content(): Content = content
}
