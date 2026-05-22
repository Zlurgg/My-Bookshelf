package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
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

    private var configuredSearchResponse: BookSearchResponse? = null

    data class SearchParams(
        val query: String,
        val resultLimit: Int?,
        val language: String?,
        val authorFilter: String?,
        val titleFilter: String?,
        val subjectFilter: String?,
        val sort: String?
    )

    fun configureSearchResponse(response: BookSearchResponse) {
        configuredSearchResponse = response
    }

    fun configureSearchResults(count: Int) {
        val books = (1..count).map { index ->
            TestSearchedBookDtoBuilder()
                .withId("/works/OL${index}W")
                .withTitle("Test Book $index")
                .build()
                .toBook()
        }
        configuredSearchResponse = BookSearchResponse(totalResults = count, books = books)
    }

    fun reset() {
        shouldThrowException = false
        returnEmptyResults = false
        searchBooksCallCount = 0
        lastSearchQuery = null
        lastSearchParams = null
        configuredSearchResponse = null
        configuredDescription = null
        getBookDescriptionCallCount = 0
        lastBookId = null
        networkError = DataError.Remote.REQUEST_TIMEOUT
    }

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<BookSearchResponse, DataError.Remote> {
        searchBooksCallCount++
        lastSearchQuery = query
        lastSearchParams = SearchParams(query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort)

        return when {
            shouldThrowException -> Result.Error(networkError)
            returnEmptyResults -> Result.Success(BookSearchResponse(totalResults = 0, books = emptyList()))
            configuredSearchResponse != null -> Result.Success(configuredSearchResponse!!)
            else -> {
                val defaultBooks = listOf(
                    TestSearchedBookDtoBuilder.withAllFields().toBook(),
                    TestSearchedBookDtoBuilder.withMinimalFields().toBook()
                )
                Result.Success(BookSearchResponse(totalResults = defaultBooks.size, books = defaultBooks))
            }
        }
    }

    private var configuredDescription: String? = null
    var getBookDescriptionCallCount = 0
    var lastBookId: String? = null

    fun configureBookDescription(description: String?) {
        configuredDescription = description
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        getBookDescriptionCallCount++
        lastBookId = bookId

        return when {
            shouldThrowException -> Result.Error(networkError)
            else -> Result.Success(configuredDescription ?: "Default test description")
        }
    }
}
