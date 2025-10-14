package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestSearchedBookDtoBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRemoteBookDataSource

class SearchBooksUseCaseTest {

    private val mockRemoteDataSource = MockRemoteBookDataSource()
    private val useCase = SearchBooksUseCaseImpl(mockRemoteDataSource)

    @After
    fun tearDown() {
        mockRemoteDataSource.reset()
    }

    @Test
    fun `execute calls remote data source with correct parameters`() = runTest {
        // Given
        val query = "kotlin programming"
        val language = "eng"
        val authorFilter = "Joshua Bloch"
        val titleFilter = "Effective"

        // When
        val result = useCase.execute(
            query = query,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        assertEquals("Should call remote data source once", 1, mockRemoteDataSource.searchBooksCallCount)
        assertEquals("Should pass correct query", query, mockRemoteDataSource.lastSearchQuery)

        val params = mockRemoteDataSource.lastSearchParams!!
        assertEquals("Should pass correct language", language, params.language)
        assertEquals("Should pass correct author filter", authorFilter, params.authorFilter)
        assertEquals("Should pass correct title filter", titleFilter, params.titleFilter)
        assertEquals("Should always use API's default relevance sorting", null, params.sort)
    }

    @Test
    fun `execute returns mapped books when successful`() = runTest {
        // Given
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Test Book 1")
                .withAuthorNames(listOf("Author 1"))
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Test Book 2")
                .withAuthorNames(listOf("Author 2"))
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(SearchResponseDto(testBooks))

        // When
        val result = useCase.execute("test query")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val books = (result as Result.Success).data
        assertEquals("Should return correct number of books", 2, books.size)
        assertEquals("Should map first book correctly", "OL1W", books[0].id)
        assertEquals("Should map first book title", "Test Book 1", books[0].title)
        assertEquals("Should map second book correctly", "OL2W", books[1].id)
        assertEquals("Should map second book title", "Test Book 2", books[1].title)
    }

    @Test
    fun `execute returns empty list when no results found`() = runTest {
        // Given
        mockRemoteDataSource.returnEmptyResults = true

        // When
        val result = useCase.execute("nonexistent book")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val books = (result as Result.Success).data
        assertTrue("Should return empty list", books.isEmpty())
    }

    @Test
    fun `execute returns error when network fails`() = runTest {
        // Given
        mockRemoteDataSource.shouldThrowException = true
        mockRemoteDataSource.networkError = DataError.Remote.NO_INTERNET

        // When
        val result = useCase.execute("test query")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return correct error type", DataError.Remote.NO_INTERNET, error)
    }

    @Test
    fun `execute handles request timeout error`() = runTest {
        // Given
        mockRemoteDataSource.shouldThrowException = true
        mockRemoteDataSource.networkError = DataError.Remote.REQUEST_TIMEOUT

        // When
        val result = useCase.execute("test query")

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return timeout error", DataError.Remote.REQUEST_TIMEOUT, error)
    }

    @Test
    fun `execute rejects query exceeding 200 characters`() = runTest {
        // Given - Query with 201 characters
        val longQuery = "a".repeat(201)

        // When
        val result = useCase.execute(query = longQuery)

        // Then
        assertTrue("Should return error for long query", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return validation error", DataError.Remote.MALFORMED_REQUEST, error)
        assertEquals("Should not call remote data source", 0, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute accepts query at exactly 200 characters`() = runTest {
        // Given - Query with exactly 200 characters
        val maxQuery = "a".repeat(200)

        // When
        val result = useCase.execute(query = maxQuery)

        // Then
        assertTrue("Should return success for max length query", result is Result.Success)
        assertEquals("Should call remote data source", 1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects author filter exceeding 100 characters`() = runTest {
        // Given - Author filter with 101 characters
        val longAuthor = "a".repeat(101)

        // When
        val result = useCase.execute(
            query = "test",
            authorFilter = longAuthor
        )

        // Then
        assertTrue("Should return error for long author filter", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return validation error", DataError.Remote.MALFORMED_REQUEST, error)
        assertEquals("Should not call remote data source", 0, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute accepts author filter at exactly 100 characters`() = runTest {
        // Given - Author filter with exactly 100 characters
        val maxAuthor = "a".repeat(100)

        // When
        val result = useCase.execute(
            query = "test",
            authorFilter = maxAuthor
        )

        // Then
        assertTrue("Should return success for max length author", result is Result.Success)
        assertEquals("Should call remote data source", 1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects title filter exceeding 200 characters`() = runTest {
        // Given - Title filter with 201 characters
        val longTitle = "a".repeat(201)

        // When
        val result = useCase.execute(
            query = "test",
            titleFilter = longTitle
        )

        // Then
        assertTrue("Should return error for long title filter", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return validation error", DataError.Remote.MALFORMED_REQUEST, error)
        assertEquals("Should not call remote data source", 0, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute accepts title filter at exactly 200 characters`() = runTest {
        // Given - Title filter with exactly 200 characters
        val maxTitle = "a".repeat(200)

        // When
        val result = useCase.execute(
            query = "test",
            titleFilter = maxTitle
        )

        // Then
        assertTrue("Should return success for max length title", result is Result.Success)
        assertEquals("Should call remote data source", 1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects when multiple filters exceed limits`() = runTest {
        // Given - Both query and filters exceeding limits
        val longQuery = "a".repeat(201)
        val longAuthor = "b".repeat(101)
        val longTitle = "c".repeat(201)

        // When
        val result = useCase.execute(
            query = longQuery,
            authorFilter = longAuthor,
            titleFilter = longTitle
        )

        // Then
        assertTrue("Should return error when any field exceeds limit", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals("Should return validation error", DataError.Remote.MALFORMED_REQUEST, error)
        assertEquals("Should not call remote data source", 0, mockRemoteDataSource.searchBooksCallCount)
    }
}