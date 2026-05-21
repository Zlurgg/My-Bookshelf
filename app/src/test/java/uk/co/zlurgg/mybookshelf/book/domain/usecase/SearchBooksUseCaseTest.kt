package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
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
        val result = useCase(
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
        mockRemoteDataSource.configureSearchResponse(SearchResponseDto(numFound = testBooks.size, results = testBooks))

        // When
        val result = useCase("test query")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertEquals("Should return correct number of books", 2, searchResult.books.size)
        assertEquals("Should map first book correctly", "OL1W", searchResult.books[0].id)
        assertEquals("Should map first book title", "Test Book 1", searchResult.books[0].title)
        assertEquals("Should map second book correctly", "OL2W", searchResult.books[1].id)
        assertEquals("Should map second book title", "Test Book 2", searchResult.books[1].title)
    }

    @Test
    fun `execute returns empty list when no results found`() = runTest {
        // Given
        mockRemoteDataSource.returnEmptyResults = true

        // When
        val result = useCase("nonexistent book")

        // Then
        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertTrue("Should return empty list", searchResult.books.isEmpty())
    }

    @Test
    fun `execute returns error when network fails`() = runTest {
        // Given
        mockRemoteDataSource.shouldThrowException = true
        mockRemoteDataSource.networkError = DataError.Remote.NO_INTERNET

        // When
        val result = useCase("test query")

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
        val result = useCase("test query")

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
        val result = useCase(query = longQuery)

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
        val result = useCase(query = maxQuery)

        // Then
        assertTrue("Should return success for max length query", result is Result.Success)
        assertEquals("Should call remote data source", 1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects author filter exceeding 100 characters`() = runTest {
        // Given - Author filter with 101 characters
        val longAuthor = "a".repeat(101)

        // When
        val result = useCase(
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
        val result = useCase(
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
        val result = useCase(
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
        val result = useCase(
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
        val result = useCase(
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

    // Safe search integration tests

    @Test
    fun `safe search ON filters explicit books and returns correct filteredCount`() = runTest {
        // Given - mix of safe and explicit books
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Science Fiction")
                .withSubjects(listOf("Fiction", "Science"))
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Explicit Book")
                .withSubjects(listOf("Erotica", "Fiction"))
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL3W")
                .withTitle("History Book")
                .withSubjects(listOf("History"))
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(
            SearchResponseDto(numFound = testBooks.size, results = testBooks)
        )

        // When
        val result = useCase("test", safeSearchEnabled = true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertEquals("Should filter to 2 safe books", 2, searchResult.books.size)
        assertEquals("Should report 1 filtered", 1, searchResult.filteredCount)
        assertEquals("First result should be safe book", "OL1W", searchResult.books[0].id)
        assertEquals("Second result should be safe book", "OL3W", searchResult.books[1].id)
    }

    @Test
    fun `safe search OFF returns all books with zero filteredCount`() = runTest {
        // Given
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Safe Book")
                .withSubjects(listOf("Fiction"))
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Explicit Book")
                .withSubjects(listOf("Erotica"))
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(
            SearchResponseDto(numFound = testBooks.size, results = testBooks)
        )

        // When
        val result = useCase("test", safeSearchEnabled = false)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertEquals("Should return all books", 2, searchResult.books.size)
        assertEquals("Should report zero filtered", 0, searchResult.filteredCount)
    }

    @Test
    fun `safe search ON with all explicit results returns empty with correct count`() = runTest {
        // Given - all books are explicit
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Explicit 1")
                .withSubjects(listOf("Erotica"))
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Explicit 2")
                .withSubjects(listOf("Pornography"))
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(
            SearchResponseDto(numFound = testBooks.size, results = testBooks)
        )

        // When
        val result = useCase("test", safeSearchEnabled = true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertTrue("Should return empty list", searchResult.books.isEmpty())
        assertEquals("Should report all filtered", 2, searchResult.filteredCount)
    }
}
