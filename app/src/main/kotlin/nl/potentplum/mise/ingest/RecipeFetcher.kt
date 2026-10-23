package nl.potentplum.mise.ingest

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import nl.potentplum.mise.core.util.IoDispatcher
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

/** Why fetching or reading a recipe page failed; the UI maps each to a Dutch `recipes_error_*` string. */
sealed class RecipeImportError : Exception() {
    data object InvalidUrl : RecipeImportError() {
        private fun readResolve(): Any = InvalidUrl
    }

    data object Unreachable : RecipeImportError() {
        private fun readResolve(): Any = Unreachable
    }

    data class Http(
        val code: Int,
    ) : RecipeImportError()

    data object TooLarge : RecipeImportError() {
        private fun readResolve(): Any = TooLarge
    }

    data object NoRecipe : RecipeImportError() {
        private fun readResolve(): Any = NoRecipe
    }
}

data class FetchedPage(
    /** After redirects. */
    val url: String,
    val html: String,
)

object RecipeUrls {
    /** A user-entered link as an https URL: "ah.nl/recept" and "http://…" both become https; other schemes → null. */
    fun normalize(input: String): HttpUrl? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val withScheme =
            when {
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.substring("http://".length)
                "://" in trimmed -> return null
                else -> "https://$trimmed"
            }
        return withScheme.toHttpUrlOrNull()?.takeIf { it.host.contains('.') }
    }

    /** Never sends cleartext (spec §10): http → https. */
    fun upgrade(url: HttpUrl): HttpUrl = if (url.scheme == "http") url.newBuilder().scheme("https").build() else url
}

/**
 * Downloads a recipe page (spec §10): https only, at most 5 redirects, browser-like headers, 20 s timeouts, the body
 * capped at 5 MB while streaming, and the charset from the headers or `<meta charset>`.
 */
class RecipeFetcher(
    client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
    /** Tests run against a plain-http MockWebServer. */
    private val upgradeToHttps: Boolean = true,
) {
    @Inject
    constructor(
        client: OkHttpClient,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ) : this(client, dispatcher, true)

    private val client =
        client
            .newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    suspend fun fetch(input: String): FetchedPage {
        var url =
            (if (upgradeToHttps) RecipeUrls.normalize(input) else input.trim().toHttpUrlOrNull())
                ?: throw RecipeImportError.InvalidUrl
        repeat(MAX_REDIRECTS + 1) {
            val response =
                try {
                    client.newCall(request(url)).await()
                } catch (e: IOException) {
                    throw RecipeImportError.Unreachable
                }
            response.use {
                if (it.isRedirect) {
                    val location = it.header("Location")?.let(url::resolve) ?: throw RecipeImportError.Http(it.code)
                    url = if (upgradeToHttps) RecipeUrls.upgrade(location) else location
                    return@repeat
                }
                if (!it.isSuccessful) throw RecipeImportError.Http(it.code)
                return FetchedPage(url.toString(), withContext(dispatcher) { readCapped(it) })
            }
        }
        throw RecipeImportError.Unreachable
    }

    private fun request(url: HttpUrl): Request =
        Request
            .Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "nl-NL,nl;q=0.9,en;q=0.8")
            .build()

    private fun readCapped(response: Response): String {
        val body = response.body
        val source = body.source()
        val buffer = Buffer()
        try {
            while (buffer.size <= MAX_BYTES) {
                if (source.read(buffer, 64 * 1024L) == -1L) break
            }
        } catch (e: IOException) {
            throw RecipeImportError.Unreachable
        }
        if (buffer.size > MAX_BYTES) throw RecipeImportError.TooLarge
        val bytes = buffer.readByteArray()
        val charset = body.contentType()?.charset() ?: sniffCharset(bytes) ?: Charsets.UTF_8
        return String(bytes, charset)
    }

    private suspend fun Call.await(): Response =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            enqueue(
                object : Callback {
                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        continuation.resume(response) { _, value, _ -> value.close() }
                    }

                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        continuation.resumeWithException(e)
                    }
                },
            )
        }

    companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_BYTES = 5L * 1024 * 1024
        private const val TIMEOUT_SECONDS = 20L
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        private val META_CHARSET = Regex("<meta[^>]+charset\\s*=\\s*[\"']?([A-Za-z0-9_\\-:.]+)", RegexOption.IGNORE_CASE)

        /** `<meta charset="…">` or `<meta http-equiv="Content-Type" content="…; charset=…">` near the top of the page. */
        fun sniffCharset(bytes: ByteArray): Charset? {
            val head = String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1)
            val name = META_CHARSET.find(head)?.groupValues?.get(1) ?: return null
            return runCatching { Charset.forName(name) }.getOrNull()
        }
    }
}
