package nl.potentplum.mise.security

import timber.log.Timber

/** Removes anything that looks like an Anthropic API key from text that may be logged or shown (spec §9). */
object SecretScrubber {
    private val API_KEY = Regex("sk-ant-[A-Za-z0-9_-]+")
    const val REPLACEMENT = "sk-ant-…"

    fun scrub(text: String): String = API_KEY.replace(text, REPLACEMENT)

    fun containsSecret(text: String?): Boolean = text != null && API_KEY.containsMatchIn(text)

    /** [throwable] itself when its chain holds no key, else a copy with scrubbed messages and the same stack traces. */
    fun scrub(throwable: Throwable): Throwable {
        val chain = generateSequence(throwable) { it.cause }.take(MAX_CAUSES).toList()
        if (chain.none { containsSecret(it.message) }) return throwable
        return copy(throwable, depth = 0)
    }

    private fun copy(
        throwable: Throwable,
        depth: Int,
    ): Throwable {
        val cause = throwable.cause?.takeIf { depth < MAX_CAUSES }?.let { copy(it, depth + 1) }
        return ScrubbedThrowable(throwable.javaClass.name, throwable.message?.let(::scrub), cause).apply {
            stackTrace = throwable.stackTrace
        }
    }

    private const val MAX_CAUSES = 16
}

/** Stands in for an exception whose message held a key; prints as the original class. */
class ScrubbedThrowable(
    private val originalClass: String,
    message: String?,
    cause: Throwable?,
) : RuntimeException(message, cause) {
    override fun toString(): String = message?.let { "$originalClass: $it" } ?: originalClass
}

/** Debug-only Timber tree; release builds plant none, so logging is a no-op there. */
class ScrubbingDebugTree : Timber.DebugTree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) = super.log(priority, tag, SecretScrubber.scrub(message), t?.let(SecretScrubber::scrub))
}

/** Scrubs crashes before the platform handler logs them. */
class ScrubbingExceptionHandler(
    private val delegate: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        delegate?.uncaughtException(thread, SecretScrubber.scrub(throwable))
    }
}
