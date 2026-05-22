package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.domain.service.SafeSearchFilter
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Implementation of SearchBooksUseCase that retrieves book data from RemoteBookDataSource.
 * Results are sorted by the API's default relevance algorithm.
 */
class SearchBooksUseCaseImpl(
    private val remoteBookDataSource: RemoteBookDataSource
) : SearchBooksUseCase {

    companion object {
        private const val MAX_QUERY_LENGTH = 200
        private const val MAX_AUTHOR_FILTER_LENGTH = 100
        private const val MAX_TITLE_FILTER_LENGTH = 200
        private const val MAX_SUBJECT_FILTER_LENGTH = 200
    }

    override suspend operator fun invoke(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        safeSearchEnabled: Boolean
    ): Result<SearchResult, DataError.Remote> {
        if (query.length > MAX_QUERY_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        if (authorFilter != null && authorFilter.length > MAX_AUTHOR_FILTER_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        if (titleFilter != null && titleFilter.length > MAX_TITLE_FILTER_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        if (subjectFilter != null && subjectFilter.length > MAX_SUBJECT_FILTER_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        return remoteBookDataSource.searchBooks(
            query = query,
            resultLimit = resultLimit,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter,
            subjectFilter = subjectFilter,
            sort = null
        ).map { response ->
            val safeBooks = if (safeSearchEnabled) {
                response.books.filter { SafeSearchFilter.isBookSafe(it) }
            } else {
                response.books
            }
            SearchResult(
                books = safeBooks,
                filteredCount = response.books.size - safeBooks.size
            )
        }
    }
}
