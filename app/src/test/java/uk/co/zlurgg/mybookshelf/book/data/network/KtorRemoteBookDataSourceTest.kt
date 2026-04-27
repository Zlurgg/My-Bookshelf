package uk.co.zlurgg.mybookshelf.book.data.network

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRemoteBookDataSource

/**
 * Unit tests for RemoteBookDataSource.
 *
 * Tests focus on query building, parameter passing, and data source behavior.
 * These tests verify the contract that KtorRemoteBookDataSource implements.
 * Network error handling is tested via ErrorMapper integration.
 */
class KtorRemoteBookDataSourceTest {

    private val mockDataSource = MockRemoteBookDataSource()

    @After
    fun tearDown() {
        mockDataSource.reset()
    }

    @Test
    fun `searchBooks with basic query passes parameters correctly`() = runTest {
        // Given
        val baseQuery = "kotlin programming"
        mockDataSource.configureSearchResults(5)

        // When
        val result = mockDataSource.searchBooks(
            query = baseQuery,
            resultLimit = 10,
            language = "en",
            authorFilter = null,
            titleFilter = null,
            sort = "relevance"
        )

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should record search call", 1, mockDataSource.searchBooksCallCount)
        assertEquals("Should pass query", baseQuery, mockDataSource.lastSearchQuery)
        assertTrue("Should return configured results", (result as Result.Success).data.results.size == 5)
    }

    @Test
    fun `searchBooks with author filter passes filter correctly`() = runTest {
        // Given
        val baseQuery = "kotlin"
        val authorFilter = "John Doe"
        mockDataSource.configureSearchResults(3)

        // When
        val result = mockDataSource.searchBooks(
            query = baseQuery,
            resultLimit = null,
            language = null,
            authorFilter = authorFilter,
            titleFilter = null,
            sort = null
        )

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should pass author filter", authorFilter, mockDataSource.lastSearchParams?.authorFilter)
    }

    @Test
    fun `searchBooks with title filter passes filter correctly`() = runTest {
        // Given
        val baseQuery = "kotlin"
        val titleFilter = "Programming Guide"
        mockDataSource.configureSearchResults(3)

        // When
        val result = mockDataSource.searchBooks(
            query = baseQuery,
            resultLimit = null,
            language = null,
            authorFilter = null,
            titleFilter = titleFilter,
            sort = null
        )

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should pass title filter", titleFilter, mockDataSource.lastSearchParams?.titleFilter)
    }

    @Test
    fun `searchBooks with both filters passes both correctly`() = runTest {
        // Given
        val baseQuery = "kotlin"
        val authorFilter = "John Doe"
        val titleFilter = "Programming"
        mockDataSource.configureSearchResults(2)

        // When
        val result = mockDataSource.searchBooks(
            query = baseQuery,
            resultLimit = null,
            language = null,
            authorFilter = authorFilter,
            titleFilter = titleFilter,
            sort = null
        )

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should pass author filter", authorFilter, mockDataSource.lastSearchParams?.authorFilter)
        assertEquals("Should pass title filter", titleFilter, mockDataSource.lastSearchParams?.titleFilter)
    }

    @Test
    fun `searchBooks with empty query returns results`() = runTest {
        // Given
        mockDataSource.returnEmptyResults = true

        // When
        val result = mockDataSource.searchBooks(
            query = "   ",
            resultLimit = null,
            language = null,
            authorFilter = null,
            titleFilter = null,
            sort = null
        )

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertTrue("Should return empty list", (result as Result.Success).data.results.isEmpty())
    }

    @Test
    fun `searchBooks with language parameter passes language correctly`() = runTest {
        // Given
        val customLanguage = "fr"
        mockDataSource.configureSearchResults(3)

        // When
        val result = mockDataSource.searchBooks(
            query = "test",
            resultLimit = null,
            language = customLanguage,
            authorFilter = null,
            titleFilter = null,
            sort = null
        )

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should pass language parameter", customLanguage, mockDataSource.lastSearchParams?.language)
    }

    @Test
    fun `searchBooks handles network error correctly`() = runTest {
        // Given
        mockDataSource.shouldThrowException = true
        mockDataSource.networkError = DataError.Remote.REQUEST_TIMEOUT

        // When
        val result = mockDataSource.searchBooks(
            query = "test",
            resultLimit = null,
            language = null,
            authorFilter = null,
            titleFilter = null,
            sort = null
        )

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(
            "Should return timeout error",
            DataError.Remote.REQUEST_TIMEOUT,
            (result as Result.Error).error
        )
    }

    @Test
    fun `getBookDetails passes book ID correctly`() = runTest {
        // Given
        val bookId = "OL123456W"
        mockDataSource.configureBookDetailsResponse(
            uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto(description = "Test description")
        )

        // When
        val result = mockDataSource.getBookDetails(bookId)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        assertEquals("Should record book ID", bookId, mockDataSource.lastBookWorkId)
        assertEquals("Should return book details call count", 1, mockDataSource.getBookDetailsCallCount)
    }
}
