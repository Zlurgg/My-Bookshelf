package uk.co.zlurgg.mybookshelf.book.data.network

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRemoteBookDataSource

/**
 * Unit tests for RemoteBookDataSource contract.
 *
 * Tests focus on parameter passing and data source behavior.
 */
class RemoteBookDataSourceTest {

    private val mockDataSource = MockRemoteBookDataSource()

    @After
    fun tearDown() {
        mockDataSource.reset()
    }

    @Test
    fun `searchBooks with basic query passes parameters correctly`() = runTest {
        val baseQuery = "kotlin programming"
        mockDataSource.configureSearchResults(5)

        val result = mockDataSource.searchBooks(
            query = baseQuery,
            resultLimit = 10,
            language = "en",
            authorFilter = null,
            titleFilter = null,
            sort = "relevance"
        )

        assertTrue("Should succeed", result is Result.Success)
        assertEquals(1, mockDataSource.searchBooksCallCount)
        assertEquals(baseQuery, mockDataSource.lastSearchQuery)
        assertEquals(5, (result as Result.Success).data.books.size)
    }

    @Test
    fun `searchBooks with author filter passes filter correctly`() = runTest {
        mockDataSource.configureSearchResults(3)

        mockDataSource.searchBooks(
            query = "kotlin",
            authorFilter = "John Doe",
        )

        assertEquals("John Doe", mockDataSource.lastSearchParams?.authorFilter)
    }

    @Test
    fun `searchBooks with title filter passes filter correctly`() = runTest {
        mockDataSource.configureSearchResults(3)

        mockDataSource.searchBooks(
            query = "kotlin",
            titleFilter = "Programming Guide",
        )

        assertEquals("Programming Guide", mockDataSource.lastSearchParams?.titleFilter)
    }

    @Test
    fun `searchBooks handles network error correctly`() = runTest {
        mockDataSource.shouldThrowException = true
        mockDataSource.networkError = DataError.Remote.REQUEST_TIMEOUT

        val result = mockDataSource.searchBooks(query = "test")

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.REQUEST_TIMEOUT, (result as Result.Error).error)
    }

    @Test
    fun `getBookDescription passes book ID correctly`() = runTest {
        mockDataSource.configureBookDescription("Test description")

        val result = mockDataSource.getBookDescription("OL123456W", BookProvider.OPEN_LIBRARY)

        assertTrue("Should succeed", result is Result.Success)
        assertEquals("OL123456W", mockDataSource.lastBookId)
        assertEquals(1, mockDataSource.getBookDescriptionCallCount)
        assertEquals("Test description", (result as Result.Success).data)
    }
}
