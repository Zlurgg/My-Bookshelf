package uk.co.zlurgg.mybookshelf.book.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Tests for the production [FallbackRemoteBookDataSource] class.
 *
 * Fallback semantics under test (narrow): the fallback fires only on errors that
 * indicate Google Books is unavailable to *us specifically* — quota
 * ([DataError.Remote.TOO_MANY_REQUESTS]), auth/restriction ([DataError.Remote.FORBIDDEN])
 * and explicit provider-unavailability ([DataError.Remote.PROVIDER_UNAVAILABLE], emitted
 * by the blank-API-key short-circuit). Transport errors that affect both providers
 * equally ([DataError.Remote.NO_INTERNET], [DataError.Remote.SERVER_ERROR],
 * [DataError.Remote.REQUEST_TIMEOUT]) surface as-is — falling back would waste a request
 * and mask the real failure.
 */
class FallbackRemoteBookDataSourceTest {

    private val primaryResults = BookSearchResponse(
        books = listOf(createTestBook("google-1", BookProvider.GOOGLE_BOOKS)),
        rawPageSize = 1,
        pageSize = 40,
    )
    private val fallbackResults = BookSearchResponse(
        books = listOf(createTestBook("ol-1", BookProvider.OPEN_LIBRARY)),
        rawPageSize = 1,
        pageSize = 100,
    )

    @Test
    fun `searchBooks returns primary results on success`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Success(primaryResults))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("google-1", (result as Result.Success).data.books[0].id)
        assertEquals(1, primary.searchCallCount)
        assertEquals(0, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks falls back on TOO_MANY_REQUESTS`() = runTest {
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("ol-1", (result as Result.Success).data.books[0].id)
        assertEquals(1, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks falls back on FORBIDDEN`() = runTest {
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.FORBIDDEN)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("ol-1", (result as Result.Success).data.books[0].id)
        assertEquals(1, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks falls back on PROVIDER_UNAVAILABLE`() = runTest {
        // PROVIDER_UNAVAILABLE is emitted by GoogleBooksRemoteBookDataSource when the
        // API key is missing/blank. Per the graceful-degradation design, OL takes over.
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.PROVIDER_UNAVAILABLE)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("ol-1", (result as Result.Success).data.books[0].id)
        assertEquals(1, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks does NOT fall back on SERVER_ERROR`() = runTest {
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.SERVER_ERROR)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.SERVER_ERROR, (result as Result.Error).error)
        assertEquals(0, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks does NOT fall back on NO_INTERNET`() = runTest {
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.NO_INTERNET)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.NO_INTERNET, (result as Result.Error).error)
        assertEquals(0, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks does NOT fall back on REQUEST_TIMEOUT`() = runTest {
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.REQUEST_TIMEOUT)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.REQUEST_TIMEOUT, (result as Result.Error).error)
        assertEquals(0, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks surfaces fallback error when fallback also fails`() = runTest {
        // Today's behavior: fallback's error surfaces (not the primary's). Pinned by test
        // so a future change is a conscious decision, not a silent regression.
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        )
        val fallback = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.SERVER_ERROR)
        )
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.SERVER_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `getBookDescription routes Google Books provider to primary`() = runTest {
        val primary = StubRemoteBookDataSource(
            descriptionResult = Result.Success("Google description")
        )
        val fallback = StubRemoteBookDataSource(
            descriptionResult = Result.Success("OL description")
        )
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.getBookDescription("book-1", BookProvider.GOOGLE_BOOKS)

        assertTrue(result is Result.Success)
        assertEquals("Google description", (result as Result.Success).data)
        assertEquals(1, primary.getDescriptionCallCount)
        assertEquals(0, fallback.getDescriptionCallCount)
    }

    @Test
    fun `getBookDescription routes OpenLibrary provider to fallback`() = runTest {
        val primary = StubRemoteBookDataSource(
            descriptionResult = Result.Success("Google description")
        )
        val fallback = StubRemoteBookDataSource(
            descriptionResult = Result.Success("OL description")
        )
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.getBookDescription("book-1", BookProvider.OPEN_LIBRARY)

        assertTrue(result is Result.Success)
        assertEquals("OL description", (result as Result.Success).data)
        assertEquals(0, primary.getDescriptionCallCount)
        assertEquals(1, fallback.getDescriptionCallCount)
    }

    @Test
    fun `searchBooks short-circuits to primary when startIndex non-null even on fallback error`() = runTest {
        // C1 invariant: provider-switch mid-pagination would interleave Google
        // rows 0..N with OL rows from offset=N+1 of a different result set. The
        // user sees the primary's error inline and can retry/refine.
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test", startIndex = 40)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.TOO_MANY_REQUESTS, (result as Result.Error).error)
        assertEquals("Fallback must not be invoked mid-pagination", 0, fallback.searchCallCount)
        assertEquals("Primary still gets the startIndex", 40, primary.lastSearchStartIndex)
    }

    @Test
    fun `searchBooks still falls back when startIndex is null (page 1 unchanged)`() = runTest {
        // Regression guard: the startIndex short-circuit must not affect the
        // existing page-1 fallback behavior — that's how OL takes over when
        // Google 429s on a fresh search.
        val primary = StubRemoteBookDataSource(
            searchResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        )
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.searchBooks("test", startIndex = null)

        assertTrue(result is Result.Success)
        assertEquals("ol-1", (result as Result.Success).data.books[0].id)
        assertEquals(1, fallback.searchCallCount)
    }

    @Test
    fun `getBookDescription does NOT fall through providers on primary failure`() = runTest {
        // Description fetch routes by provider only — it does not retry on the other source.
        val primary = StubRemoteBookDataSource(
            descriptionResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        )
        val fallback = StubRemoteBookDataSource(
            descriptionResult = Result.Success("OL description")
        )
        val sut = FallbackRemoteBookDataSource(primary = primary, fallback = fallback)

        val result = sut.getBookDescription("book-1", BookProvider.GOOGLE_BOOKS)

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.TOO_MANY_REQUESTS, (result as Result.Error).error)
        assertEquals(0, fallback.getDescriptionCallCount)
    }

    // === Helpers ===

    private fun createTestBook(id: String, provider: BookProvider) = Book(
        id = id,
        title = "Test",
        authors = emptyList(),
        imageUrl = "",
        description = null,
        languages = emptyList(),
        firstPublishYear = null,
        numPages = null,
        purchased = false,
        spineColor = 0,
        provider = provider,
    )

    /**
     * Stub that implements RemoteBookDataSource for testing fallback logic.
     */
    class StubRemoteBookDataSource(
        private val searchResult: Result<BookSearchResponse, DataError.Remote> =
            Result.Success(BookSearchResponse(emptyList(), rawPageSize = 0, pageSize = 40)),
        private val descriptionResult: Result<String?, DataError.Remote> =
            Result.Success(null),
    ) : RemoteBookDataSource {

        var searchCallCount = 0
        var lastSearchStartIndex: Int? = null
        var getDescriptionCallCount = 0

        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            subjectFilter: String?,
            sort: String?,
            startIndex: Int?,
        ): Result<BookSearchResponse, DataError.Remote> {
            searchCallCount++
            lastSearchStartIndex = startIndex
            return searchResult
        }

        override suspend fun getBookDescription(
            bookId: String,
            provider: BookProvider
        ): Result<String?, DataError.Remote> {
            getDescriptionCallCount++
            return descriptionResult
        }
    }
}
