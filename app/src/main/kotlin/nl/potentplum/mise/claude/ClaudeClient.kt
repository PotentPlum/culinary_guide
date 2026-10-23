package nl.potentplum.mise.claude

import kotlinx.serialization.json.JsonObject

/** `output_config.effort` (spec §8.2). */
enum class Effort(
    val wire: String,
) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
}

sealed interface ContentPart {
    data class Text(
        val text: String,
    ) : ContentPart

    /** A JPEG or PNG, already downscaled (≤ 1568 px, ≤ 5 MB, spec §8.2). */
    class Image(
        val bytes: ByteArray,
        val mediaType: String = "image/jpeg",
    ) : ContentPart
}

/**
 * One structured-output request (spec §8.1): a byte-stable [system] prompt first, then per-request [content]; the reply
 * must match [schema]. Never carries sampling parameters.
 */
data class ClaudeRequest(
    val model: String,
    val effort: Effort,
    val system: String,
    val content: List<ContentPart>,
    val schema: JsonObject,
    val maxTokens: Int = 16_000,
)

data class Usage(
    val inputTokens: Long,
    val outputTokens: Long,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
)

data class ClaudeResponse(
    /** The first text block, parsed as JSON; still to be validated by the caller (spec §8.6). */
    val json: JsonObject,
    val usage: Usage,
    val model: String,
)

/** Every way a Claude call can fail; the UI maps each to Dutch copy and never shows raw messages (spec §9). */
sealed class ClaudeError : Exception() {
    data object NoKey : ClaudeError() {
        private fun readResolve(): Any = NoKey
    }

    /** 401: the key is wrong or revoked; Instellingen offers to replace it. */
    data object InvalidKey : ClaudeError() {
        private fun readResolve(): Any = InvalidKey
    }

    /** `stop_reason: "refusal"`; [explanation] from `stop_details` when present. */
    data class Refused(
        val explanation: String?,
    ) : ClaudeError()

    /** `stop_reason: "max_tokens"`. */
    data object Truncated : ClaudeError() {
        private fun readResolve(): Any = Truncated
    }

    data object Network : ClaudeError() {
        private fun readResolve(): Any = Network
    }

    data object RateLimited : ClaudeError() {
        private fun readResolve(): Any = RateLimited
    }

    data class Http(
        val code: Int,
    ) : ClaudeError()

    /** The reply was not JSON, or not the JSON the schema asked for. */
    data object InvalidResponse : ClaudeError() {
        private fun readResolve(): Any = InvalidResponse
    }
}

enum class KeyCheck { VALID, INVALID, UNREACHABLE }

/** The Claude API behind an interface (spec §8.1), so tests use recorded responses and never call the network. */
interface ClaudeClient {
    suspend fun send(
        key: String,
        request: ClaudeRequest,
    ): ClaudeResponse

    /** `GET /v1/models`: uses no tokens. */
    suspend fun checkKey(key: String): KeyCheck
}
