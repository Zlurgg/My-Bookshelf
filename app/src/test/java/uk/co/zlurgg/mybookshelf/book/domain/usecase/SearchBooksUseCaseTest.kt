package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
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
        val query = "kotlin programming"
        val language = "eng"
        val authorFilter = "Joshua Bloch"
        val titleFilter = "Effective"

        val result = useCase(
            query = query,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter
        )

        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockRemoteDataSource.searchBooksCallCount)
        assertEquals(query, mockRemoteDataSource.lastSearchQuery)

        val params = mockRemoteDataSource.lastSearchParams!!
        assertEquals(language, params.language)
        assertEquals(authorFilter, params.authorFilter)
        assertEquals(titleFilter, params.titleFilter)
        assertEquals("Should always use API's default relevance sorting", null, params.sort)
    }

    @Test
    fun `execute returns mapped books when successful`() = runTest {
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Test Book 1")
                .withAuthorNames(listOf("Author 1"))
                .build()
                .toBook(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Test Book 2")
                .withAuthorNames(listOf("Author 2"))
                .build()
                .toBook()
        )
        mockRemoteDataSource.configureSearchResponse(
            BookSearchResponse(totalResults = testBooks.size, books = testBooks)
        )

        val result = useCase("test query")

        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertEquals(2, searchResult.books.size)
        assertEquals("OL1W", searchResult.books[0].id)
        assertEquals("Test Book 1", searchResult.books[0].title)
        assertEquals("OL2W", searchResult.books[1].id)
        assertEquals("Test Book 2", searchResult.books[1].title)
    }

    @Test
    fun `execute returns empty list when no results found`() = runTest {
        mockRemoteDataSource.returnEmptyResults = true

        val result = useCase("nonexistent book")

        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertTrue("Should return empty list", searchResult.books.isEmpty())
    }

    @Test
    fun `execute returns error when network fails`() = runTest {
        mockRemoteDataSource.shouldThrowException = true
        mockRemoteDataSource.networkError = DataError.Remote.NO_INTERNET

        val result = useCase("test query")

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.NO_INTERNET, (result as Result.Error).error)
    }

    @Test
    fun `execute handles request timeout error`() = runTest {
        mockRemoteDataSource.shouldThrowException = true
        mockRemoteDataSource.networkError = DataError.Remote.REQUEST_TIMEOUT

        val result = useCase("test query")

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.REQUEST_TIMEOUT, (result as Result.Error).error)
    }

    @Test
    fun `execute rejects query exceeding 200 characters`() = runTest {
        val result = useCase(query = "a".repeat(201))

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.MALFORMED_REQUEST, (result as Result.Error).error)
        assertEquals(0, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute accepts query at exactly 200 characters`() = runTest {
        val result = useCase(query = "a".repeat(200))

        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects author filter exceeding 100 characters`() = runTest {
        val result = useCase(query = "test", authorFilter = "a".repeat(101))

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.MALFORMED_REQUEST, (result as Result.Error).error)
        assertEquals(0, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute accepts author filter at exactly 100 characters`() = runTest {
        val result = useCase(query = "test", authorFilter = "a".repeat(100))

        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects title filter exceeding 200 characters`() = runTest {
        val result = useCase(query = "test", titleFilter = "a".repeat(201))

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.MALFORMED_REQUEST, (result as Result.Error).error)
        assertEquals(0, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute accepts title filter at exactly 200 characters`() = runTest {
        val result = useCase(query = "test", titleFilter = "a".repeat(200))

        assertTrue("Should return success", result is Result.Success)
        assertEquals(1, mockRemoteDataSource.searchBooksCallCount)
    }

    @Test
    fun `execute rejects when multiple filters exceed limits`() = runTest {
        val result = useCase(
            query = "a".repeat(201),
            authorFilter = "b".repeat(101),
            titleFilter = "c".repeat(201)
        )

        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Remote.MALFORMED_REQUEST, (result as Result.Error).error)
        assertEquals(0, mockRemoteDataSource.searchBooksCallCount)
    }

    // Safe search integration tests

    @Test
    fun `safe search ON filters explicit books and returns correct filteredCount`() = runTest {
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Science Fiction")
                .withSubjects(listOf("Fiction", "Science"))
                .build().toBook(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Explicit Book")
                .withSubjects(listOf("Erotica", "Fiction"))
                .build().toBook(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL3W")
                .withTitle("History Book")
                .withSubjects(listOf("History"))
                .build().toBook()
        )
        mockRemoteDataSource.configureSearchResponse(
            BookSearchResponse(totalResults = testBooks.size, books = testBooks)
        )

        val result = useCase("test", safeSearchEnabled = true)

        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertEquals(2, searchResult.books.size)
        assertEquals(1, searchResult.filteredCount)
        assertEquals("OL1W", searchResult.books[0].id)
        assertEquals("OL3W", searchResult.books[1].id)
    }

    @Test
    fun `safe search OFF returns all books with zero filteredCount`() = runTest {
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Safe Book")
                .withSubjects(listOf("Fiction"))
                .build().toBook(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Explicit Book")
                .withSubjects(listOf("Erotica"))
                .build().toBook()
        )
        mockRemoteDataSource.configureSearchResponse(
            BookSearchResponse(totalResults = testBooks.size, books = testBooks)
        )

        val result = useCase("test", safeSearchEnabled = false)

        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertEquals(2, searchResult.books.size)
        assertEquals(0, searchResult.filteredCount)
    }

    @Test
    fun `safe search ON with all explicit results returns empty with correct count`() = runTest {
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Explicit 1")
                .withSubjects(listOf("Erotica"))
                .build().toBook(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Explicit 2")
                .withSubjects(listOf("Pornography"))
                .build().toBook()
        )
        mockRemoteDataSource.configureSearchResponse(
            BookSearchResponse(totalResults = testBooks.size, books = testBooks)
        )

        val result = useCase("test", safeSearchEnabled = true)

        assertTrue("Should return success", result is Result.Success)
        val searchResult = (result as Result.Success).data
        assertTrue("Should return empty list", searchResult.books.isEmpty())
        assertEquals(2, searchResult.filteredCount)
    }
}
