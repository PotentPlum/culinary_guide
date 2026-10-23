package nl.potentplum.mise.claude

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import nl.potentplum.mise.core.util.IoDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

/**
 * The Claude API over raw HTTPS with the app's OkHttp client (spec §8.1, the `OkHttpClaudeClient` path): structured
 * outputs, no sampling parameters, server-side refusal fallback on Sonnet/Opus, at most two retries for transient
 * failures and none for client errors. The key goes only into the `x-api-key` header; nothing here logs.
 */
class OkHttpClaudeClient(
    client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val baseUrl: String = BASE_URL,
    /** Tests replace the backoff wait. */
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) : ClaudeClient {
    @Inject
    constructor(
        client: OkHttpClient,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ) : this(client, dispatcher, BASE_URL)

    private val client =
        client
            .newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun send(
        key: String,
        request: ClaudeRequest,
    ): ClaudeResponse {
        val body = body(request).toString().toRequestBody(JSON)
        val builder =
            Request
                .Builder()
                .url("$baseUrl/v1/messages")
                .post(body)
                .header("x-api-key", key)
                .header("anthropic-version", API_VERSION)
        if (ClaudeModels.supportsRefusalFallback(request.model)) builder.header("anthropic-beta", FALLBACK_BETA)
        val text = execute(builder.build())
        return parse(text, request.model)
    }

    override suspend fun checkKey(key: String): KeyCheck {
        val request =
            Request
                .Builder()
                .url("$baseUrl/v1/models?limit=1")
                .get()
                .header("x-api-key", key)
                .header("anthropic-version", API_VERSION)
                .build()
        return try {
            execute(request)
            KeyCheck.VALID
        } catch (e: ClaudeError) {
            // 401 and 403 mean the key itself is refused; anything else says nothing about the key.
            if (e == ClaudeError.InvalidKey || (e as? ClaudeError.Http)?.code == 403) KeyCheck.INVALID else KeyCheck.UNREACHABLE
        }
    }

    /** Sends with the retry policy; returns the body of a 2xx response. */
    private suspend fun execute(request: Request): String {
        var attempt = 0
        while (true) {
            val outcome =
                withContext(dispatcher) {
                    try {
                        client.newCall(request).execute().use { response ->
                            Attempt(response.code, response.body.string(), retryAfter(response))
                        }
                    } catch (_: IOException) {
                        null
                    }
                }
            val retryable = outcome == null || outcome.code in RETRY_CODES
            if (outcome != null && outcome.code in 200..299) return outcome.body
            if (!retryable || attempt >= MAX_RETRIES) throw error(outcome)
            sleep(outcome?.retryAfterMillis ?: backoff(attempt))
            attempt++
        }
    }

    private class Attempt(
        val code: Int,
        val body: String,
        val retryAfterMillis: Long?,
    )

    private fun error(outcome: Attempt?): ClaudeError =
        when (outcome?.code) {
            null -> ClaudeError.Network
            401 -> ClaudeError.InvalidKey
            429 -> ClaudeError.RateLimited
            else -> ClaudeError.Http(outcome.code)
        }

    private fun retryAfter(response: Response): Long? = response.header("retry-after")?.toDoubleOrNull()?.let { (it * 1000).toLong() }

    /** Exponential backoff with jitter: about 1 s, then 2 s. */
    private fun backoff(attempt: Int): Long = (1000L shl attempt) + Random.nextLong(0, 500)

    private fun body(request: ClaudeRequest): JsonObject =
        buildJsonObject {
            put("model", request.model)
            put("max_tokens", request.maxTokens)
            put("system", request.system)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        request.content.forEach { part ->
                            when (part) {
                                is ContentPart.Text -> {
                                    addJsonObject {
                                        put("type", "text")
                                        put("text", part.text)
                                    }
                                }

                                is ContentPart.Image -> {
                                    addJsonObject {
                                        put("type", "image")
                                        putJsonObject("source") {
                                            put("type", "base64")
                                            put("media_type", part.mediaType)
                                            put("data", Base64.getEncoder().encodeToString(part.bytes))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            putJsonObject("output_config") {
                put("effort", request.effort.wire)
                putJsonObject("format") {
                    put("type", "json_schema")
                    put("schema", request.schema)
                }
            }
            if (ClaudeModels.supportsRefusalFallback(request.model)) put("fallbacks", "default")
        }

    /** Response checks in the spec's order (§8.1): refusal, then `max_tokens`, then the first text block as JSON. */
    private fun parse(
        text: String,
        requestedModel: String,
    ): ClaudeResponse {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: throw ClaudeError.InvalidResponse
        when (root["stop_reason"]?.jsonPrimitive?.contentOrNull) {
            "refusal" -> {
                val explanation = (root["stop_details"] as? JsonObject)?.get("explanation")?.jsonPrimitive?.contentOrNull
                throw ClaudeError.Refused(explanation)
            }

            "max_tokens" -> {
                throw ClaudeError.Truncated
            }
        }
        val firstText =
            (root["content"] as? JsonArray)
                ?.map { it.jsonObject }
                ?.firstOrNull { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
                ?.get("text")
                ?.jsonPrimitive
                ?.contentOrNull ?: throw ClaudeError.InvalidResponse
        val parsed = runCatching { json.parseToJsonElement(firstText).jsonObject }.getOrNull() ?: throw ClaudeError.InvalidResponse
        val usage = root["usage"] as? JsonObject

        fun count(name: String) = usage?.get(name)?.jsonPrimitive?.longOrNull ?: 0
        return ClaudeResponse(
            json = parsed,
            usage =
                Usage(
                    count("input_tokens"),
                    count("output_tokens"),
                    count("cache_read_input_tokens"),
                    count("cache_creation_input_tokens"),
                ),
            model = (root["model"] as? JsonPrimitive)?.contentOrNull ?: requestedModel,
        )
    }

    companion object {
        const val BASE_URL = "https://api.anthropic.com"
        const val API_VERSION = "2026-10-21"
        const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
        const val MAX_RETRIES = 2
        private const val READ_TIMEOUT_SECONDS = 180L
        private val RETRY_CODES = setOf(408, 429, 500, 502, 503, 529)
        private val JSON = "application/json".toMediaType()
    }
}
