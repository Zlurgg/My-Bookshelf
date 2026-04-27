package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestSearchedBookDtoBuilder

class MockRemoteBookDataSource : RemoteBookDataSource {

    var shouldThrowException = false
    var returnEmptyResults = false
    var networkError: DataError.Remote = DataError.Remote.REQUEST_TIMEOUT
    var searchBooksCallCount = 0
    var lastSearchQuery: String? = null
    var lastSearchParams: SearchParams? = null

    private var configuredSearchResponse: SearchResponseDto? = null

    data class SearchParams(
        val query: String,
        val resultLimit: Int?,
        val language: String?,
        val authorFilter: String?,
        val titleFilter: String?,
        val sort: String?
    )

    fun configureSearchResponse(response: SearchResponseDto) {
        configuredSearchResponse = response
    }

    fun configureSearchResults(count: Int) {
        val results = (1..count).map { index ->
            TestSearchedBookDtoBuilder()
                .withId("/works/OL${index}W")
                .withTitle("Test Book $index")
                .build()
        }
        configuredSearchResponse = SearchResponseDto(numFound = count, results = results)
    }

    fun reset() {
        shouldThrowException = false
        returnEmptyResults = false
        searchBooksCallCount = 0
        lastSearchQuery = null
        lastSearchParams = null
        configuredSearchResponse = null
        configuredBookDetails = null
        getBookDetailsCallCount = 0
        lastBookWorkId = null
        networkError = DataError.Remote.REQUEST_TIMEOUT
    }

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        sort: String?
    ): Result<SearchResponseDto, DataError.Remote> {
        searchBooksCallCount++
        lastSearchQuery = query
        lastSearchParams = SearchParams(query, resultLimit, language, authorFilter, titleFilter, sort)

        return when {
            shouldThrowException -> Result.Error(networkError)
            returnEmptyResults -> Result.Success(SearchResponseDto(numFound = 0, results = emptyList()))
            configuredSearchResponse != null -> Result.Success(configuredSearchResponse!!)
            else -> {
                // Default response with some test data
                val defaultResults = listOf(
                    TestSearchedBookDtoBuilder.withAllFields(),
                    TestSearchedBookDtoBuilder.withMinimalFields()
                )
                Result.Success(SearchResponseDto(numFound = defaultResults.size, results = defaultResults))
            }
        }
    }

    private var configuredBookDetails: BookWorkDto? = null
    var getBookDetailsCallCount = 0
    var lastBookWorkId: String? = null

    fun configureBookDetailsResponse(bookDetails: BookWorkDto) {
        configuredBookDetails = bookDetails
    }

    override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
        getBookDetailsCallCount++
        lastBookWorkId = bookWorkId

        return when {
            shouldThrowException -> Result.Error(networkError)
            configuredBookDetails != null -> Result.Success(configuredBookDetails!!)
            else -> Result.Success(BookWorkDto(description = "Default test description"))
        }
    }
}
