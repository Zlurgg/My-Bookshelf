package uk.co.zlurgg.mybookshelf.book.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.book.data.network.api.GoogleBooksApiService
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

/**
 * Tests for [GoogleBooksRemoteBookDataSource] via Ktor [MockEngine].
 *
 * Coverage: query-building rules (single-word vs multi-word author filters,
 * combined filters), the blank-API-key short-circuit from S1 (returns
 * [DataError.Remote.PROVIDER_UNAVAILABLE] without an HTTP call), the
 * `X-Goog-Api-Key` header auth introduced in S2, the no-retry-on-429
 * policy from HttpClientFactory, and the success-mapping that includes
 * the `searchSnippet` field added in S4.
 *
 * Robolectric is required because `GoogleBookMappers.stripHtml` calls into
 * `HtmlCompat.fromHtml`, which delegates to `android.text.Html` — not stubbed
 * on the JVM-only test classpath.
 */
@RunWith(RobolectricTestRunner::class)
class GoogleBooksRemoteBookDataSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private val systemLanguageProvider = object : SystemLanguageProvider {
        override fun getCurrentLanguageCode(): String = "eng"
        override fun getRawLanguageCode(): String = "en"
    }

    private fun buildDataSource(
        apiKey: String = "test-api-key",
        responseHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): GoogleBooksRemoteBookDataSource {
        val engine = MockEngine { request ->
            requests.add(request)
            responseHandler(request)
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val apiService = GoogleBooksApiService(
            httpClient = httpClient,
            apiKeyProvider = { apiKey },
        )
        return GoogleBooksRemoteBookDataSource(
            apiService = apiService,
            systemLanguageProvider = systemLanguageProvider,
            apiKeyProvider = { apiKey },
        )
    }

    private fun MockRequestHandleScope.respondJson(
        json: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(
        content = ByteReadChannel(json),
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    @Test
    fun `blank API key short-circuits to PROVIDER_UNAVAILABLE without HTTP call`() = runTest {
        val sut = buildDataSource(apiKey = "") { respondJson(EMPTY_RESPONSE) }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            DataError.Remote.PROVIDER_UNAVAILABLE,
            (result as Result.Error).error,
        )
        assertEquals("Should NOT issue an HTTP request", 0, requests.size)
    }

    @Test
    fun `blank API key short-circuit also catches whitespace-only keys`() = runTest {
        val sut = buildDataSource(apiKey = "   ") { respondJson(EMPTY_RESPONSE) }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(
            DataError.Remote.PROVIDER_UNAVAILABLE,
            (result as Result.Error).error,
        )
        assertEquals(0, requests.size)
    }

    @Test
    fun `outgoing request carries X-Goog-Api-Key header with configured value`() = runTest {
        val sut = buildDataSource(apiKey = "AIzaTestKey") { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin")

        assertEquals(1, requests.size)
        assertEquals(
            "AIzaTestKey",
            requests[0].headers["X-Goog-Api-Key"],
        )
    }

    @Test
    fun `outgoing request does NOT include 'key' URL query parameter`() = runTest {
        // S2 moved auth from ?key= URL param to header to keep the key out of
        // request logs. Pin that here so a future refactor cannot reintroduce it.
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin")

        assertEquals(1, requests.size)
        val params = requests[0].url.parameters
        assertNull("?key= must not be present", params["key"])
    }

    @Test
    fun `single-word author filter renders as inauthor without quotes`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin", authorFilter = "Bloch")

        val q = requests[0].url.parameters["q"] ?: error("missing q")
        assertEquals("kotlin inauthor:Bloch", q)
    }

    @Test
    fun `multi-word author filter is wrapped in quotes`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin", authorFilter = "Joshua Bloch")

        val q = requests[0].url.parameters["q"] ?: error("missing q")
        assertEquals("kotlin inauthor:\"Joshua Bloch\"", q)
    }

    @Test
    fun `combined title author and subject filters join into one q parameter`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(
            query = "effective",
            authorFilter = "Joshua Bloch",
            titleFilter = "Effective Java",
            subjectFilter = "Programming",
        )

        val q = requests[0].url.parameters["q"] ?: error("missing q")
        assertEquals(
            "effective inauthor:\"Joshua Bloch\" intitle:\"Effective Java\" subject:Programming",
            q,
        )
    }

    @Test
    fun `empty base query with only filters still issues a request`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        val result = sut.searchBooks(query = "  ", authorFilter = "Bloch")

        assertTrue(result is Result.Success)
        assertEquals(1, requests.size)
        val q = requests[0].url.parameters["q"] ?: error("missing q")
        assertEquals("inauthor:Bloch", q)
    }

    @Test
    fun `blank query and no filters produces empty q parameter`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "  ")

        val q = requests[0].url.parameters["q"]
        assertEquals("", q)
    }

    @Test
    fun `successful response maps to domain with searchSnippet populated`() = runTest {
        val sut = buildDataSource { respondJson(SUCCESS_RESPONSE) }

        val result = sut.searchBooks(query = "effective")

        assertTrue(result is Result.Success)
        val books = (result as Result.Success).data.books
        assertEquals(1, books.size)
        val book = books[0]
        assertEquals("test-id-1", book.id)
        assertEquals("Effective Java", book.title)
        assertEquals("Joshua Bloch", book.authors.single())
        assertEquals(BookProvider.GOOGLE_BOOKS, book.provider)
        assertEquals("A comprehensive guide to Java.", book.description)
        // searchSnippet from S4 — must be stripped of HTML
        assertEquals("Effective Java best practices", book.searchSnippet)
        // HTTPS coercion for thumbnail
        assertTrue(book.imageUrl.startsWith("https://"))
        // S1.7 HTTPS gating
        assertEquals("https://books.google.com/preview", book.previewLink)
        assertNotNull(book.isbn)
    }

    @Test
    fun `HTTP 429 maps to TOO_MANY_REQUESTS without retry`() = runTest {
        var calls = 0
        val sut = buildDataSource {
            calls++
            respond(
                content = "",
                status = HttpStatusCode.TooManyRequests,
            )
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            DataError.Remote.TOO_MANY_REQUESTS,
            (result as Result.Error).error,
        )
        // The HttpClientFactory retry policy explicitly excludes 429 — Google
        // returns 429 for daily quota exhaustion, which is not transient.
        assertEquals("429 must not be retried", 1, calls)
    }

    @Test
    fun `HTTP 401 maps to UNAUTHORIZED`() = runTest {
        val sut = buildDataSource {
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.UNAUTHORIZED, (result as Result.Error).error)
    }

    @Test
    fun `HTTP 403 maps to FORBIDDEN`() = runTest {
        val sut = buildDataSource {
            respond(content = "", status = HttpStatusCode.Forbidden)
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.FORBIDDEN, (result as Result.Error).error)
    }

    @Test
    fun `HTTP 400 maps to CLIENT_ERROR`() = runTest {
        val sut = buildDataSource {
            respond(content = "", status = HttpStatusCode.BadRequest)
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.CLIENT_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `HTTP 500 maps to SERVER_ERROR`() = runTest {
        // 5xx is retried by HttpClientFactory; we only assert the final mapping.
        val sut = buildDataSource {
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.SERVER_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `getBookDescription returns stripped description on success`() = runTest {
        val sut = buildDataSource { respondJson(DETAIL_RESPONSE) }

        val result = sut.getBookDescription("test-id-1", BookProvider.GOOGLE_BOOKS)

        assertTrue(result is Result.Success)
        assertEquals("Hello World", (result as Result.Success).data)
    }

    @Test
    fun `getBookDescription sends X-Goog-Api-Key header`() = runTest {
        val sut = buildDataSource(apiKey = "AIzaDetailKey") { respondJson(DETAIL_RESPONSE) }

        sut.getBookDescription("test-id-1", BookProvider.GOOGLE_BOOKS)

        assertEquals("AIzaDetailKey", requests[0].headers["X-Goog-Api-Key"])
    }

    @Test
    fun `getBookDescription does NOT short-circuit on blank API key`() = runTest {
        // The blank-key short-circuit lives on the search path; description
        // fetch routes by provider and is expected to attempt the HTTP call.
        // (If the upstream were ever extended to short-circuit here too, this
        // test catches the change and forces a conscious update.)
        val sut = buildDataSource(apiKey = "") {
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }

        val result = sut.getBookDescription("test-id-1", BookProvider.GOOGLE_BOOKS)

        assertTrue(result is Result.Error)
        assertEquals(1, requests.size)
    }

    companion object {
        private const val EMPTY_RESPONSE = """{"totalItems":0,"items":[]}"""

        private const val SUCCESS_RESPONSE = """
            {
              "totalItems": 1,
              "items": [
                {
                  "id": "test-id-1",
                  "volumeInfo": {
                    "title": "Effective Java",
                    "authors": ["Joshua Bloch"],
                    "description": "A comprehensive guide to Java.",
                    "publishedDate": "2018",
                    "imageLinks": {
                      "thumbnail": "http://books.google.com/img/test.jpg"
                    },
                    "previewLink": "https://books.google.com/preview",
                    "infoLink": "https://books.google.com/info",
                    "industryIdentifiers": [
                      {"type":"ISBN_13","identifier":"9780134685991"}
                    ]
                  },
                  "searchInfo": {
                    "textSnippet": "<b>Effective</b> Java best practices"
                  }
                }
              ]
            }
        """

        private const val DETAIL_RESPONSE = """
            {
              "id": "test-id-1",
              "volumeInfo": {
                "description": "<b>Hello</b> <i>World</i>"
              }
            }
        """
    }
}
