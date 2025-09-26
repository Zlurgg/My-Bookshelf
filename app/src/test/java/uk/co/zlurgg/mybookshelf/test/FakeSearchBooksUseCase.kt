package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Fake implementation of SearchBooksUseCase for testing.
 * Allows tests to control search results and verify interactions.
 */
class FakeSearchBooksUseCase : SearchBooksUseCase {

    var searchResults: List<Book> = emptyList()
    var errorToReturn: DataError.Remote? = null

    // Track last search parameters for verification
    var lastSearchQuery: String? = null
    var lastSortBy: BookSearchSort? = null
    var lastLanguage: String? = null
    var lastAuthorFilter: String? = null
    var lastTitleFilter: String? = null
    var searchCallCount = 0

    override suspend fun execute(
        query: String,
        sortBy: BookSearchSort,
        language: String?,
        authorFilter: String?,
        titleFilter: String?
    ): Result<List<Book>, DataError.Remote> {
        // Track parameters for test verification
        lastSearchQuery = query
        lastSortBy = sortBy
        lastLanguage = language
        lastAuthorFilter = authorFilter
        lastTitleFilter = titleFilter
        searchCallCount++

        return errorToReturn?.let {
            Result.Error(it)
        } ?: Result.Success(searchResults)
    }

    fun reset() {
        searchResults = emptyList()
        errorToReturn = null
        lastSearchQuery = null
        lastSortBy = null
        lastLanguage = null
        lastAuthorFilter = null
        lastTitleFilter = null
        searchCallCount = 0
    }
}