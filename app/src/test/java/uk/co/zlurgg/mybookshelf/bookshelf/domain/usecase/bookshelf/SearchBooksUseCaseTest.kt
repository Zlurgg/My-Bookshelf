package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestSearchedBookDtoBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockRemoteBookDataSource

class SearchBooksUseCaseTest {

    private val mockRemoteDataSource = MockRemoteBookDataSource()
    private val bookSorter = BookSorter()
    private val useCase = SearchBooksUseCaseImpl(mockRemoteDataSource, bookSorter)

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
            sortBy = BookSearchSort.BEST_MATCH,
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
        assertEquals("Should not pass server sort for BEST_MATCH", null, params.sort)
    }

    @Test
    fun `execute with server-side sort passes correct sort parameter`() = runTest {
        // Given
        val query = "science fiction"

        // When
        val result = useCase.execute(
            query = query,
            sortBy = BookSearchSort.NEWEST
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val params = mockRemoteDataSource.lastSearchParams!!
        assertEquals("Should pass server sort parameter", "new", params.sort)
    }

    @Test
    fun `execute with OLDEST sort passes correct server parameter`() = runTest {
        // Given
        val query = "historical fiction"

        // When
        val result = useCase.execute(
            query = query,
            sortBy = BookSearchSort.OLDEST
        )

        // Then
        assertTrue("Should return success", result is Result.Success)
        val params = mockRemoteDataSource.lastSearchParams!!
        assertEquals("Should pass old sort parameter", "old", params.sort)
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
    fun `execute with client-side sorting applies BookSorter for BEST_MATCH`() = runTest {
        // Given - Books that should be reordered by best match algorithm
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Unrelated Title")
                .withAuthorNames(listOf("Random Author"))
                .withRatingsAverage(3.0)
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Kotlin Programming Guide") // Should match better
                .withAuthorNames(listOf("Expert Author"))
                .withRatingsAverage(4.5)
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(SearchResponseDto(testBooks))

        // When
        val result = useCase.execute("kotlin", sortBy = BookSearchSort.BEST_MATCH)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val books = (result as Result.Success).data
        assertEquals("Should return same number of books", 2, books.size)
        // The BookSorter should have reordered these, with better title match first
        assertEquals("Should have Kotlin book first after sorting", "OL2W", books[0].id)
    }

    @Test
    fun `execute with client-side sorting applies BookSorter for HIGHEST_RATED`() = runTest {
        // Given - Books with different ratings
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Lower Rated Book")
                .withRatingsAverage(3.0)
                .withRatingsCount(100)
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Higher Rated Book")
                .withRatingsAverage(4.8)
                .withRatingsCount(200)
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(SearchResponseDto(testBooks))

        // When
        val result = useCase.execute("books", sortBy = BookSearchSort.HIGHEST_RATED)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val books = (result as Result.Success).data
        assertEquals("Should return same number of books", 2, books.size)
        // BookSorter should put higher rated book first
        assertEquals("Should have higher rated book first", "OL2W", books[0].id)
        assertEquals("Higher rated book should have correct rating", 4.8, books[0].averageRating!!, 0.0)
    }

    @Test
    fun `execute with server-side sorting does not apply client sorting`() = runTest {
        // Given - Server will return pre-sorted results for NEWEST
        val testBooks = listOf(
            TestSearchedBookDtoBuilder()
                .withId("/works/OL1W")
                .withTitle("Book from 2023")
                .withFirstPublishYear(2023)
                .build(),
            TestSearchedBookDtoBuilder()
                .withId("/works/OL2W")
                .withTitle("Book from 2020")
                .withFirstPublishYear(2020)
                .build()
        )
        mockRemoteDataSource.configureSearchResponse(SearchResponseDto(testBooks))

        // When
        val result = useCase.execute("books", sortBy = BookSearchSort.NEWEST)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val books = (result as Result.Success).data
        // Should preserve server order (no client-side re-sorting)
        assertEquals("Should maintain server order", "OL1W", books[0].id)
        assertEquals("Should maintain server order", "OL2W", books[1].id)
    }
}