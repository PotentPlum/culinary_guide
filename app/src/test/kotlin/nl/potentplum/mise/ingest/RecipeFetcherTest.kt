package nl.potentplum.mise.ingest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecipeFetcherTest {
    private val server = MockWebServer()
    private val fetcher = RecipeFetcher(OkHttpClient(), Dispatchers.IO, upgradeToHttps = false)

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun url(path: String = "/recept") = server.url(path).toString()

    private suspend fun failure(input: String = url()): RecipeImportError? =
        runCatching { fetcher.fetch(input) }.exceptionOrNull() as? RecipeImportError

    @Test
    fun `fetches html with browser headers`() =
        runTest {
            server.enqueue(MockResponse.Builder().body("<html>pompoen</html>").build())
            val page = fetcher.fetch(url())
            assertEquals("<html>pompoen</html>", page.html)
            val request = server.takeRequest()
            assertTrue(request.headers["User-Agent"]!!.contains("Chrome"))
            assertTrue(request.headers["Accept-Language"]!!.startsWith("nl-NL"))
        }

    @Test
    fun `follows up to five redirects`() =
        runTest {
            repeat(5) { i ->
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(302)
                        .addHeader("Location", "/stap$i")
                        .build(),
                )
            }
            server.enqueue(MockResponse.Builder().body("klaar").build())
            val page = fetcher.fetch(url())
            assertEquals("klaar", page.html)
            assertTrue(page.url.endsWith("/stap4"))
        }

    @Test
    fun `a sixth redirect gives up`() =
        runTest {
            repeat(6) { i ->
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(301)
                        .addHeader("Location", "/stap$i")
                        .build(),
                )
            }
            assertEquals(RecipeImportError.Unreachable, failure())
        }

    @Test
    fun `http errors carry the status code`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(404).build())
            assertEquals(RecipeImportError.Http(404), failure())
        }

    @Test
    fun `bodies over 5 MB are refused`() =
        runTest {
            val big = Buffer().apply { write(ByteArray((RecipeFetcher.MAX_BYTES + 1).toInt()) { 'a'.code.toByte() }) }
            server.enqueue(MockResponse.Builder().body(big).build())
            assertEquals(RecipeImportError.TooLarge, failure())
        }

    @Test
    fun `the charset comes from the content type`() =
        runTest {
            val latin1 = Buffer().write("Crème brûlée".toByteArray(Charsets.ISO_8859_1))
            server.enqueue(
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "text/html; charset=ISO-8859-1")
                    .body(latin1)
                    .build(),
            )
            assertEquals("Crème brûlée", fetcher.fetch(url()).html)
        }

    @Test
    fun `the charset falls back to the meta tag`() =
        runTest {
            val html = "<html><head><meta charset=\"windows-1252\"></head><body>Paté</body></html>"
            server.enqueue(
                MockResponse
                    .Builder()
                    .addHeader(
                        "Content-Type",
                        "text/html",
                    ).body(Buffer().write(html.toByteArray(Charsets.ISO_8859_1)))
                    .build(),
            )
            assertTrue(fetcher.fetch(url()).html.contains("Paté"))
        }

    @Test
    fun `an unreachable host is reported as such`() =
        runTest {
            val dead = url()
            server.close()
            assertEquals(RecipeImportError.Unreachable, failure(dead))
        }

    @Test
    fun `invalid input is rejected before any request`() =
        runTest {
            assertEquals(RecipeImportError.InvalidUrl, failure("geen link"))
        }

    @Test
    fun `urls are normalised to https`() {
        assertEquals("https://www.ah.nl/recept", RecipeUrls.normalize("http://www.ah.nl/recept").toString())
        assertEquals("https://ah.nl/recept", RecipeUrls.normalize("  ah.nl/recept ").toString())
        assertEquals("https://ah.nl/", RecipeUrls.normalize("HTTPS://ah.nl").toString())
        assertNull(RecipeUrls.normalize("ftp://ah.nl/recept"))
        assertNull(RecipeUrls.normalize("javascript:alert(1)"))
        assertNull(RecipeUrls.normalize("pompoensoep"))
        assertNull(RecipeUrls.normalize("ah.nl/een recept"))
    }

    @Test
    fun `redirect targets are upgraded too`() {
        assertEquals("https", RecipeUrls.upgrade(RecipeUrls.normalize("https://a.nl")!!.resolve("http://b.nl/x")!!).scheme)
    }
}
