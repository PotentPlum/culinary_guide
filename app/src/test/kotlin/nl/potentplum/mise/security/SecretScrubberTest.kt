package nl.potentplum.mise.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SecretScrubberTest {
    private val key = "sk-ant-api03-Zx_9-QQ"

    @Test
    fun `keys are removed from text`() {
        assertEquals("x-api-key: sk-ant-… failed", SecretScrubber.scrub("x-api-key: $key failed"))
        assertEquals("nothing secret", SecretScrubber.scrub("nothing secret"))
    }

    @Test
    fun `exceptions without a key are returned as they are`() {
        val original = IOException("timeout")
        assertSame(original, SecretScrubber.scrub(original))
    }

    @Test
    fun `a key anywhere in the cause chain is scrubbed and the stack kept`() {
        val original = IllegalStateException("request failed", IOException("401 for $key"))
        val scrubbed = SecretScrubber.scrub(original)

        assertFalse(scrubbed.toString().contains(key))
        assertFalse(scrubbed.cause!!.message!!.contains(key))
        assertTrue(scrubbed.cause.toString().startsWith("java.io.IOException: 401 for sk-ant-…"))
        assertTrue(scrubbed.toString().startsWith("java.lang.IllegalStateException"))
        assertEquals(original.stackTrace.toList(), scrubbed.stackTrace.toList())
    }

    @Test
    fun `the crash handler passes a scrubbed throwable on`() {
        var received: Throwable? = null
        val handler = ScrubbingExceptionHandler { _, t -> received = t }
        handler.uncaughtException(Thread.currentThread(), RuntimeException("boom $key"))
        assertEquals("java.lang.RuntimeException: boom sk-ant-…", received.toString())
    }
}
