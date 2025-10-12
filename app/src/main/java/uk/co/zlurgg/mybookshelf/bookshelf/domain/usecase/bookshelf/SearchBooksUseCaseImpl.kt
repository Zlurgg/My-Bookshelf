package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Implementation of SearchBooksUseCase that combines network data retrieval with domain sorting.
 * Follows Clean Architecture by coordinating between data and domain layers.
 */
class SearchBooksUseCaseImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val bookSorter: BookSorter
) : SearchBooksUseCase {

    companion object {
        private const val MAX_QUERY_LENGTH = 200
        private const val MAX_AUTHOR_FILTER_LENGTH = 100
        private const val MAX_TITLE_FILTER_LENGTH = 200
    }

    override suspend fun execute(
        query: String,
        sortBy: BookSearchSort,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?
    ): Result<List<Book>, DataError.Remote> {
        // Validate input lengths to prevent abuse and performance issues
        if (query.length > MAX_QUERY_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        if (authorFilter != null && authorFilter.length > MAX_AUTHOR_FILTER_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        if (titleFilter != null && titleFilter.length > MAX_TITLE_FILTER_LENGTH) {
            return Result.Error(DataError.Remote.MALFORMED_REQUEST)
        }

        // Determine server-side sort parameter
        val serverSort = if (sortBy.useServerSide) sortBy.serverSortParam else null

        return remoteBookDataSource.searchBooks(
            query = query,
            resultLimit = resultLimit,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter,
            sort = serverSort
        ).map { dto ->
            val books = dto.results.map { it.toBook() }
            // Apply client-side sorting only if not handled server-side
            if (sortBy.isClientSide) {
                bookSorter.sortBooks(books, sortBy, query)
            } else {
                books // Server already sorted
            }
        }
    }
}