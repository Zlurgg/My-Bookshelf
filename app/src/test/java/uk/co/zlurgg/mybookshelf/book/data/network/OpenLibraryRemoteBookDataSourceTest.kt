package uk.co.zlurgg.mybookshelf.book.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
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
import uk.co.zlurgg.mybookshelf.book.data.network.api.OpenLibraryApiService
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

/**
 * Tests for [OpenLibraryRemoteBookDataSource] via Ktor [MockEngine].
 *
 * Recovers and updates coverage from the removed `KtorRemoteBookDataSourceTest`
 * (deleted when the data source was split into provider-specific classes).
 * Asserts query building (single + multi-word filters, combined fields),
 * the empty-query path, success-mapping, and HTTP error mapping.
 *
 * Unlike the Google Books equivalent there is no API-key short-circuit and
 * no auth header; the OL endpoints are anonymous.
 */
class OpenLibraryRemoteBookDataSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private val systemLanguageProvider = object : SystemLanguageProvider {
        // OL uses three-letter ISO 639-2 codes.
        override fun getCurrentLanguageCode(): String = "eng"
        override fun getRawLanguageCode(): String = "en"
    }

    private fun buildDataSource(
        responseHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): OpenLibraryRemoteBookDataSource {
        val engine = MockEngine { request ->
            requests.add(request)
            responseHandler(request)
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val apiService = OpenLibraryApiService(httpClient)
        return OpenLibraryRemoteBookDataSource(
            apiService = apiService,
            systemLanguageProvider = systemLanguageProvider,
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
    fun `single-word author filter renders as author without quotes`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin", authorFilter = "Bloch")

        val q = requests[0].url.parameters["q"] ?: error("missing q")
        assertEquals("kotlin author:Bloch", q)
    }

    @Test
    fun `multi-word author filter is wrapped in quotes`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin", authorFilter = "Joshua Bloch")

        val q = requests[0].url.parameters["q"] ?: error("missing q")
        assertEquals("kotlin author:\"Joshua Bloch\"", q)
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
            "effective author:\"Joshua Bloch\" title:\"Effective Java\" subject:Programming",
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
        assertEquals("author:Bloch", q)
    }

    @Test
    fun `blank query and no filters produces empty q parameter`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "  ")

        assertEquals("", requests[0].url.parameters["q"])
    }

    @Test
    fun `request passes through resultLimit language and sort parameters`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(
            query = "kotlin",
            resultLimit = 25,
            language = "fra",
            sort = "rating",
        )

        val params = requests[0].url.parameters
        assertEquals("25", params["limit"])
        assertEquals("fra", params["language"])
        assertEquals("rating", params["sort"])
    }

    @Test
    fun `language defaults to system-language code when unspecified`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin")

        // SystemLanguageProvider stub returns "eng" — OL uses three-letter codes.
        assertEquals("eng", requests[0].url.parameters["language"])
    }

    @Test
    fun `null resultLimit resolves to MAX_RESULTS at the data source and sends limit=100`() = runTest {
        // C1 invariant: the OL data source is the single source of truth for
        // limit resolution so BookSearchResponse.pageSize is deterministic.
        // The API service stays a thin pass-through; in production it never
        // receives null from this data source.
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin")

        assertEquals("100", requests[0].url.parameters["limit"])
    }

    @Test
    fun `startIndex passes through as offset query parameter`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin", startIndex = 200)

        assertEquals("200", requests[0].url.parameters["offset"])
    }

    @Test
    fun `null startIndex omits the offset query parameter`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin")

        assertNull("Page-1 request must not send offset", requests[0].url.parameters["offset"])
    }

    @Test
    fun `negative startIndex is coerced to zero`() = runTest {
        val sut = buildDataSource { respondJson(EMPTY_RESPONSE) }

        sut.searchBooks(query = "kotlin", startIndex = -10)

        assertEquals("0", requests[0].url.parameters["offset"])
    }

    @Test
    fun `rawPageSize equals docs size and pageSize reflects resolved limit`() = runTest {
        // OL doesn't post-filter, so rawPageSize == books.size. pageSize is the
        // limit the data source actually asked for — null → 100.
        val sut = buildDataSource { respondJson(SUCCESS_RESPONSE) }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Success)
        val response = (result as Result.Success).data
        assertEquals(2, response.rawPageSize)
        assertEquals("OL data source resolves null limit to 100", 100, response.pageSize)
    }

    @Test
    fun `successful response maps numFound and docs to domain books`() = runTest {
        val sut = buildDataSource { respondJson(SUCCESS_RESPONSE) }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Success)
        val books = (result as Result.Success).data.books
        assertEquals(2, books.size)
        val first = books[0]
        // The OL mapper strips the "/works/" prefix from the key.
        assertEquals("OL1W", first.id)
        assertEquals("Kotlin in Action", first.title)
        assertEquals(BookProvider.OPEN_LIBRARY, first.provider)
        assertNotNull(first.imageUrl)
    }

    @Test
    fun `HTTP 429 maps to TOO_MANY_REQUESTS without retry`() = runTest {
        var calls = 0
        val sut = buildDataSource {
            calls++
            respond(content = "", status = HttpStatusCode.TooManyRequests)
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.TOO_MANY_REQUESTS, (result as Result.Error).error)
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
        val sut = buildDataSource {
            respond(content = "", status = HttpStatusCode.InternalServerError)
        }

        val result = sut.searchBooks(query = "kotlin")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.SERVER_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `getBookDescription unwraps a plain-string description`() = runTest {
        val sut = buildDataSource { respondJson(DETAIL_STRING_RESPONSE) }

        val result = sut.getBookDescription("OL1W", BookProvider.OPEN_LIBRARY)

        assertTrue(result is Result.Success)
        assertEquals("Plain string description.", (result as Result.Success).data)
    }

    @Test
    fun `getBookDescription unwraps an object-form description`() = runTest {
        // OL's /works endpoint sometimes returns description as a string and
        // sometimes as {"type":"...","value":"..."} — the custom serializer
        // handles both. Exercise the object form here.
        val sut = buildDataSource { respondJson(DETAIL_OBJECT_RESPONSE) }

        val result = sut.getBookDescription("OL1W", BookProvider.OPEN_LIBRARY)

        assertTrue(result is Result.Success)
        assertEquals("Object-form description.", (result as Result.Success).data)
    }

    @Test
    fun `getBookDescription maps HTTP 404 to NOT_FOUND`() = runTest {
        val sut = buildDataSource {
            respond(content = "", status = HttpStatusCode.NotFound)
        }

        val result = sut.getBookDescription("OL-missing", BookProvider.OPEN_LIBRARY)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.NOT_FOUND, (result as Result.Error).error)
    }

    companion object {
        private const val EMPTY_RESPONSE = """{"numFound":0,"docs":[]}"""

        private const val SUCCESS_RESPONSE = """
            {
              "numFound": 2,
              "docs": [
                {
                  "key": "/works/OL1W",
                  "title": "Kotlin in Action",
                  "author_name": ["Dmitry Jemerov"],
                  "cover_i": 12345,
                  "first_publish_year": 2017
                },
                {
                  "key": "/works/OL2W",
                  "title": "Effective Kotlin",
                  "author_name": ["Marcin Moskala"],
                  "cover_edition_key": "OL27W",
                  "first_publish_year": 2019
                }
              ]
            }
        """

        private const val DETAIL_STRING_RESPONSE = """
            {"description":"Plain string description."}
        """

        private const val DETAIL_OBJECT_RESPONSE = """
            {"description":{"type":"/type/text","value":"Object-form description."}}
        """
    }
}
