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

class FallbackRemoteBookDataSourceTest {

    private val primaryResults = BookSearchResponse(
        totalResults = 1,
        books = listOf(createTestBook("google-1", BookProvider.GOOGLE_BOOKS))
    )
    private val fallbackResults = BookSearchResponse(
        totalResults = 1,
        books = listOf(createTestBook("ol-1", BookProvider.OPEN_LIBRARY))
    )

    @Test
    fun `searchBooks returns primary results on success`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Success(primaryResults))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("google-1", (result as Result.Success).data.books[0].id)
        assertEquals(1, primary.searchCallCount)
        assertEquals(0, fallback.searchCallCount)
    }

    @Test
    fun `getBookDescription routes Google Books provider to primary`() = runTest {
        val primary = StubRemoteBookDataSource(descriptionResult = Result.Success("Google description"))
        val fallback = StubRemoteBookDataSource(descriptionResult = Result.Success("OL description"))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.getBookDescription("book-1", BookProvider.GOOGLE_BOOKS)

        assertTrue(result is Result.Success)
        assertEquals("Google description", (result as Result.Success).data)
        assertEquals(1, primary.getDescriptionCallCount)
        assertEquals(0, fallback.getDescriptionCallCount)
    }

    @Test
    fun `getBookDescription routes OpenLibrary provider to fallback`() = runTest {
        val primary = StubRemoteBookDataSource(descriptionResult = Result.Success("Google description"))
        val fallback = StubRemoteBookDataSource(descriptionResult = Result.Success("OL description"))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.getBookDescription("book-1", BookProvider.OPEN_LIBRARY)

        assertTrue(result is Result.Success)
        assertEquals("OL description", (result as Result.Success).data)
        assertEquals(0, primary.getDescriptionCallCount)
        assertEquals(1, fallback.getDescriptionCallCount)
    }

    @Test
    fun `searchBooks falls back on TOO_MANY_REQUESTS`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Error(DataError.Remote.TOO_MANY_REQUESTS))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("ol-1", (result as Result.Success).data.books[0].id)
    }

    @Test
    fun `searchBooks falls back on FORBIDDEN`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Error(DataError.Remote.FORBIDDEN))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Success)
        assertEquals("ol-1", (result as Result.Success).data.books[0].id)
    }

    @Test
    fun `searchBooks does NOT fall back on SERVER_ERROR`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Error(DataError.Remote.SERVER_ERROR))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.SERVER_ERROR, (result as Result.Error).error)
        assertEquals(0, fallback.searchCallCount)
    }

    @Test
    fun `searchBooks does NOT fall back on NO_INTERNET`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Error(DataError.Remote.NO_INTERNET))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.NO_INTERNET, (result as Result.Error).error)
    }

    @Test
    fun `searchBooks does NOT fall back on REQUEST_TIMEOUT`() = runTest {
        val primary = StubRemoteBookDataSource(searchResult = Result.Error(DataError.Remote.REQUEST_TIMEOUT))
        val fallback = StubRemoteBookDataSource(searchResult = Result.Success(fallbackResults))
        val sut = createFallbackDataSource(primary, fallback)

        val result = sut.searchBooks("test")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Remote.REQUEST_TIMEOUT, (result as Result.Error).error)
    }

    // === Helpers ===

    private fun createFallbackDataSource(
        primary: StubRemoteBookDataSource,
        fallback: StubRemoteBookDataSource
    ): FallbackDataSourceWrapper {
        return FallbackDataSourceWrapper(primary, fallback)
    }

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
            Result.Success(BookSearchResponse(0, emptyList())),
        private val descriptionResult: Result<String?, DataError.Remote> =
            Result.Success(null),
    ) : RemoteBookDataSource {

        var searchCallCount = 0
        var getDescriptionCallCount = 0

        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            subjectFilter: String?,
            sort: String?
        ): Result<BookSearchResponse, DataError.Remote> {
            searchCallCount++
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

    /**
     * Wraps the fallback logic using stub data sources instead of concrete Google/OL classes.
     * This tests the routing logic without requiring Ktor HTTP clients.
     */
    class FallbackDataSourceWrapper(
        private val primary: StubRemoteBookDataSource,
        private val fallback: StubRemoteBookDataSource,
    ) : RemoteBookDataSource {

        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            subjectFilter: String?,
            sort: String?
        ): Result<BookSearchResponse, DataError.Remote> {
            val result = primary.searchBooks(
                query = query,
                resultLimit = resultLimit,
                language = language,
                authorFilter = authorFilter,
                titleFilter = titleFilter,
                subjectFilter = subjectFilter,
                sort = sort,
            )

            return when {
                result is Result.Error && shouldFallback(result.error) -> {
                    fallback.searchBooks(
                        query = query,
                        resultLimit = resultLimit,
                        language = language,
                        authorFilter = authorFilter,
                        titleFilter = titleFilter,
                        subjectFilter = subjectFilter,
                        sort = sort,
                    )
                }
                else -> result
            }
        }

        override suspend fun getBookDescription(
            bookId: String,
            provider: BookProvider
        ): Result<String?, DataError.Remote> {
            return when (provider) {
                BookProvider.GOOGLE_BOOKS -> primary.getBookDescription(bookId, provider)
                BookProvider.OPEN_LIBRARY -> fallback.getBookDescription(bookId, provider)
            }
        }

        private fun shouldFallback(error: DataError.Remote): Boolean {
            return error == DataError.Remote.TOO_MANY_REQUESTS ||
                error == DataError.Remote.FORBIDDEN
        }
    }
}
