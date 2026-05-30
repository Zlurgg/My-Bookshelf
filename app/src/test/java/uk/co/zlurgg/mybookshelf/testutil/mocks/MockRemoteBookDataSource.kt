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
        val sort: String?,
        val startIndex: Int?,
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
        configuredSearchResponse = BookSearchResponse(
            books = books,
            rawPageSize = books.size,
            pageSize = DEFAULT_TEST_PAGE_SIZE,
        )
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
        sort: String?,
        startIndex: Int?,
    ): Result<BookSearchResponse, DataError.Remote> {
        searchBooksCallCount++
        lastSearchQuery = query
        lastSearchParams = SearchParams(
            query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort, startIndex,
        )

        return when {
            shouldThrowException -> Result.Error(networkError)
            returnEmptyResults -> Result.Success(
                BookSearchResponse(
                    books = emptyList(),
                    rawPageSize = 0,
                    pageSize = DEFAULT_TEST_PAGE_SIZE,
                )
            )
            configuredSearchResponse != null -> Result.Success(configuredSearchResponse!!)
            else -> {
                val defaultBooks = listOf(
                    TestSearchedBookDtoBuilder.withAllFields().toBook(),
                    TestSearchedBookDtoBuilder.withMinimalFields().toBook()
                )
                Result.Success(
                    BookSearchResponse(
                        books = defaultBooks,
                        rawPageSize = defaultBooks.size,
                        pageSize = DEFAULT_TEST_PAGE_SIZE,
                    )
                )
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

    companion object {
        // Arbitrary single-page size for tests that don't care about pagination —
        // big enough that `rawPageSize < pageSize` evaluates to "end of results."
        private const val DEFAULT_TEST_PAGE_SIZE = 1000
    }
}
