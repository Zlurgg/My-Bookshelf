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

    override suspend fun execute(
        query: String,
        sortBy: BookSearchSort,
        language: String?,
        authorFilter: String?,
        titleFilter: String?
    ): Result<List<Book>, DataError.Remote> {
        // Determine server-side sort parameter
        val serverSort = if (sortBy.useServerSide) sortBy.serverSortParam else null

        return remoteBookDataSource.searchBooks(
            query = query,
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