package nl.potentplum.mise.claude

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OkHttpClaudeClientTest {
    private val server = MockWebServer()
    private val waits = mutableListOf<Long>()
    private lateinit var client: OkHttpClaudeClient

    private val schema = OutputSchemas.obj(mapOf("answer" to OutputSchemas.string()))

    private fun request(
        model: String = ClaudeModels.SONNET,
        content: List<ContentPart> = listOf(ContentPart.Text("Hoi")),
    ) = ClaudeRequest(model, Effort.MEDIUM, "Systeem", content, schema)

    private fun reply(
        text: String = """{"answer":"ja"}""",
        stopReason: String = "end_turn",
        extra: String = "",
    ) = MockResponse
        .Builder()
        .addHeader("Content-Type", "application/json")
        .body(
            """{"model":"claude-sonnet-5-5","stop_reason":"$stopReason",$extra
               "content":[{"type":"thinking","thinking":""},{"type":"text","text":${Json.encodeToString(String.serializer(), text)}}],
               "usage":{"input_tokens":1200,"output_tokens":300,"cache_read_input_tokens":50}}""",
        ).build()

    @Before
    fun setUp() {
        server.start()
        client = OkHttpClaudeClient(OkHttpClient(), Dispatchers.IO, server.url("/").toString().trimEnd('/'), sleep = { waits += it })
    }

    @After
    fun tearDown() = server.close()

    private fun sentBody(): JsonObject = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    @Test
    fun `the request uses structured outputs, effort and no sampling parameters`() =
        runTest {
            server.enqueue(reply())
            val response = client.send("sk-ant-test", request())
            assertEquals("ja", response.json["answer"]!!.jsonPrimitive.content)
            assertEquals(Usage(1200, 300, 50, 0), response.usage)

            val recorded = server.takeRequest()
            assertEquals("sk-ant-test", recorded.headers["x-api-key"])
            assertEquals(OkHttpClaudeClient.API_VERSION, recorded.headers["anthropic-version"])
            val body = Json.parseToJsonElement(recorded.body!!.utf8()).jsonObject
            assertEquals(
                "json_schema",
                body["output_config"]!!
                    .jsonObject["format"]!!
                    .jsonObject["type"]!!
                    .jsonPrimitive.content,
            )
            assertEquals("medium", body["output_config"]!!.jsonObject["effort"]!!.jsonPrimitive.content)
            assertEquals("Systeem", body["system"]!!.jsonPrimitive.content)
            listOf("temperature", "top_p", "top_k", "thinking").forEach { assertFalse(it, it in body) }
        }

    @Test
    fun `sonnet and opus opt into the refusal fallback, haiku does not`() =
        runTest {
            server.enqueue(reply())
            server.enqueue(reply())
            client.send("k", request(ClaudeModels.OPUS))
            client.send("k", request(ClaudeModels.HAIKU))
            val opus = server.takeRequest()
            assertEquals(OkHttpClaudeClient.FALLBACK_BETA, opus.headers["anthropic-beta"])
            assertEquals(
                "default",
                Json
                    .parseToJsonElement(opus.body!!.utf8())
                    .jsonObject["fallbacks"]!!
                    .jsonPrimitive.content,
            )
            val haiku = server.takeRequest()
            assertNull(haiku.headers["anthropic-beta"])
            assertFalse("fallbacks" in Json.parseToJsonElement(haiku.body!!.utf8()).jsonObject)
        }

    @Test
    fun `images are sent as base64 blocks`() =
        runTest {
            server.enqueue(reply())
            client.send("k", request(content = listOf(ContentPart.Image(byteArrayOf(1, 2, 3)), ContentPart.Text("Lees"))))
            val content = sentBody()["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
            val image = content[0].jsonObject
            assertEquals("image", image["type"]!!.jsonPrimitive.content)
            assertEquals("AQID", image["source"]!!.jsonObject["data"]!!.jsonPrimitive.content)
            assertEquals("image/jpeg", image["source"]!!.jsonObject["media_type"]!!.jsonPrimitive.content)
        }

    @Test
    fun `a refusal is reported with its explanation`() =
        runTest {
            server.enqueue(
                reply(stopReason = "refusal", extra = """"stop_details":{"type":"refusal","category":"cyber","explanation":"Nee."},"""),
            )
            assertEquals(ClaudeError.Refused("Nee."), runCatching { client.send("k", request()) }.exceptionOrNull())
        }

    @Test
    fun `a truncated reply is reported`() =
        runTest {
            server.enqueue(reply(stopReason = "max_tokens"))
            assertEquals(ClaudeError.Truncated, runCatching { client.send("k", request()) }.exceptionOrNull())
        }

    @Test
    fun `a reply that is not json is invalid`() =
        runTest {
            server.enqueue(reply(text = "Hier is je recept!"))
            assertEquals(ClaudeError.InvalidResponse, runCatching { client.send("k", request()) }.exceptionOrNull())
        }

    @Test
    fun `transient errors are retried twice with backoff`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(529).build())
            server.enqueue(MockResponse.Builder().code(503).build())
            server.enqueue(reply())
            client.send("k", request())
            assertEquals(3, server.requestCount)
            assertEquals(2, waits.size)
            assertTrue(waits[1] > waits[0])
        }

    @Test
    fun `retry-after is honoured and a third failure gives up`() =
        runTest {
            repeat(3) {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(429)
                        .addHeader("retry-after", "7")
                        .build(),
                )
            }
            assertEquals(ClaudeError.RateLimited, runCatching { client.send("k", request()) }.exceptionOrNull())
            assertEquals(3, server.requestCount)
            assertEquals(listOf(7000L, 7000L), waits)
        }

    @Test
    fun `client errors are never retried`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(400).build())
            assertEquals(ClaudeError.Http(400), runCatching { client.send("k", request()) }.exceptionOrNull())
            server.enqueue(MockResponse.Builder().code(401).build())
            assertEquals(ClaudeError.InvalidKey, runCatching { client.send("k", request()) }.exceptionOrNull())
            assertEquals(2, server.requestCount)
            assertTrue(waits.isEmpty())
        }

    @Test
    fun `the key check uses the models endpoint`() =
        runTest {
            server.enqueue(MockResponse.Builder().body("""{"data":[]}""").build())
            assertEquals(KeyCheck.VALID, client.checkKey("k"))
            assertTrue(server.takeRequest().target.startsWith("/v1/models"))
            server.enqueue(MockResponse.Builder().code(401).build())
            assertEquals(KeyCheck.INVALID, client.checkKey("k"))
            repeat(3) { server.enqueue(MockResponse.Builder().code(500).build()) }
            assertEquals(KeyCheck.UNREACHABLE, client.checkKey("k"))
        }

    @Test
    fun `an unreachable api is a network error after retries`() =
        runTest {
            val dead = OkHttpClaudeClient(OkHttpClient(), Dispatchers.IO, "http://127.0.0.1:1", sleep = { waits += it })
            assertEquals(ClaudeError.Network, runCatching { dead.send("k", request()) }.exceptionOrNull())
            assertEquals(2, waits.size)
        }
}
